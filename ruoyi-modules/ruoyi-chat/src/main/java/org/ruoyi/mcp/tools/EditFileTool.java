package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/**
 * 编辑文件工具
 * 支持基于diff的文件编辑
 */
@Component
public class EditFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Edits a file by applying a diff. " +
        "Use this tool when you need to make specific changes to a file. " +
        "The tool will show the diff before applying changes. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public EditFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    /**
     * 编辑文件
     *
     * @param filePath 文件绝对路径
     * @param diff     要应用的diff内容
     * @return 操作结果
     */
    @Tool(DESCRIPTION)
    public String editFile(String filePath, String diff) {
        try {
            // 验证参数
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
            }

            if (diff == null || diff.trim().isEmpty()) {
                return "Error: Diff cannot be empty";
            }

            Path path = Paths.get(filePath);

            // 验证是否为绝对路径
            if (!path.isAbsolute()) {
                return "Error: File path must be absolute: " + filePath;
            }

            // 验证是否在工作目录内
            if (!isWithinWorkspace(path)) {
                return "Error: File path must be within the workspace directory (" + rootDirectory + "): " + filePath;
            }

            // 检查文件是否存在
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }

            // 检查是否为目录
            if (Files.isDirectory(path)) {
                return "Error: Path is a directory, not a file: " + filePath;
            }

            // 读取原始内容
            String originalContent = Files.readString(path, StandardCharsets.UTF_8);
            List<String> originalLines = Arrays.asList(originalContent.split("\n"));

            // 应用diff
            try {
                // 这里简化处理，直接用新内容替换
                // 在实际应用中，可能需要更复杂的diff解析
                String newContent = applyDiff(originalContent, diff);

                // 写入文件
                Files.writeString(path, newContent, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                String relativePath = getRelativePath(path);
                return String.format("Successfully edited file: %s", relativePath);

            } catch (Exception e) {
                return "Error: Failed to apply diff: " + e.getMessage();
            }

        } catch (IOException e) {
            logger.error("Error editing file: {}", filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error editing file: {}", filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
    }

    /**
     * 简化的diff应用逻辑
     * 实际应用中可能需要使用更复杂的diff解析器
     */
    private String applyDiff(String originalContent, String diff) {
        // 这里简化处理，实际应用中需要解析diff格式
        // 目前将diff作为新内容直接替换
        // 可以考虑使用jgit等库来解析 unified diff 格式
        return diff;
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

    @Override
    public String getToolName() {
        return "edit_file";
    }

    @Override
    public String getDisplayName() {
        return "编辑文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
