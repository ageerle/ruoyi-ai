package org.ruoyi.service.coding;

import org.ruoyi.mcp.tools.ExecuteCommandTool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

/** Basic, workspace-scoped file and command operations for the Copilot UI. */
@Service
public class CodingWorkspaceService {

    public static final String DEFAULT_WORKSPACE = "D:/Project/github/ruoyi-copilot";
    private static final long MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_ENTRIES = 500;

    public WorkspaceResult list(String workspacePath) throws IOException {
        Path root = resolveRoot(workspacePath);
        Files.createDirectories(root);
        try (var stream = Files.walk(root, 8)) {
            List<FileEntry> files = stream
                .filter(path -> !path.equals(root))
                .filter(path -> !isIgnored(root, path))
                .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                .limit(MAX_ENTRIES)
                .map(path -> toEntry(root, path))
                .toList();
            return new WorkspaceResult(root.toString(), files.size(), files);
        }
    }

    public FileContent read(String workspacePath, String relativePath) throws IOException {
        Path root = resolveRoot(workspacePath);
        Path file = resolveFile(root, relativePath);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File does not exist: " + relativePath);
        }
        long size = Files.size(file);
        if (size > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File is larger than 1 MB: " + relativePath);
        }
        if (isBinary(file)) {
            throw new IllegalArgumentException("Binary files cannot be edited: " + relativePath);
        }
        return new FileContent(normalizeRelative(root, file), Files.readString(file, StandardCharsets.UTF_8), size);
    }

    public FileContent write(String workspacePath, String relativePath, String content) throws IOException {
        Path root = resolveRoot(workspacePath);
        Path file = resolveFile(root, relativePath);
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File content is larger than 1 MB");
        }
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return new FileContent(normalizeRelative(root, file), content == null ? "" : content, bytes.length);
    }

    public CommandResult execute(String workspacePath, String command) {
        Path root = resolveRoot(workspacePath);
        String output = new ExecuteCommandTool(root, null).executeCommand(command);
        boolean success = !output.startsWith("Error:");
        return new CommandResult(command, output, success);
    }

    public Path resolveRoot(String workspacePath) {
        Path configured = Paths.get(DEFAULT_WORKSPACE).toAbsolutePath().normalize();
        if (workspacePath == null || workspacePath.isBlank()) return configured;
        Path requested = Paths.get(workspacePath).toAbsolutePath().normalize();
        if (!requested.equals(configured)) {
            throw new IllegalArgumentException("Workspace is not allowed: " + requested);
        }
        return configured;
    }

    private Path resolveFile(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        Path supplied = Paths.get(relativePath);
        Path target = (supplied.isAbsolute() ? supplied : root.resolve(supplied)).normalize();
        if (!WorkspaceGuard.isWithinWorkspace(root, target)) {
            throw new IllegalArgumentException("File must be inside the workspace");
        }
        return target;
    }

    private boolean isIgnored(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if (name.equals(".git") || name.equals("node_modules") || name.equals("dist") || name.equals("target")) {
                return true;
            }
        }
        return false;
    }

    private FileEntry toEntry(Path root, Path path) {
        try {
            return new FileEntry(normalizeRelative(root, path), path.getFileName().toString(),
                Files.isDirectory(path), Files.isDirectory(path) ? 0 : Files.size(path));
        } catch (IOException e) {
            return new FileEntry(normalizeRelative(root, path), path.getFileName().toString(),
                Files.isDirectory(path), 0);
        }
    }

    private String normalizeRelative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private boolean isBinary(Path file) throws IOException {
        byte[] sample;
        try (var input = Files.newInputStream(file)) {
            sample = input.readNBytes(4096);
        }
        for (byte value : sample) if (value == 0) return true;
        return false;
    }

    public record FileEntry(String path, String name, boolean directory, long size) { }
    public record WorkspaceResult(String root, int fileCount, List<FileEntry> files) { }
    public record FileContent(String path, String content, long size) { }
    public record CommandResult(String command, String output, boolean success) { }
}
