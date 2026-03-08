package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 目录列表工具
 * 列出指定目录的文件和子目录，支持递归列表
 */
@Component
public class ListDirectoryTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Lists files and directories in the specified path. " +
        "Supports recursive listing and filtering. " +
        "Shows file sizes, modification times, and types. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public ListDirectoryTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    /**
     * 列出目录内容
     *
     * @param filePath  目录绝对路径
     * @param recursive 是否递归列出子目录（可选，默认 false）
     * @param maxDepth  最大递归深度（可选，默认 3，范围 1-10）
     * @return 目录列表结果
     */
    @Tool(DESCRIPTION)
    public String listDirectory(String filePath, Boolean recursive, Integer maxDepth) {
        // 创建参数对象
        ListDirectoryParams params = new ListDirectoryParams();
        params.filePath = filePath;
        params.recursive = recursive != null ? recursive : false;
        params.maxDepth = maxDepth != null ? maxDepth : 3;

        return execute(params);
    }

    public String execute(ListDirectoryParams params) {
        try {
            // 验证参数
            String validationError = validateParams(params);
            if (validationError != null) {
                return "Error: " + validationError;
            }

            Path dirPath = Paths.get(params.filePath);

            // 检查目录是否存在
            if (!Files.exists(dirPath)) {
                return "Error: Directory not found: " + params.filePath;
            }

            // 检查是否为目录
            if (!Files.isDirectory(dirPath)) {
                return "Error: Path is not a directory: " + params.filePath;
            }

            // 列出文件和目录
            List<FileInfo> fileInfos = listFiles(dirPath, params);

            // 生成输出
            return formatFileList(fileInfos, params);

        } catch (IOException e) {
            logger.error("Error listing directory: {}", params.filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error listing directory: {}", params.filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
    }

    private String validateParams(ListDirectoryParams params) {
        // 验证路径
        if (params.filePath == null || params.filePath.trim().isEmpty()) {
            return "Directory path cannot be empty";
        }

        Path dirPath = Paths.get(params.filePath);

        // 验证是否为绝对路径
        if (!dirPath.isAbsolute()) {
            return "Directory path must be absolute: " + params.filePath;
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(dirPath)) {
            return "Directory path must be within the workspace directory (" + rootDirectory + "): " + params.filePath;
        }

        // 验证最大深度
        if (params.maxDepth != null && (params.maxDepth < 1 || params.maxDepth > 10)) {
            return "Max depth must be between 1 and 10";
        }

        return null;
    }

    private List<FileInfo> listFiles(Path dirPath, ListDirectoryParams params) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();

        if (params.recursive != null && params.recursive) {
            int maxDepth = params.maxDepth != null ? params.maxDepth : 3;
            listFilesRecursive(dirPath, fileInfos, 0, maxDepth, params);
        } else {
            listFilesInDirectory(dirPath, fileInfos, params);
        }

        // 排序：目录在前，然后按名称排序
        fileInfos.sort(Comparator
            .comparing((FileInfo f) -> !f.isDirectory())
            .thenComparing(FileInfo::name));

        return fileInfos;
    }

    private void listFilesInDirectory(Path dirPath, List<FileInfo> fileInfos, ListDirectoryParams params) throws IOException {
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.forEach(path -> {
                try {
                    FileInfo fileInfo = createFileInfo(path, dirPath);
                    fileInfos.add(fileInfo);
                } catch (IOException e) {
                    logger.warn("Could not get info for file: " + path, e);
                }
            });
        }
    }

    private void listFilesRecursive(Path dirPath, List<FileInfo> fileInfos, int currentDepth, int maxDepth, ListDirectoryParams params) throws IOException {
        if (currentDepth >= maxDepth) {
            return;
        }

        try (Stream<Path> stream = Files.list(dirPath)) {
            List<Path> paths = stream.toList();

            for (Path path : paths) {
                try {
                    FileInfo fileInfo = createFileInfo(path, Paths.get(params.filePath));
                    fileInfos.add(fileInfo);

                    // 如果是目录，递归列出
                    if (Files.isDirectory(path)) {
                        listFilesRecursive(path, fileInfos, currentDepth + 1, maxDepth, params);
                    }
                } catch (IOException e) {
                    logger.warn("Could not get info for file: " + path, e);
                }
            }
        }
    }

    private FileInfo createFileInfo(Path path, Path basePath) throws IOException {
        String name = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);
        long size = isDirectory ? 0 : Files.size(path);

        LocalDateTime lastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(path).toInstant(),
            ZoneId.systemDefault()
        );

        String relativePath = basePath.relativize(path).toString();

        return new FileInfo(name, relativePath, isDirectory, size, lastModified);
    }

    private String formatFileList(List<FileInfo> fileInfos, ListDirectoryParams params) {
        if (fileInfos.isEmpty()) {
            return "Directory is empty.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Directory listing for: %s\n", getRelativePath(Paths.get(params.filePath))));
        sb.append(String.format("Total items: %d\n\n", fileInfos.size()));

        // 表头
        sb.append(String.format("%-4s %-40s %-12s %-20s %s\n",
            "Type", "Name", "Size", "Modified", "Path"));
        sb.append("-".repeat(80)).append("\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (FileInfo fileInfo : fileInfos) {
            String type = fileInfo.isDirectory() ? "DIR" : "FILE";
            String sizeStr = fileInfo.isDirectory() ? "-" : formatFileSize(fileInfo.size());
            String modifiedStr = fileInfo.lastModified().format(formatter);

            sb.append(String.format("%-4s %-40s %-12s %-20s %s\n",
                type,
                truncate(fileInfo.name()),
                sizeStr,
                modifiedStr,
                fileInfo.relativePath()
            ));
        }

        return sb.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String truncate(String str) {
        if (str.length() <= 40) {
            return str;
        }
        return str.substring(0, 40 - 3) + "...";
    }

    private boolean isWithinWorkspace(Path dirPath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = dirPath.normalize();
            return normalizedPath.startsWith(workspaceRoot.normalize());
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }

    private String getRelativePath(Path dirPath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory);
            return workspaceRoot.relativize(dirPath).toString();
        } catch (Exception e) {
            return dirPath.toString();
        }
    }

    @Override
    public String getToolName() {
        return "list_directory";
    }

    @Override
    public String getDisplayName() {
        return "列出目录";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * 文件信息
     */
    public record FileInfo(String name, String relativePath, boolean isDirectory, long size,
                           LocalDateTime lastModified) {
    }

    /**
     * 列表目录参数
     */
    public static class ListDirectoryParams {
        public String filePath;
        public Boolean recursive;
        public Integer maxDepth;
    }
}
