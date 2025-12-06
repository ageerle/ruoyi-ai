package com.example.demo.tools;

import com.example.demo.config.AppProperties;
import com.example.demo.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具类 - 使用Spring AI 1.0.0 @Tool注解
 */
@Component
public class FileOperationTools {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationTools.class);

    private final String rootDirectory;
    private final AppProperties appProperties;

    // 在构造函数中
    public FileOperationTools(AppProperties appProperties) {
        this.appProperties = appProperties;
        // 使用规范化的路径
        this.rootDirectory = PathUtils.normalizePath(appProperties.getWorkspace().getRootDirectory());
    }

    @Tool(description = "Read the content of a file from the local filesystem. Supports pagination for large files.")
    public String readFile(
            @ToolParam(description = "The absolute path to the file to read. Must be within the workspace directory.")
            String absolutePath,
            @ToolParam(description = "Optional: For text files, the 0-based line number to start reading from.", required = false)
            Integer offset,
            @ToolParam(description = "Optional: For text files, the number of lines to read from the offset.", required = false)
            Integer limit) {

        long startTime = System.currentTimeMillis();
        try {
            logger.debug("Starting readFile operation for: {}", absolutePath);
            // 验证路径
            String validationError = validatePath(absolutePath);
            if (validationError != null) {
                return "Error: " + validationError;
            }

            Path filePath = Paths.get(absolutePath);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return "Error: File not found: " + absolutePath;
            }

            // 检查是否为文件
            if (!Files.isRegularFile(filePath)) {
                return "Error: Path is not a regular file: " + absolutePath;
            }

            // 检查文件大小
            long fileSize = Files.size(filePath);
            if (fileSize > appProperties.getWorkspace().getMaxFileSize()) {
                return "Error: File too large: " + fileSize + " bytes. Maximum allowed: " +
                        appProperties.getWorkspace().getMaxFileSize() + " bytes";
            }

            // 检查文件扩展名
            String fileName = filePath.getFileName().toString();
            if (!isAllowedFileType(fileName)) {
                return "Error: File type not allowed: " + fileName +
                        ". Allowed extensions: " + appProperties.getWorkspace().getAllowedExtensions();
            }

            // 读取文件
            if (offset != null && limit != null) {
                return readFileWithPagination(filePath, offset, limit);
            } else {
                return readFullFile(filePath);
            }

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error reading file: {} (duration: {}ms)", absolutePath, duration, e);
            return String.format("❌ Error reading file: %s\n⏱️ Duration: %dms\n🔍 Details: %s",
                    absolutePath, duration, e.getMessage());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error reading file: {} (duration: {}ms)", absolutePath, duration, e);
            return String.format("❌ Unexpected error reading file: %s\n⏱️ Duration: %dms\n🔍 Details: %s",
                    absolutePath, duration, e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Completed readFile operation for: {} (duration: {}ms)", absolutePath, duration);
        }
    }

    @Tool(description = "Write content to a file. Creates new file or overwrites existing file.")
    public String writeFile(
            @ToolParam(description = "The absolute path to the file to write. Must be within the workspace directory.")
            String filePath,
            @ToolParam(description = "The content to write to the file")
            String content) {

        long startTime = System.currentTimeMillis();
        try {
            logger.debug("Starting writeFile operation for: {}", filePath);
            // 验证路径
            String validationError = validatePath(filePath);
            if (validationError != null) {
                return "Error: " + validationError;
            }

            // 验证内容大小
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            if (contentBytes.length > appProperties.getWorkspace().getMaxFileSize()) {
                return "Error: Content too large: " + contentBytes.length + " bytes. Maximum allowed: " +
                        appProperties.getWorkspace().getMaxFileSize() + " bytes";
            }

            Path path = Paths.get(filePath);
            boolean isNewFile = !Files.exists(path);

            // 确保父目录存在
            Files.createDirectories(path.getParent());

            // 写入文件
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long lineCount = content.lines().count();
            String absolutePath = path.toAbsolutePath().toString();
            String relativePath = getRelativePath(path);

            if (isNewFile) {
                return String.format("Successfully created file:\n📁 Full path: %s\n📂 Relative path: %s\n📊 Stats: %d lines, %d bytes",
                        absolutePath, relativePath, lineCount, contentBytes.length);
            } else {
                return String.format("Successfully wrote to file:\n📁 Full path: %s\n📂 Relative path: %s\n📊 Stats: %d lines, %d bytes",
                        absolutePath, relativePath, lineCount, contentBytes.length);
            }

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error writing file: {} (duration: {}ms)", filePath, duration, e);
            return String.format("❌ Error writing file: %s\n⏱️ Duration: %dms\n🔍 Details: %s",
                    filePath, duration, e.getMessage());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error writing file: {} (duration: {}ms)", filePath, duration, e);
            return String.format("❌ Unexpected error writing file: %s\n⏱️ Duration: %dms\n🔍 Details: %s",
                    filePath, duration, e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Completed writeFile operation for: {} (duration: {}ms)", filePath, duration);
        }
    }

    @Tool(description = "Edit a file by replacing specific text content.")
    public String editFile(
            @ToolParam(description = "The absolute path to the file to edit. Must be within the workspace directory.")
            String filePath,
            @ToolParam(description = "The text to find and replace in the file")
            String oldText,
            @ToolParam(description = "The new text to replace the old text with")
            String newText) {

        try {
            // 验证路径
            String validationError = validatePath(filePath);
            if (validationError != null) {
                return "Error: " + validationError;
            }

            Path path = Paths.get(filePath);

            // 检查文件是否存在
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }

            // 检查是否为文件
            if (!Files.isRegularFile(path)) {
                return "Error: Path is not a regular file: " + filePath;
            }

            // 读取原始内容
            String originalContent = Files.readString(path, StandardCharsets.UTF_8);

            // 执行替换
            if (!originalContent.contains(oldText)) {
                return "Error: Could not find the specified text to replace in file: " + filePath;
            }

            String newContent = originalContent.replace(oldText, newText);

            // 写入新内容
            Files.writeString(path, newContent, StandardCharsets.UTF_8);

            String absolutePath = path.toAbsolutePath().toString();
            String relativePath = getRelativePath(path);
            return String.format("Successfully edited file:\n📁 Full path: %s\n📂 Relative path: %s\n✏️ Replaced text successfully",
                    absolutePath, relativePath);

        } catch (IOException e) {
            logger.error("Error editing file: " + filePath, e);
            return "Error editing file: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error editing file: " + filePath, e);
            return "Unexpected error: " + e.getMessage();
        }
    }

    @Tool(description = "List the contents of a directory.")
    public String listDirectory(
            @ToolParam(description = "The absolute path to the directory to list. Must be within the workspace directory.")
            String directoryPath,
            @ToolParam(description = "Whether to list contents recursively", required = false)
            Boolean recursive) {

        try {
            // 验证路径
            String validationError = validatePath(directoryPath);
            if (validationError != null) {
                return "Error: " + validationError;
            }

            Path path = Paths.get(directoryPath);

            // 检查目录是否存在
            if (!Files.exists(path)) {
                return "Error: Directory not found: " + directoryPath;
            }

            // 检查是否为目录
            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: " + directoryPath;
            }

            boolean isRecursive = recursive != null && recursive;
            String absolutePath = path.toAbsolutePath().toString();
            String relativePath = getRelativePath(path);

            if (isRecursive) {
                return listDirectoryRecursive(path, absolutePath, relativePath);
            } else {
                return listDirectorySimple(path, absolutePath, relativePath);
            }

        } catch (IOException e) {
            logger.error("Error listing directory: " + directoryPath, e);
            return "Error listing directory: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error listing directory: " + directoryPath, e);
            return "Unexpected error: " + e.getMessage();
        }
    }

    // 辅助方法
    private String validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "Path cannot be empty";
        }

        Path filePath = Paths.get(path);

        // 验证是否为绝对路径
        if (!filePath.isAbsolute()) {
            return "Path must be absolute: " + path;
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(filePath)) {
            return "Path must be within the workspace directory (" + rootDirectory + "): " + path;
        }

        return null;
    }

    private boolean isWithinWorkspace(Path path) {
        try {
            Path workspacePath = Paths.get(rootDirectory).toRealPath();
            Path targetPath = path.toRealPath();
            return targetPath.startsWith(workspacePath);
        } catch (IOException e) {
            // 如果路径不存在，检查其父目录
            try {
                Path workspacePath = Paths.get(rootDirectory).toRealPath();
                Path normalizedPath = path.normalize();
                return normalizedPath.startsWith(workspacePath.normalize());
            } catch (IOException ex) {
                return false;
            }
        }
    }

    private boolean isAllowedFileType(String fileName) {
        List<String> allowedExtensions = appProperties.getWorkspace().getAllowedExtensions();
        return allowedExtensions.stream()
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
    }

    private String getRelativePath(Path path) {
        try {
            Path workspacePath = Paths.get(rootDirectory);
            return workspacePath.relativize(path).toString();
        } catch (Exception e) {
            return path.toString();
        }
    }

    private String readFullFile(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String absolutePath = filePath.toAbsolutePath().toString();
        String relativePath = getRelativePath(filePath);

        long lineCount = content.lines().count();
        return String.format("📁 Full path: %s\n📂 Relative path: %s\n📊 Stats: %d lines, %d bytes\n\n📄 Content:\n%s",
                absolutePath, relativePath, lineCount, content.getBytes(StandardCharsets.UTF_8).length, content);
    }

    private String readFileWithPagination(Path filePath, int offset, int limit) throws IOException {
        List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        if (offset >= allLines.size()) {
            return "Error: Offset " + offset + " is beyond file length (" + allLines.size() + " lines)";
        }

        int endIndex = Math.min(offset + limit, allLines.size());
        List<String> selectedLines = allLines.subList(offset, endIndex);
        String content = String.join("\n", selectedLines);

        String absolutePath = filePath.toAbsolutePath().toString();
        String relativePath = getRelativePath(filePath);
        return String.format("📁 Full path: %s\n📂 Relative path: %s\n📊 Showing lines %d-%d of %d total\n\n📄 Content:\n%s",
                absolutePath, relativePath, offset + 1, endIndex, allLines.size(), content);
    }

    private String listDirectorySimple(Path path, String absolutePath, String relativePath) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append("📁 Full path: ").append(absolutePath).append("\n");
        result.append("📂 Relative path: ").append(relativePath).append("\n\n");
        result.append("📋 Directory contents:\n");

        try (Stream<Path> entries = Files.list(path)) {
            List<Path> sortedEntries = entries.sorted().collect(Collectors.toList());

            for (Path entry : sortedEntries) {
                String name = entry.getFileName().toString();
                String entryAbsolutePath = entry.toAbsolutePath().toString();
                if (Files.isDirectory(entry)) {
                    result.append("📁 [DIR]  ").append(name).append("/\n");
                    result.append("   └─ ").append(entryAbsolutePath).append("\n");
                } else {
                    long size = Files.size(entry);
                    result.append("📄 [FILE] ").append(name).append(" (").append(size).append(" bytes)\n");
                    result.append("   └─ ").append(entryAbsolutePath).append("\n");
                }
            }
        }

        return result.toString();
    }

    private String listDirectoryRecursive(Path path, String absolutePath, String relativePath) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append("📁 Full path: ").append(absolutePath).append("\n");
        result.append("📂 Relative path: ").append(relativePath).append("\n\n");
        result.append("🌳 Directory tree (recursive):\n");

        try (Stream<Path> entries = Files.walk(path)) {
            entries.sorted()
                    .forEach(entry -> {
                        if (!entry.equals(path)) {
                            String entryAbsolutePath = entry.toAbsolutePath().toString();
                            String entryRelativePath = getRelativePath(entry);

                            // 计算缩进级别
                            int depth = entry.getNameCount() - path.getNameCount();
                            String indent = "  ".repeat(depth);

                            if (Files.isDirectory(entry)) {
                                result.append(indent).append("📁 ").append(entryRelativePath).append("/\n");
                                result.append(indent).append("   └─ ").append(entryAbsolutePath).append("\n");
                            } else {
                                try {
                                    long size = Files.size(entry);
                                    result.append(indent).append("📄 ").append(entryRelativePath).append(" (").append(size).append(" bytes)\n");
                                    result.append(indent).append("   └─ ").append(entryAbsolutePath).append("\n");
                                } catch (IOException e) {
                                    result.append(indent).append("📄 ").append(entryRelativePath).append(" (size unknown)\n");
                                    result.append(indent).append("   └─ ").append(entryAbsolutePath).append("\n");
                                }
                            }
                        }
                    });
        }

        return result.toString();
    }
}
