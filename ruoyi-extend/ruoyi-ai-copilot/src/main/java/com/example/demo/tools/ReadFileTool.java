package com.example.demo.tools;

import com.example.demo.config.AppProperties;
import com.example.demo.schema.JsonSchema;
import com.example.demo.service.ToolExecutionLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文件读取工具
 * 支持读取文本文件，可以分页读取大文件
 */
@Component
public class ReadFileTool extends BaseTool<ReadFileTool.ReadFileParams> {

    private final String rootDirectory;
    private final AppProperties appProperties;

    @Autowired
    private ToolExecutionLogger executionLogger;

    public ReadFileTool(AppProperties appProperties) {
        super(
                "read_file",
                "ReadFile",
                "Reads and returns the content of a specified file from the local filesystem. " +
                        "Handles text files and supports pagination for large files. " +
                        "Always use absolute paths within the workspace directory.",
                createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }

    private static String getWorkspaceBasePath() {
        return Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    private static String getPathExample(String subPath) {
        return "Example: \"" + Paths.get(getWorkspaceBasePath(), subPath).toString() + "\"";
    }

    private static JsonSchema createSchema() {
        return JsonSchema.object()
                .addProperty("absolute_path", JsonSchema.string(
                        "MUST be an absolute path to the file to read. Path must be within the workspace directory (" +
                                getWorkspaceBasePath() + "). " +
                                getPathExample("project/src/main.java") + ". " +
                                "Relative paths are NOT allowed."
                ))
                .addProperty("offset", JsonSchema.integer(
                        "Optional: For text files, the 0-based line number to start reading from. " +
                                "Requires 'limit' to be set. Use for paginating through large files."
                ).minimum(0))
                .addProperty("limit", JsonSchema.integer(
                        "Optional: For text files, the number of lines to read from the offset. " +
                                "Use for paginating through large files."
                ).minimum(1))
                .required("absolute_path");
    }

    @Override
    public String validateToolParams(ReadFileParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        // 验证路径
        if (params.absolutePath == null || params.absolutePath.trim().isEmpty()) {
            return "File path cannot be empty";
        }

        Path filePath = Paths.get(params.absolutePath);

        // 验证是否为绝对路径
        if (!filePath.isAbsolute()) {
            return "File path must be absolute: " + params.absolutePath;
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(filePath)) {
            return "File path must be within the workspace directory (" + rootDirectory + "): " + params.absolutePath;
        }

        // 验证分页参数
        if (params.offset != null && params.limit == null) {
            return "When 'offset' is specified, 'limit' must also be specified";
        }

        if (params.offset != null && params.offset < 0) {
            return "Offset must be non-negative";
        }

        if (params.limit != null && params.limit <= 0) {
            return "Limit must be positive";
        }

        return null;
    }

    /**
     * Read file tool method for Spring AI integration
     */
    @Tool(name = "read_file", description = "Reads and returns the content of a specified file from the local filesystem")
    public String readFile(String absolutePath, Integer offset, Integer limit) {
        long callId = executionLogger.logToolStart("read_file", "读取文件内容",
                String.format("文件路径=%s, 偏移量=%s, 限制行数=%s", absolutePath, offset, limit));
        long startTime = System.currentTimeMillis();

        try {
            ReadFileParams params = new ReadFileParams();
            params.setAbsolutePath(absolutePath);
            params.setOffset(offset);
            params.setLimit(limit);

            executionLogger.logToolStep(callId, "read_file", "参数验证", "验证文件路径和分页参数");

            // Validate parameters
            String validation = validateToolParams(params);
            if (validation != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                executionLogger.logToolError(callId, "read_file", "参数验证失败: " + validation, executionTime);
                return "Error: " + validation;
            }

            executionLogger.logFileOperation(callId, "读取文件", absolutePath,
                    offset != null ? String.format("分页读取: 偏移=%d, 限制=%d", offset, limit) : "完整读取");

            // Execute the tool
            ToolResult result = execute(params).join();

            long executionTime = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                executionLogger.logToolSuccess(callId, "read_file", "文件读取成功", executionTime);
                return result.getLlmContent();
            } else {
                executionLogger.logToolError(callId, "read_file", result.getErrorMessage(), executionTime);
                return "Error: " + result.getErrorMessage();
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            executionLogger.logToolError(callId, "read_file", "工具执行异常: " + e.getMessage(), executionTime);
            logger.error("Error in read file tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(ReadFileParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Paths.get(params.absolutePath);

                // 检查文件是否存在
                if (!Files.exists(filePath)) {
                    return ToolResult.error("File not found: " + params.absolutePath);
                }

                // 检查是否为文件
                if (!Files.isRegularFile(filePath)) {
                    return ToolResult.error("Path is not a regular file: " + params.absolutePath);
                }

                // 检查文件大小
                long fileSize = Files.size(filePath);
                if (fileSize > appProperties.getWorkspace().getMaxFileSize()) {
                    return ToolResult.error("File too large: " + fileSize + " bytes. Maximum allowed: " +
                            appProperties.getWorkspace().getMaxFileSize() + " bytes");
                }

                // 检查文件扩展名
                String fileName = filePath.getFileName().toString();
                if (!isAllowedFileType(fileName)) {
                    return ToolResult.error("File type not allowed: " + fileName +
                            ". Allowed extensions: " + appProperties.getWorkspace().getAllowedExtensions());
                }

                // 读取文件
                if (params.offset != null && params.limit != null) {
                    return readFileWithPagination(filePath, params.offset, params.limit);
                } else {
                    return readFullFile(filePath);
                }

            } catch (IOException e) {
                logger.error("Error reading file: " + params.absolutePath, e);
                return ToolResult.error("Error reading file: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error reading file: " + params.absolutePath, e);
                return ToolResult.error("Unexpected error: " + e.getMessage());
            }
        });
    }

    private ToolResult readFullFile(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relativePath = getRelativePath(filePath);

        long lineCount = content.lines().count();
        String displayMessage = String.format("Read file: %s (%d lines, %d bytes)",
                relativePath, lineCount, content.getBytes(StandardCharsets.UTF_8).length);

        return ToolResult.success(content, displayMessage);
    }

    private ToolResult readFileWithPagination(Path filePath, int offset, int limit) throws IOException {
        List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        if (offset >= allLines.size()) {
            return ToolResult.error("Offset " + offset + " is beyond file length (" + allLines.size() + " lines)");
        }

        int endIndex = Math.min(offset + limit, allLines.size());
        List<String> selectedLines = allLines.subList(offset, endIndex);
        String content = String.join("\n", selectedLines);

        String relativePath = getRelativePath(filePath);
        String displayMessage = String.format("Read file: %s (lines %d-%d of %d total)",
                relativePath, offset + 1, endIndex, allLines.size());

        return ToolResult.success(content, displayMessage);
    }

    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path resolvedPath = filePath.toRealPath();
            return resolvedPath.startsWith(workspaceRoot);
        } catch (IOException e) {
            // 如果路径不存在，检查其父目录
            try {
                Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
                Path normalizedPath = filePath.normalize();
                return normalizedPath.startsWith(workspaceRoot.normalize());
            } catch (IOException ex) {
                logger.warn("Could not resolve workspace path", ex);
                return false;
            }
        }
    }

    private boolean isAllowedFileType(String fileName) {
        List<String> allowedExtensions = appProperties.getWorkspace().getAllowedExtensions();
        return allowedExtensions.stream()
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
    }

    private String getRelativePath(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory);
            return workspaceRoot.relativize(filePath).toString();
        } catch (Exception e) {
            return filePath.toString();
        }
    }

    /**
     * 读取文件参数
     */
    public static class ReadFileParams {
        @JsonProperty("absolute_path")
        private String absolutePath;

        private Integer offset;
        private Integer limit;

        // 构造器
        public ReadFileParams() {
        }

        public ReadFileParams(String absolutePath) {
            this.absolutePath = absolutePath;
        }

        public ReadFileParams(String absolutePath, Integer offset, Integer limit) {
            this.absolutePath = absolutePath;
            this.offset = offset;
            this.limit = limit;
        }

        // Getters and Setters
        public String getAbsolutePath() {
            return absolutePath;
        }

        public void setAbsolutePath(String absolutePath) {
            this.absolutePath = absolutePath;
        }

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        @Override
        public String toString() {
            return String.format("ReadFileParams{path='%s', offset=%d, limit=%d}",
                    absolutePath, offset, limit);
        }
    }
}
