package org.ruoyi.service.coding.harness.journal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * File-system journal projector. It publishes a standalone checkpoint page first, then atomically
 * replaces the index. The source snapshot is never changed, including when either I/O operation
 * fails.
 */
public final class FileRunJournalProjector implements RunJournalProjector {

    private static final String INDEX_FILE = "index.html";
    private static final String CHECKPOINT_PREFIX = "checkpoint-";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT
        .withZone(ZoneOffset.UTC);
    private final Path outputDirectory;

    public FileRunJournalProjector(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory")
            .toAbsolutePath().normalize();
    }

    @Override
    public JournalProjectionResult project(StructuredContextSnapshot snapshot)
        throws JournalProjectionException {
        Objects.requireNonNull(snapshot, "snapshot");
        Path runDirectory = outputDirectory.resolve(snapshot.runId()).normalize();
        Path checkpointFile = runDirectory.resolve(checkpointFileName(snapshot)).normalize();
        Path indexFile = runDirectory.resolve(INDEX_FILE).normalize();
        if (!runDirectory.getParent().equals(outputDirectory)
            || !checkpointFile.getParent().equals(runDirectory)
            || !indexFile.getParent().equals(runDirectory)) {
            throw new JournalProjectionException("Journal output resolved outside its configured directory", null);
        }
        try {
            Files.createDirectories(runDirectory);
            if (!Files.isDirectory(runDirectory)) {
                throw new IOException("Journal output is not a directory: " + runDirectory);
            }
            atomicWrite(runDirectory, checkpointFile, renderCheckpoint(snapshot));
            atomicWrite(runDirectory, indexFile, renderIndex(runDirectory, snapshot.runId()));
            return new JournalProjectionResult(checkpointFile, indexFile);
        } catch (IOException exception) {
            throw new JournalProjectionException("Unable to project run journal to " + outputDirectory,
                exception);
        }
    }

    private String checkpointFileName(StructuredContextSnapshot snapshot) {
        return CHECKPOINT_PREFIX + String.format("%020d", snapshot.checkpointSequence())
            + "-" + snapshot.checkpointId() + ".html";
    }

    private void atomicWrite(Path runDirectory, Path target, String content) throws IOException {
        Path temporary = Files.createTempFile(runDirectory, ".journal-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic moves are required for journal publication", exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String renderIndex(Path runDirectory, String runId) throws IOException {
        List<String> checkpoints = checkpointFileNames(runDirectory);
        StringBuilder body = new StringBuilder();
        body.append("<h1>Harness run journal</h1><p>Run: <code>")
            .append(escapeHtml(runId)).append("</code></p><p>Independent checkpoint projections. "
            + "Secrets are deterministically redacted.</p><ol>");
        for (String checkpoint : checkpoints) {
            body.append("<li><a href=\"").append(escapeHtmlAttribute(checkpoint)).append("\">")
                .append(escapeHtml(checkpoint)).append("</a></li>");
        }
        body.append("</ol>");
        return document("Harness run journal", body.toString());
    }

    private List<String> checkpointFileNames(Path runDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(runDirectory)) {
            return paths.filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith(CHECKPOINT_PREFIX) && name.endsWith(".html"))
                .sorted(Comparator.reverseOrder())
                .toList();
        }
    }

    private String renderCheckpoint(StructuredContextSnapshot snapshot) {
        StringBuilder body = new StringBuilder();
        body.append("<h1>Harness checkpoint</h1>");
        body.append("<dl>");
        definition(body, "Run", snapshot.runId());
        definition(body, "Checkpoint", snapshot.checkpointId());
        definition(body, "Sequence", Long.toString(snapshot.checkpointSequence()));
        definition(body, "Captured (UTC)", TIMESTAMP.format(Instant.ofEpochMilli(snapshot.capturedAtEpochMillis())));
        definition(body, "Snapshot hash", snapshot.snapshotHash());
        definition(body, "Context hash", snapshot.contextHash());
        body.append("</dl>");
        section(body, "Objective", snapshot.objective());
        section(body, "Compaction summary", snapshot.checkpointSummary());
        listSection(body, "Checkpoint artifact IDs", snapshot.checkpointArtifactIds());
        listSection(body, "Acceptance criteria", snapshot.acceptanceCriteria());
        listSection(body, "Constraints", snapshot.constraints());
        planSection(body, snapshot.plan());
        decisionsSection(body, snapshot.decisions());
        repositoriesSection(body, snapshot.repositories());
        changedFilesSection(body, snapshot.changedFiles());
        commandsSection(body, snapshot.commands());
        resolvedErrorsSection(body, snapshot.resolvedErrors());
        listSection(body, "Blockers", snapshot.blockers());
        listSection(body, "Unknown effects", snapshot.unknownEffects());
        section(body, "Next action", snapshot.nextAction());
        artifactsSection(body, snapshot.artifacts());
        metadataSection(body, snapshot.metadata());
        return document("Harness checkpoint " + snapshot.checkpointId(), body.toString());
    }

    private static String document(String title, String body) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<title>" + escapeHtml(title) + "</title><style>body{font-family:system-ui,sans-serif;"
            + "line-height:1.5;max-width:1100px;margin:2rem auto;padding:0 1rem}table{border-collapse:"
            + "collapse;width:100%;margin:.5rem 0 1.5rem}th,td{border:1px solid #ccd;padding:.5rem;"
            + "text-align:left;vertical-align:top}pre{white-space:pre-wrap;overflow-wrap:anywhere;"
            + "margin:0}dt{font-weight:700}dd{margin:0 0 .5rem}</style></head><body>" + body
            + "</body></html>";
    }

    private static void section(StringBuilder body, String title, String value) {
        body.append("<h2>").append(escapeHtml(title)).append("</h2><pre>")
            .append(escapeHtml(JournalSecretRedactor.redact(value))).append("</pre>");
    }

    private static void listSection(StringBuilder body, String title, List<String> values) {
        body.append("<h2>").append(escapeHtml(title)).append("</h2>");
        if (values.isEmpty()) {
            body.append("<p>None recorded.</p>");
            return;
        }
        body.append("<ul>");
        for (String value : values) {
            body.append("<li>").append(escapeHtml(JournalSecretRedactor.redact(value))).append("</li>");
        }
        body.append("</ul>");
    }

    private static void planSection(StringBuilder body, List<StructuredContextSnapshot.PlanStatus> values) {
        body.append("<h2>Plan status</h2><table><thead><tr><th>Step</th><th>Status</th><th>Summary</th>"
            + "<th>Evidence</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.PlanStatus value : values) {
            row(body, value.stepId(), value.status(), value.summary(), value.evidence());
        }
        body.append("</tbody></table>");
    }

    private static void decisionsSection(StringBuilder body, List<StructuredContextSnapshot.Decision> values) {
        body.append("<h2>Decisions</h2><table><thead><tr><th>Decision</th><th>Rationale</th>"
            + "</tr></thead><tbody>");
        for (StructuredContextSnapshot.Decision value : values) {
            row(body, value.decision(), value.rationale());
        }
        body.append("</tbody></table>");
    }

    private static void repositoriesSection(StringBuilder body, List<StructuredContextSnapshot.Repository> values) {
        body.append("<h2>Repositories</h2><table><thead><tr><th>ID</th><th>Relative path</th><th>Revision</th>"
            + "<th>State hash</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.Repository value : values) {
            row(body, value.repositoryId(), value.relativePath(), value.revision(), value.stateHash());
        }
        body.append("</tbody></table>");
    }

    private static void changedFilesSection(StringBuilder body, List<StructuredContextSnapshot.ChangedFile> values) {
        body.append("<h2>Changed files</h2><table><thead><tr><th>Repository</th><th>Relative path</th>"
            + "<th>Symbols</th><th>Content hash</th><th>Summary</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.ChangedFile value : values) {
            row(body, value.repositoryId(), value.relativePath(), String.join(", ", value.symbols()),
                value.contentHash(), value.changeSummary());
        }
        body.append("</tbody></table>");
    }

    private static void commandsSection(StringBuilder body, List<StructuredContextSnapshot.CommandEvidence> values) {
        body.append("<h2>Commands and tests</h2><table><thead><tr><th>Kind</th><th>Command</th>"
            + "<th>Exit</th><th>Evidence</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.CommandEvidence value : values) {
            String kind = value.test() == null ? ""
                : Boolean.TRUE.equals(value.test()) ? "test" : "command";
            row(body, kind, value.command(),
                Integer.toString(value.exitCode()), value.evidence());
        }
        body.append("</tbody></table>");
    }

    private static void resolvedErrorsSection(StringBuilder body, List<StructuredContextSnapshot.ResolvedError> values) {
        body.append("<h2>Resolved errors</h2><table><thead><tr><th>Error</th><th>Resolution</th>"
            + "<th>Evidence</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.ResolvedError value : values) {
            row(body, value.error(), value.resolution(), value.evidence());
        }
        body.append("</tbody></table>");
    }

    private static void artifactsSection(StringBuilder body, List<StructuredContextSnapshot.ArtifactReference> values) {
        body.append("<h2>Artifacts</h2><table><thead><tr><th>ID</th><th>Relative handle</th>"
            + "<th>Content hash</th><th>Media type</th><th>Bytes</th></tr></thead><tbody>");
        for (StructuredContextSnapshot.ArtifactReference value : values) {
            row(body, value.artifactId(), value.relativeHandle(), value.contentHash(), value.mediaType(),
                Long.toString(value.byteSize()));
        }
        body.append("</tbody></table>");
    }

    private static void metadataSection(StringBuilder body, Map<String, String> values) {
        body.append("<h2>Checkpoint metadata</h2><table><thead><tr><th>Key</th><th>Value</th>"
            + "</tr></thead><tbody>");
        values.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> row(body, entry.getKey(), entry.getValue()));
        body.append("</tbody></table>");
    }

    private static void definition(StringBuilder body, String term, String value) {
        body.append("<dt>").append(escapeHtml(term)).append("</dt><dd>")
            .append(escapeHtml(JournalSecretRedactor.redact(value))).append("</dd>");
    }

    private static void row(StringBuilder body, String... values) {
        body.append("<tr>");
        for (String value : values) {
            body.append("<td><pre>").append(escapeHtml(JournalSecretRedactor.redact(value)))
                .append("</pre></td>");
        }
        body.append("</tr>");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtml(value);
    }
}
