package org.ruoyi.chat.service.coding.harness.artifact;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;

/** Reads bounded ranges only from artifacts belonging to the authenticated current session. */
public final class HarnessArtifactTools {

    private final HarnessArtifactRepository repository;
    private final HarnessOwner owner;
    private final String sessionId;
    private final String runId;

    public HarnessArtifactTools(HarnessArtifactRepository repository, HarnessOwner owner,
                                String sessionId, String runId) {
        this.repository = repository;
        this.owner = owner;
        this.sessionId = sessionId;
        this.runId = runId;
    }

    @Tool(name = "read_artifact", value = {
        "Read one bounded UTF-8 byte range from a content-addressed output artifact belonging",
        "to this exact owner/session and its declared source run. Use sourceRunId, artifactId",
        "and byteSize returned by an",
        "offloaded tool result; request only the range needed for the next decision."
    })
    public ArtifactReadResult read(
        @P(value = "Run id that created the artifact; omit only for the current run",
            required = false) String sourceRunId,
        @P(value = "Lowercase SHA-256 artifact id", required = true) String artifactId,
        @P(value = "Zero-based byte offset", required = true) long offset,
        @P(value = "Positive byte count, bounded by runtime policy", required = true) int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Artifact read length must be positive");
        }
        String scopeRunId = sourceRunId == null || sourceRunId.isBlank()
            ? runId : sourceRunId.strip();
        return repository.readText(owner, sessionId, scopeRunId, artifactId, offset, length);
    }

    @Tool(name = "list_artifacts", value = {
        "List a stable bounded page of all durable artifact handles in this exact owner/session.",
        "Use this after compaction when an older sourceRunId:artifactId handle is no longer in",
        "the recent model-context projection. Continue with nextCursor only while hasMore=true."
    })
    public ArtifactManifestPage list(
        @P(value = "Exclusive sourceRunId:artifactId cursor from the preceding page",
            required = false) String afterCursor,
        @P(value = "Page size from 1 through 256", required = true) int limit) {
        return repository.list(owner, sessionId, runId, afterCursor, limit);
    }
}
