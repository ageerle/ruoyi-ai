package org.ruoyi.service.coding.harness.tool.builtin;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.common.process.ChildProcessSecretSanitizer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Run-bound, first-party coding tools. This class deliberately exposes no shell or delete primitive.
 * All process arguments used by search_text are passed directly to ProcessBuilder; no shell parses them.
 */
public final class BuiltinCodingTools {

    private static final ConcurrentHashMap<Path, WriteLockRef> WRITE_LOCKS = new ConcurrentHashMap<>();
    private static final int MAX_GIT_CONTEXT_LINES = 20;
    private static final List<String> RIPGREP_IGNORES = List.of(
        "!**/.git/**", "!**/.hg/**", "!**/.svn/**", "!**/.idea/**", "!**/.gradle/**",
        "!**/.harness-deps/**", "!**/node_modules/**", "!**/target/**", "!**/build/**",
        "!**/dist/**", "!**/coverage/**"
    );

    private final RunContext context;
    private final BuiltinToolLimits limits;
    private final WorkspaceLeaseGuard guard;

    public BuiltinCodingTools(RunContext context) {
        this.context = java.util.Objects.requireNonNull(context, "context");
        this.limits = context.limits();
        this.guard = new WorkspaceLeaseGuard(context);
    }

    public RunContext context() {
        return context;
    }

    @Tool(name = "read_file", value = {
        "Read one bounded file page inside the current workspace lease. Offset is the zero-based line offset. " +
            "Returns SHA-256 for safe subsequent writes and reports binary files without decoding them unless requested."
    })
    public ReadFileResult readFile(
        @P(name = "path", value = "Workspace-relative path, or an absolute path inside the lease", required = true)
        String path,
        @P(name = "offset", value = "Zero-based line offset; defaults to 0", required = false) Integer offset,
        @P(name = "limit", value = "Maximum lines; clamped to the run limit", required = false) Integer limit,
        @P(name = "lineNumbers", value = "Prefix returned text lines with 1-based line numbers", required = false)
        Boolean lineNumbers,
        @P(name = "includeBinary",
            value = "For a bounded binary file, return base64 content instead of metadata only", required = false)
        Boolean includeBinary
    ) {
        Path file = guard.existingFile(path);
        byte[] bytes = readBounded(file, limits.maxReadFileBytes(), "FILE_TOO_LARGE");
        String relative = guard.relative(file);
        String sha256 = Hashing.sha256(bytes);
        int requestedOffset = boundedOffset(offset);
        int requestedLimit = boundedPositive(limit, limits.maxReadLines(), "limit");
        if (isBinary(bytes)) {
            String content = "";
            String encoding = "binary";
            if (Boolean.TRUE.equals(includeBinary)) {
                content = Base64.getEncoder().encodeToString(bytes);
                if (utf8Length(content) > limits.maxReadOutputBytes()) {
                    throw new BuiltinToolException("BINARY_OUTPUT_TOO_LARGE",
                        "Base64 output exceeds the run output limit: " + relative);
                }
                encoding = "base64";
            }
            return new ReadFileResult(relative, bytes.length, sha256, true, encoding, content,
                requestedOffset, 0, 0, 0, 0, false);
        }

        String text = decodeUtf8(bytes, relative);
        List<String> lines = splitLines(text);
        int endExclusive = (int) Math.min(lines.size(), (long) requestedOffset + requestedLimit);
        boolean truncated = requestedOffset > lines.size()
            || (limit != null && limit > limits.maxReadLines())
            || endExclusive < lines.size();
        StringBuilder page = new StringBuilder();
        int outputBytes = 0;
        int returnedLines = 0;
        for (int index = Math.min(requestedOffset, lines.size()); index < endExclusive; index++) {
            String rendered = Boolean.TRUE.equals(lineNumbers)
                ? String.format(Locale.ROOT, "%6d\t%s", index + 1, lines.get(index))
                : lines.get(index);
            String prefix = returnedLines == 0 ? "" : "\n";
            int remaining = Math.toIntExact(Math.min(Integer.MAX_VALUE,
                limits.maxReadOutputBytes() - outputBytes));
            String candidate = prefix + rendered;
            int candidateBytes = utf8Length(candidate);
            if (candidateBytes <= remaining) {
                page.append(candidate);
                outputBytes += candidateBytes;
                returnedLines++;
                continue;
            }
            String bounded = truncateUtf8(candidate, remaining);
            if (!bounded.isEmpty()) {
                page.append(bounded);
                returnedLines++;
            }
            truncated = true;
            break;
        }
        int startLine = returnedLines == 0 ? 0 : Math.min(requestedOffset, lines.size()) + 1;
        int endLine = returnedLines == 0 ? 0 : startLine + returnedLines - 1;
        return new ReadFileResult(relative, bytes.length, sha256, false, "utf-8", page.toString(),
            requestedOffset, returnedLines, startLine, endLine, lines.size(), truncated);
    }

    @Tool(name = "read_source", value = {
        "Read one bounded UTF-8 source page as literal text rather than a JSON-encoded content field. " +
        "Prefer this for code containing backslashes, quotes, template strings, or escape sequences. " +
        "The header includes the current SHA-256 required by mutation tools. Reuse unchanged " +
        "source evidence where possible; coding tasks may re-read to refresh context or obtain " +
        "the current file version before editing."
    })
    public String readSource(
        @P(name = "path", value = "Workspace-relative path, or an absolute path inside the lease", required = true)
        String path,
        @P(name = "offset", value = "Zero-based line offset; defaults to 0", required = false) Integer offset,
        @P(name = "limit", value = "Maximum lines; clamped to the run limit", required = false) Integer limit,
        @P(name = "lineNumbers", value = "Prefix returned text lines with 1-based line numbers", required = false)
        Boolean lineNumbers
    ) {
        ReadFileResult result = readFile(path, offset, limit, lineNumbers, false);
        if (result.binary()) {
            throw new BuiltinToolException("BINARY_FILE",
                "read_source requires a UTF-8 text file: " + result.path());
        }
        return "path: " + result.path() + "\n"
            + "sha256: " + result.sha256() + "\n"
            + "lines: " + result.startLine() + "-" + result.endLine() + "/"
            + result.totalLines() + "\n"
            + "truncated: " + result.truncated() + "\n"
            + "--- BEGIN LITERAL SOURCE ---\n"
            + result.content()
            + "\n--- END LITERAL SOURCE ---";
    }

    @Tool(name = "list_files", value = {
        "List a bounded workspace subtree. Honors common generated directories and .gitignore rules, " +
            "and never follows symbolic links or junctions."
    })
    public FileListResult listFiles(
        @P(name = "path", value = "Workspace-relative directory, or an absolute directory inside the lease",
            required = false)
        String path,
        @P(name = "maxDepth", value = "Maximum traversal depth from the requested directory", required = false)
        Integer maxDepth,
        @P(name = "limit", value = "Maximum returned entries", required = false) Integer limit
    ) {
        return enumerate(path, null, maxDepth, limit);
    }

    @Tool(name = "glob_files", value = {
        "Find paths by a platform-neutral glob inside a bounded workspace subtree. Honors .gitignore and never follows links."
    })
    public FileListResult globFiles(
        @P(name = "glob", value = "Glob such as **/*.java", required = true) String glob,
        @P(name = "path", value = "Workspace-relative base directory", required = false) String path,
        @P(name = "maxDepth", value = "Maximum traversal depth from the requested directory", required = false)
        Integer maxDepth,
        @P(name = "limit", value = "Maximum returned entries", required = false) Integer limit
    ) {
        validateGlob(glob);
        return enumerate(path, glob, maxDepth, limit);
    }

    @Tool(name = "search_text", value = {
        "Search workspace text using a literal or regular expression. Uses parameterized ripgrep when available, " +
            "otherwise a bounded Java implementation; respects ignore rules and never follows links."
    })
    public SearchTextResult searchText(
        @P(name = "query", value = "Non-empty literal or regular expression", required = true) String query,
        @P(name = "regex", value = "Treat query as a regular expression; defaults to false", required = false)
        Boolean regex,
        @P(name = "glob", value = "Optional inclusive file glob such as **/*.java", required = false) String glob,
        @P(name = "maxResults", value = "Maximum matches; clamped to the run limit", required = false)
        Integer maxResults,
        @P(name = "maxLineChars", value = "Maximum characters returned from one matching line", required = false)
        Integer maxLineChars
    ) {
        if (query == null || query.isEmpty()) {
            throw new BuiltinToolException("INVALID_QUERY", "Search query must not be empty");
        }
        if (utf8Length(query) > 64 * 1024) {
            throw new BuiltinToolException("INVALID_QUERY", "Search query exceeds 64 KiB");
        }
        boolean useRegex = Boolean.TRUE.equals(regex);
        Pattern compiled = null;
        if (useRegex) {
            try {
                compiled = Pattern.compile(query);
            } catch (PatternSyntaxException error) {
                throw new BuiltinToolException("INVALID_REGEX", error.getDescription(), error);
            }
        }
        if (glob != null && !glob.isBlank()) {
            validateGlob(glob);
        }
        int resultLimit = boundedPositive(maxResults, limits.maxSearchResults(), "maxResults");
        int lineLimit = boundedPositive(maxLineChars, limits.maxSearchLineChars(), "maxLineChars");
        boolean clamped = (maxResults != null && maxResults > limits.maxSearchResults())
            || (maxLineChars != null && maxLineChars > limits.maxSearchLineChars());
        if (context.preferRipgrep()) {
            SearchTextResult ripgrep = searchWithRipgrep(query, useRegex, glob, resultLimit, lineLimit, clamped);
            if (ripgrep != null) {
                return ripgrep;
            }
        }
        return searchWithJava(query, useRegex, compiled, glob, resultLimit, lineLimit, clamped);
    }

    @Tool(name = "git_status", value = {
        "Return bounded two-column status for the repository whose top-level directory is exactly the current " +
            "workspace lease. This pure-Java tool is read-only and cannot invoke repository filter commands."
    })
    public GitStatusResult gitStatus(
        @P(name = "limit", value = "Maximum entries; clamped to the run list limit", required = false)
        Integer limit
    ) {
        int entryLimit = boundedPositive(limit, limits.maxListEntries(), "limit");
        List<GitStatusEntry> entries = runJGit("status", git -> {
            Status status = git.status()
                .setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.ALL)
                .setProgressMonitor(new DeadlineProgressMonitor(limits.searchTimeout()))
                .call();
            return projectStatus(status);
        });
        boolean truncated = entries.size() > entryLimit;
        if (entries.size() > entryLimit) {
            entries = new ArrayList<>(entries.subList(0, entryLimit));
        }
        return new GitStatusResult(entries, entries.isEmpty() && !truncated, truncated);
    }

    @Tool(name = "git_diff", value = {
        "Return a bounded unified Git diff for the repository whose top-level directory is exactly the current " +
            "workspace lease. This pure-Java tool cannot invoke external diff, textconv, or filter commands."
    })
    public GitDiffResult gitDiff(
        @P(name = "staged", value = "Read the staged index diff instead of unstaged worktree changes",
            required = false)
        Boolean staged,
        @P(name = "contextLines", value = "Unified context lines from 0 through 20; defaults to 3",
            required = false)
        Integer contextLines
    ) {
        int contextLineCount = contextLines == null ? 3 : contextLines;
        if (contextLineCount < 0 || contextLineCount > MAX_GIT_CONTEXT_LINES) {
            throw new BuiltinToolException("INVALID_LIMIT", "contextLines must be between 0 and 20");
        }
        boolean stagedDiff = Boolean.TRUE.equals(staged);
        BoundedOutputStream output = new BoundedOutputStream(Math.toIntExact(limits.maxReadOutputBytes()));
        runJGit("diff", git -> {
            git.diff()
                .setCached(stagedDiff)
                .setContextLines(contextLineCount)
                .setOutputStream(output)
                .setProgressMonitor(new DeadlineProgressMonitor(limits.searchTimeout()))
                .call();
            return null;
        });
        String content = new String(output.bytes(), StandardCharsets.UTF_8);
        return new GitDiffResult(stagedDiff, contextLineCount, content, output.size(), output.truncated());
    }

    @Tool(name = "write_file", value = {
        "Atomically create or replace a UTF-8 file inside the workspace lease. " +
            "When the file exists, expectedSha256 from the current read_source or read_file header is mandatory."
    })
    public FileMutationResult writeFile(
        @P(name = "path", value = "Workspace-relative path, or an absolute path inside the lease", required = true)
        String path,
        @P(name = "content", value = "Complete UTF-8 file content", required = true) String content,
        @P(name = "expectedSha256", value = "Required current SHA-256 when replacing an existing file",
            required = false)
        String expectedSha256
    ) {
        if (content == null) {
            throw new BuiltinToolException("INVALID_CONTENT", "File content is required");
        }
        byte[] newBytes = content.getBytes(StandardCharsets.UTF_8);
        enforceWriteLimit(newBytes);
        Path initial = guard.writeTarget(path);
        return withWriteLock(initial, () -> {
            Path candidate = guard.writeTarget(path);
            ExistingState existing = verifyPrecondition(candidate, expectedSha256, false);
            Path target = guard.prepareWriteTarget(path);
            atomicReplace(target, newBytes, existing);
            String summary = existing.exists()
                ? "updated " + existing.bytes().length + " -> " + newBytes.length + " bytes"
                : "created " + newBytes.length + " bytes";
            return new FileMutationResult(guard.relative(target), Hashing.sha256(newBytes), newBytes.length,
                !existing.exists(), summary);
        });
    }

    @Tool(name = "replace_text", value = {
        "Atomically replace exactly one literal text occurrence in an existing UTF-8 file. " +
            "LF and CRLF line endings are equivalent when matching; existing file line endings are preserved. " +
            "Fails without side effects when the match is missing/non-unique or expectedSha256 is stale."
    })
    public FileMutationResult replaceText(
        @P(name = "path", value = "Workspace-relative path, or an absolute path inside the lease", required = true)
        String path,
        @P(name = "oldText", value = "Literal text that must occur exactly once", required = true) String oldText,
        @P(name = "newText", value = "Replacement text", required = true) String newText,
        @P(name = "expectedSha256", value = "Required current SHA-256 from read_source or read_file",
            required = true)
        String expectedSha256
    ) {
        if (oldText == null || oldText.isEmpty()) {
            throw new BuiltinToolException("INVALID_MATCH", "oldText must not be empty");
        }
        if (newText == null) {
            throw new BuiltinToolException("INVALID_CONTENT", "newText is required");
        }
        Path initial = guard.writeTarget(path);
        return withWriteLock(initial, () -> {
            Path candidate = guard.writeTarget(path);
            ExistingState existing = verifyPrecondition(candidate, expectedSha256, true);
            Path target = guard.prepareWriteTarget(path);
            if (isBinary(existing.bytes())) {
                throw new BuiltinToolException("BINARY_FILE", "replace_text only supports UTF-8 text files");
            }
            String original = decodeUtf8(existing.bytes(), guard.relative(target));
            String normalized = normalizeLineEndings(original);
            String needle = normalizeLineEndings(oldText);
            int match = normalized.indexOf(needle);
            int first = originalOffset(original, match);
            if (first < 0) {
                throw new BuiltinToolException("MATCH_NOT_FOUND", "oldText was not found");
            }
            if (normalized.indexOf(needle, match + 1) >= 0) {
                throw new BuiltinToolException("MATCH_NOT_UNIQUE", "oldText occurs more than once");
            }
            int end = originalOffset(original, match + needle.length());
            String matched = original.substring(first, end);
            String lineEnding = preferredLineEnding(matched.isEmpty() || !matched.contains("\n")
                && !matched.contains("\r") ? original : matched);
            String replacement = original.substring(0, first)
                + normalizeLineEndings(newText).replace("\n", lineEnding) + original.substring(end);
            byte[] newBytes = replacement.getBytes(StandardCharsets.UTF_8);
            enforceWriteLimit(newBytes);
            atomicReplace(target, newBytes, existing);
            int line = 1;
            for (int index = 0; index < first; index++) {
                if (original.charAt(index) == '\n') {
                    line++;
                }
            }
            return new FileMutationResult(guard.relative(target), Hashing.sha256(newBytes), newBytes.length,
                false, "replaced one occurrence at line " + line + "; "
                    + existing.bytes().length + " -> " + newBytes.length + " bytes");
        });
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Map an LF-normalized match back to original bytes without rewriting surrounding text. */
    private static int originalOffset(String original, int normalizedOffset) {
        if (normalizedOffset < 0) {
            return -1;
        }
        int offset = 0;
        for (int count = 0; count < normalizedOffset; count++) {
            if (original.charAt(offset++) == '\r' && offset < original.length()
                && original.charAt(offset) == '\n') {
                offset++;
            }
        }
        return offset;
    }

    private static String preferredLineEnding(String value) {
        int cr = value.indexOf('\r');
        int lf = value.indexOf('\n');
        if (cr >= 0 && (lf < 0 || cr < lf)) {
            return cr + 1 < value.length() && value.charAt(cr + 1) == '\n' ? "\r\n" : "\r";
        }
        return "\n";
    }

    private FileListResult enumerate(String path, String glob, Integer requestedDepth, Integer requestedLimit) {
        Path base = guard.existingDirectory(path == null || path.isBlank() ? "." : path);
        int depthLimit = boundedPositive(requestedDepth, limits.maxTraversalDepth(), "maxDepth");
        int resultLimit = boundedPositive(requestedLimit, limits.maxListEntries(), "limit");
        PathGlob matcher = glob == null ? null : PathGlob.compile(glob);
        GitIgnoreMatcher ignore = new GitIgnoreMatcher(guard.root());
        List<FileListEntry> entries = new ArrayList<>();
        TraversalState state = new TraversalState(limits.maxListEntries(), resultLimit,
            requestedDepth != null && requestedDepth > limits.maxTraversalDepth()
                || requestedLimit != null && requestedLimit > limits.maxListEntries());
        enumerateDirectory(base, base, 0, depthLimit, matcher, ignore, entries, state);
        return new FileListResult(displayBase(base), glob, entries, state.truncated);
    }

    private void enumerateDirectory(Path base, Path directory, int depth, int maxDepth, PathGlob matcher,
                                    GitIgnoreMatcher ignore, List<FileListEntry> entries, TraversalState state) {
        if (state.stop()) {
            return;
        }
        ChildBatch batch = sortedChildren(directory, state.remainingVisitBudget());
        state.truncated |= batch.overflow();
        for (Path child : batch.children()) {
            if (state.stop()) {
                state.truncated = true;
                return;
            }
            boolean childDirectory = guard.isUnlinkedDirectory(child);
            boolean childFile = guard.isUnlinkedRegularFile(child);
            if (!childDirectory && !childFile) {
                continue;
            }
            if (ignore.ignored(child, childDirectory)) {
                continue;
            }
            state.visited++;
            int childDepth = depth + 1;
            String relativeToBase = base.relativize(child).toString().replace('\\', '/');
            boolean matches = matcher == null || matcher.matches(relativeToBase);
            if (matches) {
                if (entries.size() >= state.resultLimit) {
                    state.truncated = true;
                    return;
                }
                entries.add(new FileListEntry(guard.relative(child), childDirectory ? "directory" : "file",
                    childFile ? safeSize(child) : 0));
            }
            if (childDirectory) {
                if (childDepth < maxDepth) {
                    enumerateDirectory(base, child, childDepth, maxDepth, matcher, ignore, entries, state);
                } else if (hasVisibleChild(child, ignore)) {
                    state.truncated = true;
                }
            }
        }
        if (state.visited >= state.visitLimit) {
            state.truncated = true;
        }
    }

    private SearchTextResult searchWithRipgrep(String query, boolean regex, String glob, int resultLimit,
                                                int lineLimit, boolean initiallyTruncated) {
        List<String> command = new ArrayList<>();
        command.add("rg");
        command.add("--line-number");
        command.add("--column");
        command.add("--no-heading");
        command.add("--null");
        command.add("--color");
        command.add("never");
        command.add("--no-messages");
        command.add("--max-filesize");
        command.add(Long.toString(limits.maxSearchFileBytes()));
        if (!regex) {
            command.add("--fixed-strings");
        }
        if (glob != null && !glob.isBlank()) {
            command.add("--glob");
            command.add(glob);
        }
        for (String ignored : RIPGREP_IGNORES) {
            command.add("--glob");
            command.add(ignored);
        }
        command.add("-e");
        command.add(query);
        command.add("--");
        command.add(".");

        final Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                .directory(guard.root().toFile())
                .redirectErrorStream(true);
            process = ChildProcessSecretSanitizer.start(builder);
        } catch (IOException unavailable) {
            return null;
        }

        CompletableFuture<RgCapture> captureFuture = CompletableFuture.supplyAsync(
            () -> captureRipgrep(process, resultLimit, lineLimit, initiallyTruncated));
        try {
            if (!process.waitFor(limits.searchTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new BuiltinToolException("SEARCH_TIMEOUT", "ripgrep exceeded the run search timeout");
            }
            RgCapture capture = captureFuture.get(2, TimeUnit.SECONDS);
            int exit = process.exitValue();
            if (exit > 1 && !capture.truncated()) {
                // Regex dialects differ (for example Java look-around versus ripgrep's default engine).
                // Falling back preserves the advertised Java-regex behavior without invoking a shell.
                return null;
            }
            return new SearchTextResult(query, regex, glob, "ripgrep", capture.matches(), capture.truncated());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BuiltinToolException("SEARCH_INTERRUPTED", "Search was interrupted", error);
        } catch (ExecutionException | TimeoutException error) {
            process.destroyForcibly();
            throw new BuiltinToolException("SEARCH_FAILED", "Cannot collect ripgrep output", error);
        }
    }

    private <T> T runJGit(String operationName, JGitOperation<T> operation) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try (Git git = openLeaseRepository()) {
                return operation.run(git);
            } catch (BuiltinToolException error) {
                throw error;
            } catch (GitAPIException | IOException error) {
                throw new CompletionException(error);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        });
        try {
            return future.get(limits.searchTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new BuiltinToolException("GIT_INTERRUPTED", "Git " + operationName + " was interrupted", error);
        } catch (TimeoutException error) {
            future.cancel(true);
            throw new BuiltinToolException("GIT_TIMEOUT", "Git " + operationName + " exceeded the run timeout",
                error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof BuiltinToolException builtin) {
                throw builtin;
            }
            if (cause instanceof RepositoryNotFoundException) {
                throw new BuiltinToolException("NOT_GIT_REPOSITORY", "Workspace lease is not a Git repository",
                    cause);
            }
            throw new BuiltinToolException("GIT_COMMAND_FAILED", "Git " + operationName + " failed", cause);
        }
    }

    private Git openLeaseRepository() throws IOException {
        final org.eclipse.jgit.lib.Repository repository;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .setWorkTree(guard.root().toFile())
                .findGitDir(guard.root().toFile());
            if (builder.getGitDir() == null) {
                throw new BuiltinToolException("NOT_GIT_REPOSITORY", "Workspace lease is not a Git repository");
            }
            Path metadata = guard.root().resolve(".git");
            if (!Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
                throw new BuiltinToolException("GIT_ROOT_OUTSIDE_LEASE",
                    "The workspace lease must be the Git repository top-level directory");
            }
            if (!guard.isUnlinkedDirectory(metadata)) {
                throw new BuiltinToolException("GIT_METADATA_UNSAFE",
                    "Git metadata must be an unlinked directory inside the workspace lease");
            }
            repository = builder.setMustExist(true).build();
        } catch (RepositoryNotFoundException error) {
            throw new BuiltinToolException("NOT_GIT_REPOSITORY", "Workspace lease is not a Git repository", error);
        }
        try {
            Path workTree = repository.getWorkTree().toPath().toRealPath().normalize();
            if (!workTree.equals(guard.root())) {
                throw new BuiltinToolException("GIT_ROOT_OUTSIDE_LEASE",
                    "The workspace lease must be the Git repository top-level directory");
            }
            return Git.wrap(repository);
        } catch (RuntimeException | IOException error) {
            repository.close();
            throw error;
        }
    }

    private List<GitStatusEntry> projectStatus(Status status) {
        Map<String, char[]> projected = new TreeMap<>();
        setStatus(projected, status.getAdded(), 0, 'A');
        setStatus(projected, status.getChanged(), 0, 'M');
        setStatus(projected, status.getRemoved(), 0, 'D');
        setStatus(projected, status.getMissing(), 1, 'D');
        setStatus(projected, status.getModified(), 1, 'M');
        for (String path : status.getUntracked()) {
            projected.put(path, new char[]{'?', '?'});
        }
        for (String path : status.getConflicting()) {
            projected.put(path, new char[]{'U', 'U'});
        }
        List<GitStatusEntry> entries = new ArrayList<>(projected.size());
        projected.forEach((path, columns) -> entries.add(
            new GitStatusEntry(new String(columns), path.replace('\\', '/'), null)));
        return entries;
    }

    private void setStatus(Map<String, char[]> projected, Set<String> paths, int column, char value) {
        for (String path : paths) {
            projected.computeIfAbsent(path, ignored -> new char[]{' ', ' '})[column] = value;
        }
    }

    private RgCapture captureRipgrep(Process process, int resultLimit, int lineLimit,
                                     boolean initiallyTruncated) {
        List<SearchMatch> matches = new ArrayList<>();
        boolean truncated = initiallyTruncated;
        String diagnostic = "";
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            String output;
            while ((output = reader.readLine()) != null) {
                SearchMatch match = parseRipgrepLine(output, lineLimit);
                if (match == null) {
                    if (diagnostic.isBlank()) {
                        diagnostic = truncateCharacters(output, lineLimit);
                    }
                    continue;
                }
                if (matches.size() >= resultLimit) {
                    truncated = true;
                    process.destroyForcibly();
                    break;
                }
                matches.add(match);
            }
        } catch (IOException error) {
            if (process.isAlive()) {
                throw new BuiltinToolException("SEARCH_FAILED", "Cannot read ripgrep output", error);
            }
        }
        return new RgCapture(List.copyOf(matches), truncated, diagnostic);
    }

    private SearchMatch parseRipgrepLine(String output, int lineLimit) {
        int nul = output.indexOf('\0');
        int first = nul >= 0 ? nul : output.indexOf(':');
        int second = first < 0 ? -1 : output.indexOf(':', first + 1);
        int third = second < 0 ? -1 : output.indexOf(':', second + 1);
        if (first <= 0 || second <= first || third <= second) {
            return null;
        }
        try {
            int line = Integer.parseInt(output.substring(first + 1, second));
            int column = Integer.parseInt(output.substring(second + 1, third));
            String path = output.substring(0, first).replace('\\', '/');
            while (path.startsWith("./")) {
                path = path.substring(2);
            }
            String rawText = output.substring(third + 1);
            boolean lineTruncated = rawText.length() > lineLimit;
            return new SearchMatch(path, line, column, truncateCharacters(rawText, lineLimit), lineTruncated);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private SearchTextResult searchWithJava(String query, boolean regex, Pattern compiled, String glob,
                                             int resultLimit, int lineLimit, boolean initiallyTruncated) {
        PathGlob matcher = glob == null || glob.isBlank() ? null : PathGlob.compile(glob);
        GitIgnoreMatcher ignore = new GitIgnoreMatcher(guard.root());
        FileCollection files = collectSearchFiles(guard.root(), matcher, ignore);
        List<SearchMatch> matches = new ArrayList<>();
        boolean truncated = initiallyTruncated || files.truncated();
        outer:
        for (Path file : files.files()) {
            byte[] bytes;
            try {
                bytes = readBounded(file, limits.maxSearchFileBytes(), "SEARCH_FILE_TOO_LARGE");
            } catch (BuiltinToolException skipped) {
                continue;
            }
            if (isBinary(bytes)) {
                continue;
            }
            String text;
            try {
                text = decodeUtf8(bytes, guard.relative(file));
            } catch (BuiltinToolException skipped) {
                continue;
            }
            List<String> lines = splitLines(text);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                int column;
                if (regex) {
                    Matcher lineMatcher = compiled.matcher(line);
                    column = lineMatcher.find() ? lineMatcher.start() : -1;
                } else {
                    column = line.indexOf(query);
                }
                if (column < 0) {
                    continue;
                }
                if (matches.size() >= resultLimit) {
                    truncated = true;
                    break outer;
                }
                boolean lineTruncated = line.length() > lineLimit;
                matches.add(new SearchMatch(guard.relative(file), index + 1, column + 1,
                    truncateCharacters(line, lineLimit), lineTruncated));
            }
        }
        return new SearchTextResult(query, regex, glob, "java", matches, truncated);
    }

    private FileCollection collectSearchFiles(Path directory, PathGlob matcher, GitIgnoreMatcher ignore) {
        List<Path> files = new ArrayList<>();
        TraversalFlag truncated = new TraversalFlag();
        collectSearchFiles(directory, directory, 0, matcher, ignore, files, truncated);
        return new FileCollection(List.copyOf(files), truncated.value);
    }

    private void collectSearchFiles(Path base, Path directory, int depth, PathGlob matcher,
                                    GitIgnoreMatcher ignore, List<Path> files, TraversalFlag truncated) {
        if (files.size() >= limits.maxSearchFiles()) {
            truncated.value = true;
            return;
        }
        ChildBatch batch = sortedChildren(directory, limits.maxSearchFiles() - files.size() + 1);
        truncated.value |= batch.overflow();
        for (Path child : batch.children()) {
            boolean childDirectory = guard.isUnlinkedDirectory(child);
            boolean childFile = guard.isUnlinkedRegularFile(child);
            if (!childDirectory && !childFile || ignore.ignored(child, childDirectory)) {
                continue;
            }
            if (childDirectory) {
                if (depth + 1 < limits.maxTraversalDepth()) {
                    collectSearchFiles(base, child, depth + 1, matcher, ignore, files, truncated);
                } else if (hasVisibleChild(child, ignore)) {
                    truncated.value = true;
                }
            } else {
                String relative = base.relativize(child).toString().replace('\\', '/');
                if ((matcher == null || matcher.matches(relative)) && safeSize(child) <= limits.maxSearchFileBytes()) {
                    if (files.size() >= limits.maxSearchFiles()) {
                        truncated.value = true;
                        return;
                    }
                    files.add(child);
                }
            }
        }
    }

    private ExistingState verifyPrecondition(Path target, String expectedSha256, boolean mustExist) {
        if (expectedSha256 != null && !expectedSha256.isBlank()) {
            validateSha256(expectedSha256);
        }
        boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
        if (!exists) {
            if (mustExist) {
                throw new BuiltinToolException("NOT_FOUND", "File does not exist: " + guard.relative(target));
            }
            if (expectedSha256 != null && !expectedSha256.isBlank()) {
                throw new BuiltinToolException("HASH_CONFLICT",
                    "File no longer exists, so expectedSha256 cannot match");
            }
            return new ExistingState(false, new byte[0], null);
        }
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            throw new BuiltinToolException("PRECONDITION_REQUIRED",
                "expectedSha256 is required for existing file " + guard.relative(target)
                    + ". No write occurred. Read this current file with read_file/read_source, "
                    + "then copy its exact sha256 into the retry. Do not guess a hash or repeat this call unchanged.");
        }
        byte[] current = readBounded(target, limits.maxWriteBytes(), "FILE_TOO_LARGE");
        String actual = Hashing.sha256(current);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw new BuiltinToolException("HASH_CONFLICT",
                "File changed since it was read: " + guard.relative(target) + "; expected "
                    + expectedSha256 + " but found " + actual
                    + ". No write occurred. Re-read the current file, preserve its changes, and retry "
                    + "using the fresh sha256. Each successful write returns a new sha256.");
        }
        return new ExistingState(true, current, actual);
    }

    private void atomicReplace(Path target, byte[] content, ExistingState original) {
        Path parent = target.getParent();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(parent, "." + target.getFileName() + ".harness-", ".tmp");
            preservePosixPermissions(target, temporary, original.exists());
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            verifyUnchangedBeforeMove(target, original);
            if (original.exists()) {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            }
            temporary = null;
            forceDirectory(parent);
        } catch (AtomicMoveNotSupportedException error) {
            throw new BuiltinToolException("ATOMIC_MOVE_UNSUPPORTED",
                "Filesystem does not support atomic replacement for " + guard.relative(target), error);
        } catch (IOException error) {
            throw new BuiltinToolException("IO_ERROR", "Atomic write failed: " + guard.relative(target), error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The primary write error remains more useful; stale temp names are never exposed as targets.
                }
            }
        }
    }

    private void verifyUnchangedBeforeMove(Path target, ExistingState original) {
        boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
        if (original.exists() != exists) {
            throw new BuiltinToolException("HASH_CONFLICT", "File existence changed during the write");
        }
        if (exists) {
            Path guarded = guard.existingFile(target.toString());
            byte[] current = readBounded(guarded, limits.maxWriteBytes(), "FILE_TOO_LARGE");
            if (!Hashing.sha256(current).equals(original.sha256())) {
                throw new BuiltinToolException("HASH_CONFLICT", "File changed during the write");
            }
        }
    }

    private <T> T withWriteLock(Path target, LockedOperation<T> operation) {
        Path key = target.toAbsolutePath().normalize();
        WriteLockRef reference = WRITE_LOCKS.compute(key, (ignored, current) -> {
            WriteLockRef selected = current == null ? new WriteLockRef() : current;
            selected.references++;
            return selected;
        });
        reference.lock.lock();
        try {
            return operation.run();
        } finally {
            reference.lock.unlock();
            WRITE_LOCKS.computeIfPresent(key, (ignored, current) -> {
                if (current != reference) {
                    return current;
                }
                current.references--;
                return current.references == 0 ? null : current;
            });
        }
    }

    private ChildBatch sortedChildren(Path directory, int budget) {
        List<Path> children = new ArrayList<>();
        int boundedBudget = Math.max(1, budget);
        boolean overflow = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path child : stream) {
                if (children.size() >= boundedBudget) {
                    overflow = true;
                    break;
                }
                children.add(child);
            }
        } catch (IOException error) {
            throw new BuiltinToolException("IO_ERROR", "Cannot list directory: " + guard.relative(directory), error);
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return new ChildBatch(List.copyOf(children), overflow);
    }

    private boolean hasVisibleChild(Path directory, GitIgnoreMatcher ignore) {
        ChildBatch batch = sortedChildren(directory, 32);
        for (Path child : batch.children()) {
            boolean childDirectory = guard.isUnlinkedDirectory(child);
            boolean childFile = guard.isUnlinkedRegularFile(child);
            if ((childDirectory || childFile) && !ignore.ignored(child, childDirectory)) {
                return true;
            }
        }
        return batch.overflow();
    }

    private byte[] readBounded(Path path, long maximum, String code) {
        if (maximum >= Integer.MAX_VALUE) {
            throw new IllegalStateException("Byte limits must fit in a Java array");
        }
        try {
            if (Files.size(path) > maximum) {
                throw new BuiltinToolException(code,
                    "File exceeds the " + maximum + " byte limit: " + guard.relative(path));
            }
            try (var input = Files.newInputStream(path)) {
                byte[] bytes = input.readNBytes((int) maximum + 1);
                if (bytes.length > maximum) {
                    throw new BuiltinToolException(code,
                        "File exceeds the " + maximum + " byte limit: " + guard.relative(path));
                }
                return bytes;
            }
        } catch (IOException error) {
            throw new BuiltinToolException("IO_ERROR", "Cannot read file: " + guard.relative(path), error);
        }
    }

    private void enforceWriteLimit(byte[] bytes) {
        if (bytes.length > limits.maxWriteBytes()) {
            throw new BuiltinToolException("CONTENT_TOO_LARGE",
                "UTF-8 content exceeds the " + limits.maxWriteBytes() + " byte write limit");
        }
    }

    private static String decodeUtf8(byte[] bytes, String path) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) {
            throw new BuiltinToolException("BINARY_FILE", "File is not valid UTF-8: " + path, error);
        }
    }

    private static boolean isBinary(byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        int controls = 0;
        int inspected = Math.min(bytes.length, 8 * 1024);
        for (int index = 0; index < inspected; index++) {
            int value = bytes[index] & 0xff;
            if (value == 0) {
                return true;
            }
            if (value < 0x20 && value != '\n' && value != '\r' && value != '\t' && value != '\f') {
                controls++;
            }
        }
        if (controls * 20 > inspected) {
            return true;
        }
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return false;
        } catch (CharacterCodingException invalidUtf8) {
            return true;
        }
    }

    private static List<String> splitLines(String text) {
        if (text.isEmpty()) {
            return List.of();
        }
        String[] raw = text.split("\\R", -1);
        int length = raw.length;
        if (length > 0 && raw[length - 1].isEmpty()) {
            length--;
        }
        List<String> lines = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            lines.add(raw[index]);
        }
        return lines;
    }

    private static int boundedOffset(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new BuiltinToolException("INVALID_LIMIT", "offset must not be negative");
        }
        return value;
    }

    private static int boundedPositive(Integer value, int hardLimit, String name) {
        if (value == null) {
            return hardLimit;
        }
        if (value <= 0) {
            throw new BuiltinToolException("INVALID_LIMIT", name + " must be positive");
        }
        return Math.min(value, hardLimit);
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String truncateUtf8(String value, int maximumBytes) {
        if (maximumBytes <= 0) {
            return "";
        }
        if (utf8Length(value) <= maximumBytes) {
            return value;
        }
        int used = 0;
        int end = 0;
        while (end < value.length()) {
            int codePoint = value.codePointAt(end);
            int encoded = utf8Length(new String(Character.toChars(codePoint)));
            if (used + encoded > maximumBytes) {
                break;
            }
            used += encoded;
            end += Character.charCount(codePoint);
        }
        return value.substring(0, end);
    }

    private static String truncateCharacters(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private void validateGlob(String glob) {
        if (glob == null || glob.isBlank() || glob.length() > 4_096 || glob.indexOf('\0') >= 0
            || glob.indexOf('\n') >= 0 || glob.indexOf('\r') >= 0 || glob.startsWith("!")) {
            throw new BuiltinToolException("INVALID_GLOB", "Glob is empty, unsafe, or too long");
        }
        try {
            PathGlob.compile(glob);
        } catch (RuntimeException error) {
            throw new BuiltinToolException("INVALID_GLOB", "Invalid glob: " + glob, error);
        }
    }

    private static void validateSha256(String value) {
        if (!value.matches("(?i)[0-9a-f]{64}")) {
            throw new BuiltinToolException("INVALID_SHA256", "expectedSha256 must contain 64 hexadecimal characters");
        }
    }

    private static long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException error) {
            return -1;
        }
    }

    private String displayBase(Path base) {
        String relative = guard.relative(base);
        return relative.isEmpty() ? "." : relative;
    }

    private static void preservePosixPermissions(Path target, Path temporary, boolean exists) {
        if (!exists) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(target, LinkOption.NOFOLLOW_LINKS);
            Files.setPosixFilePermissions(temporary, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and non-POSIX stores do not expose these permissions.
        }
    }

    private static void forceDirectory(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException | IOException ignored) {
            // The file itself was fsynced. Directory fsync is not supported by every Java filesystem provider.
        }
    }

    private record ExistingState(boolean exists, byte[] bytes, String sha256) {
    }

    private record RgCapture(List<SearchMatch> matches, boolean truncated, String diagnostic) {
    }

    private record FileCollection(List<Path> files, boolean truncated) {
    }

    private record ChildBatch(List<Path> children, boolean overflow) {
    }

    private static final class TraversalFlag {
        private boolean value;
    }

    private static final class WriteLockRef {
        private final ReentrantLock lock = new ReentrantLock();
        /** Accessed only while ConcurrentHashMap.compute holds the per-key map lock. */
        private int references;
    }

    private static final class TraversalState {
        private final int visitLimit;
        private final int resultLimit;
        private int visited;
        private boolean truncated;

        private TraversalState(int visitLimit, int resultLimit, boolean truncated) {
            this.visitLimit = visitLimit;
            this.resultLimit = resultLimit;
            this.truncated = truncated;
        }

        private int remainingVisitBudget() {
            return Math.max(1, visitLimit - visited + 1);
        }

        private boolean stop() {
            return visited >= visitLimit;
        }
    }

    @FunctionalInterface
    private interface LockedOperation<T> {
        T run();
    }

    @FunctionalInterface
    private interface JGitOperation<T> {
        T run(Git git) throws Exception;
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final int limit;
        private final ByteArrayOutputStream output;
        private boolean truncated;

        private BoundedOutputStream(int limit) {
            this.limit = limit;
            this.output = new ByteArrayOutputStream(Math.min(limit, 16 * 1024));
        }

        @Override
        public void write(int value) {
            if (output.size() < limit) {
                output.write(value);
            } else {
                truncated = true;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            int remaining = limit - output.size();
            if (remaining > 0) {
                output.write(bytes, offset, Math.min(remaining, length));
            }
            if (length > remaining) {
                truncated = true;
            }
        }

        private byte[] bytes() {
            return output.toByteArray();
        }

        private int size() {
            return output.size();
        }

        private boolean truncated() {
            return truncated;
        }
    }

    private static final class DeadlineProgressMonitor implements ProgressMonitor {
        private final long deadlineNanos;

        private DeadlineProgressMonitor(java.time.Duration timeout) {
            long timeoutNanos = timeout.toNanos();
            long now = System.nanoTime();
            this.deadlineNanos = now > Long.MAX_VALUE - timeoutNanos ? Long.MAX_VALUE : now + timeoutNanos;
        }

        @Override public void start(int totalTasks) { }
        @Override public void beginTask(String title, int totalWork) { }
        @Override public void update(int completed) { }
        @Override public void endTask() { }
        @Override public void showDuration(boolean enabled) { }

        @Override
        public boolean isCancelled() {
            return Thread.currentThread().isInterrupted() || System.nanoTime() >= deadlineNanos;
        }
    }
}
