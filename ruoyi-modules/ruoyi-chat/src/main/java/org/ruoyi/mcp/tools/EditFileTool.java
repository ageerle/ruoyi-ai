package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.ruoyi.service.coding.CodingEventChannel;
import org.ruoyi.service.coding.CodingSseEvent;
import org.ruoyi.service.coding.WorkspaceGuard;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 编辑文件工具
 * 支持基于diff的文件编辑
 */
@Component
public class EditFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Edits an existing file by replacing its full content. " +
        "ALWAYS read the file first with read_file, then provide the COMPLETE new content here. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    /** 编程能力 SSE 事件通道，可为 null（兼容无参构造的老调用方） */
    private final CodingEventChannel channel;

    public EditFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        this.channel = null;
    }

    /**
     * 编程能力专用构造：注入会话工作目录与事件通道。
     */
    public EditFileTool(Path root, CodingEventChannel channel) {
        this.rootDirectory = root.toAbsolutePath().normalize().toString();
        this.channel = channel;
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
            if (!WorkspaceGuard.isWithinWorkspace(Paths.get(rootDirectory), path)) {
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

            // 推送编辑开始事件
            String relativePath = getRelativePath(path);
            if (channel != null) {
                channel.send(CodingSseEvent.of("edit-start", filePath, null, null, "running"));
            }

            // 应用diff（简化：整体替换为新内容）
            try {
                String newContent = applyDiff(null, diff);

                Files.writeString(path, newContent, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                if (channel != null) {
                    channel.send(CodingSseEvent.of("edit-end", filePath, null, null, "done"));
                }
                return String.format("Successfully edited file: %s", relativePath);

            } catch (Exception e) {
                if (channel != null) {
                    channel.send(CodingSseEvent.of("edit-end", filePath, null, "Error: " + e.getMessage(), "done"));
                }
                return "Error: Failed to apply diff: " + e.getMessage();
            }

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
        return WorkspaceGuard.isWithinWorkspace(Paths.get(rootDirectory), filePath);
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
