package com.example.demo.tools;

import com.example.demo.config.AppProperties;
import com.example.demo.schema.JsonSchema;
import com.example.demo.service.ToolExecutionLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * File editing tool
 * Supports file editing based on string replacement, automatically shows differences
 */
@Component
public class EditFileTool extends BaseTool<EditFileTool.EditFileParams> {

    private final String rootDirectory;
    private final AppProperties appProperties;

    @Autowired
    private ToolExecutionLogger executionLogger;

    public EditFileTool(AppProperties appProperties) {
        super(
            "edit_file",
            "EditFile",
            "Edits a file by replacing specified text with new text. " +
            "Shows a diff of the changes before applying them. " +
            "Supports both exact string matching and line-based editing. " +
            "Use absolute paths within the workspace directory.",
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
            .addProperty("file_path", JsonSchema.string(
                "MUST be an absolute path to the file to edit. Path must be within the workspace directory (" + 
                getWorkspaceBasePath() + "). " +
                getPathExample("project/src/main.java") + ". " +
                "Relative paths are NOT allowed."
            ))
            .addProperty("old_str", JsonSchema.string(
                "The exact string to find and replace. Must match exactly including whitespace and newlines."
            ))
            .addProperty("new_str", JsonSchema.string(
                "The new string to replace the old string with. Can be empty to delete the old string."
            ))
            .addProperty("start_line", JsonSchema.integer(
                "Optional: 1-based line number where the old_str starts. Helps with disambiguation."
            ).minimum(1))
            .addProperty("end_line", JsonSchema.integer(
                "Optional: 1-based line number where the old_str ends. Must be >= start_line."
            ).minimum(1))
            .required("file_path", "old_str", "new_str");
    }

    @Override
    public String validateToolParams(EditFileParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        // 验证路径
        if (params.filePath == null || params.filePath.trim().isEmpty()) {
            return "File path cannot be empty";
        }

        if (params.oldStr == null) {
            return "Old string cannot be null";
        }

        if (params.newStr == null) {
            return "New string cannot be null";
        }

        Path filePath = Paths.get(params.filePath);
        
        // Validate if it's an absolute path
        if (!filePath.isAbsolute()) {
            return "File path must be absolute: " + params.filePath;
        }

        // 验证是否在工作目录内
        if (!isWithinWorkspace(filePath)) {
            return "File path must be within the workspace directory (" + rootDirectory + "): " + params.filePath;
        }

        // 验证行号
        if (params.startLine != null && params.endLine != null) {
            if (params.endLine < params.startLine) {
                return "End line must be >= start line";
            }
        }

        return null;
    }

    @Override
    public CompletableFuture<ToolConfirmationDetails> shouldConfirmExecute(EditFileParams params) {
        // Decide whether confirmation is needed based on configuration
        if (appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.AUTO_EDIT ||
            appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.YOLO) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Paths.get(params.filePath);
                
                if (!Files.exists(filePath)) {
                    return null; // 文件不存在，无法预览差异
                }
                
                String currentContent = Files.readString(filePath, StandardCharsets.UTF_8);
                String newContent = performEdit(currentContent, params);
                
                if (newContent == null) {
                    return null; // Edit failed, cannot preview differences
                }
                
                // 生成差异显示
                String diff = generateDiff(filePath.getFileName().toString(), currentContent, newContent);
                String title = "Confirm Edit: " + getRelativePath(filePath);
                
                return ToolConfirmationDetails.edit(title, filePath.getFileName().toString(), diff);
                    
            } catch (IOException e) {
                logger.warn("Could not read file for edit preview: " + params.filePath, e);
                return null;
            }
        });
    }

    /**
     * Edit file tool method for Spring AI integration
     */
    @Tool(name = "edit_file", description = "Edits a file by replacing specified text with new text")
    public String editFile(String filePath, String oldStr, String newStr, Integer startLine, Integer endLine) {
        long callId = executionLogger.logToolStart("edit_file", "编辑文件内容",
            String.format("文件=%s, 替换文本长度=%d->%d, 行号范围=%s-%s",
                filePath, oldStr != null ? oldStr.length() : 0,
                newStr != null ? newStr.length() : 0, startLine, endLine));
        long startTime = System.currentTimeMillis();

        try {
            EditFileParams params = new EditFileParams();
            params.setFilePath(filePath);
            params.setOldStr(oldStr);
            params.setNewStr(newStr);
            params.setStartLine(startLine);
            params.setEndLine(endLine);

            executionLogger.logToolStep(callId, "edit_file", "参数验证", "验证文件路径和替换内容");

            // Validate parameters
            String validation = validateToolParams(params);
            if (validation != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                executionLogger.logToolError(callId, "edit_file", "参数验证失败: " + validation, executionTime);
                return "Error: " + validation;
            }

            String editDetails = startLine != null && endLine != null ?
                String.format("行号范围编辑: %d-%d行", startLine, endLine) : "字符串替换编辑";
            executionLogger.logFileOperation(callId, "编辑文件", filePath, editDetails);

            // Execute the tool
            ToolResult result = execute(params).join();

            long executionTime = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                executionLogger.logToolSuccess(callId, "edit_file", "文件编辑成功", executionTime);
                return result.getLlmContent();
            } else {
                executionLogger.logToolError(callId, "edit_file", result.getErrorMessage(), executionTime);
                return "Error: " + result.getErrorMessage();
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            executionLogger.logToolError(callId, "edit_file", "工具执行异常: " + e.getMessage(), executionTime);
            logger.error("Error in edit file tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(EditFileParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Paths.get(params.filePath);
                
                // Check if file exists
                if (!Files.exists(filePath)) {
                    return ToolResult.error("File not found: " + params.filePath);
                }

                // Check if it's a file
                if (!Files.isRegularFile(filePath)) {
                    return ToolResult.error("Path is not a regular file: " + params.filePath);
                }

                // 读取原始内容
                String originalContent = Files.readString(filePath, StandardCharsets.UTF_8);
                
                // 执行编辑
                String newContent = performEdit(originalContent, params);
                if (newContent == null) {
                    return ToolResult.error("Could not find the specified text to replace in file: " + params.filePath);
                }
                
                // 创建备份
                if (shouldCreateBackup()) {
                    createBackup(filePath, originalContent);
                }
                
                // Write new content
                Files.writeString(filePath, newContent, StandardCharsets.UTF_8);
                
                // Generate differences and results
                String diff = generateDiff(filePath.getFileName().toString(), originalContent, newContent);
                String relativePath = getRelativePath(filePath);
                String successMessage = String.format("Successfully edited file: %s", params.filePath);
                
                return ToolResult.success(successMessage, new FileDiff(diff, filePath.getFileName().toString()));

            } catch (IOException e) {
                logger.error("Error editing file: " + params.filePath, e);
                return ToolResult.error("Error editing file: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error editing file: " + params.filePath, e);
                return ToolResult.error("Unexpected error: " + e.getMessage());
            }
        });
    }

    private String performEdit(String content, EditFileParams params) {
        // If line numbers are specified, use line numbers to assist in finding
        if (params.startLine != null && params.endLine != null) {
            return performEditWithLineNumbers(content, params);
        } else {
            return performSimpleEdit(content, params);
        }
    }

    private String performSimpleEdit(String content, EditFileParams params) {
        // Simple string replacement
        if (!content.contains(params.oldStr)) {
            return null; // Cannot find string to replace
        }
        
        // Only replace the first match to avoid unexpected multiple replacements
        int index = content.indexOf(params.oldStr);
        if (index == -1) {
            return null;
        }
        
        return content.substring(0, index) + params.newStr + content.substring(index + params.oldStr.length());
    }

    private String performEditWithLineNumbers(String content, EditFileParams params) {
        String[] lines = content.split("\n", -1); // -1 preserve trailing empty lines

        // Validate line number range
        if (params.startLine > lines.length || params.endLine > lines.length) {
            return null; // Line number out of range
        }

        // Extract content from specified line range
        StringBuilder targetContent = new StringBuilder();
        for (int i = params.startLine - 1; i < params.endLine; i++) {
            if (i > params.startLine - 1) {
                targetContent.append("\n");
            }
            targetContent.append(lines[i]);
        }
        
        // 检查是否匹配
        if (!targetContent.toString().equals(params.oldStr)) {
            return null; // 指定行范围的内容与old_str不匹配
        }

        // 执行替换
        StringBuilder result = new StringBuilder();

        // 添加前面的行
        for (int i = 0; i < params.startLine - 1; i++) {
            if (i > 0) result.append("\n");
            result.append(lines[i]);
        }
        
        // 添加新内容
        if (params.startLine > 1) result.append("\n");
        result.append(params.newStr);
        
        // 添加后面的行
        for (int i = params.endLine; i < lines.length; i++) {
            result.append("\n");
            result.append(lines[i]);
        }
        
        return result.toString();
    }

    private String generateDiff(String fileName, String oldContent, String newContent) {
        try {
            List<String> oldLines = Arrays.asList(oldContent.split("\n"));
            List<String> newLines = Arrays.asList(newContent.split("\n"));
            
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                fileName + " (Original)",
                fileName + " (Edited)",
                oldLines,
                patch,
                3 // context lines
            );
            
            return String.join("\n", unifiedDiff);
        } catch (Exception e) {
            logger.warn("Could not generate diff", e);
            return "Diff generation failed: " + e.getMessage();
        }
    }

    private void createBackup(Path filePath, String content) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = filePath.getFileName().toString() + ".backup." + timestamp;
        Path backupPath = filePath.getParent().resolve(backupFileName);
        
        Files.writeString(backupPath, content, StandardCharsets.UTF_8);
        logger.info("Created backup: {}", backupPath);
    }

    private boolean shouldCreateBackup() {
        return true; // 总是创建备份
    }

    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.normalize();
            return normalizedPath.startsWith(workspaceRoot.normalize());
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
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
     * 编辑文件参数
     */
    public static class EditFileParams {
        @JsonProperty("file_path")
        private String filePath;
        
        @JsonProperty("old_str")
        private String oldStr;
        
        @JsonProperty("new_str")
        private String newStr;
        
        @JsonProperty("start_line")
        private Integer startLine;
        
        @JsonProperty("end_line")
        private Integer endLine;

        // 构造器
        public EditFileParams() {}

        public EditFileParams(String filePath, String oldStr, String newStr) {
            this.filePath = filePath;
            this.oldStr = oldStr;
            this.newStr = newStr;
        }

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getOldStr() { return oldStr; }
        public void setOldStr(String oldStr) { this.oldStr = oldStr; }

        public String getNewStr() { return newStr; }
        public void setNewStr(String newStr) { this.newStr = newStr; }

        public Integer getStartLine() { return startLine; }
        public void setStartLine(Integer startLine) { this.startLine = startLine; }

        public Integer getEndLine() { return endLine; }
        public void setEndLine(Integer endLine) { this.endLine = endLine; }

        @Override
        public String toString() {
            return String.format("EditFileParams{path='%s', oldStrLength=%d, newStrLength=%d, lines=%s-%s}", 
                filePath, 
                oldStr != null ? oldStr.length() : 0,
                newStr != null ? newStr.length() : 0,
                startLine, endLine);
        }
    }
}
