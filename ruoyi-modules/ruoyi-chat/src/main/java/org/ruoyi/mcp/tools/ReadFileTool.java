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

/**
 * 读取文件工具
 * 读取指定路径的文件内容
 */
@Component
public class ReadFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Reads the contents of a file. " +
        "Use absolute paths within the workspace directory. " +
        "Returns the complete file content as a string.";

    private final String rootDirectory;
    /** 读取内容截断上限，避免大文件撑爆 LLM 上下文（32KB） */
    private static final int MAX_BYTES = 32 * 1024;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    /** 编程能力 SSE 事件通道，可为 null（兼容 BuiltinToolRegistry 无参构造的老调用方） */
    private final CodingEventChannel channel;

    public ReadFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        this.channel = null;
    }

    /**
     * 编程能力专用构造：注入会话工作目录与事件通道。
     *
     * @param root    工作目录根（绝对路径）
     * @param channel SSE 事件通道，工具执行前后推送 read 进度
     */
    public ReadFileTool(Path root, CodingEventChannel channel) {
        this.rootDirectory = root.toAbsolutePath().normalize().toString();
        this.channel = channel;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件绝对路径
     * @return 文件内容
     */
    @Tool(DESCRIPTION)
    public String readFile(String filePath) {
        try {
            // 验证参数
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
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

            // 推送读取开始事件（前端展示为正在读取）
            String relativePath = getRelativePath(path);
            if (channel != null) {
                channel.send(CodingSseEvent.thinking("正在读取文件：" + filePath));
                channel.send(CodingSseEvent.of("edit-start", filePath, null, null, "running"));
            }

            // 读取文件内容（截断超大文件，避免撑爆上下文）
            String content = Files.readString(path, StandardCharsets.UTF_8);
            boolean truncated = false;
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES) {
                content = new String(bytes, 0, MAX_BYTES, StandardCharsets.UTF_8);
                truncated = true;
            }

            long sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
            long lineCount = content.lines().count();
            String header = String.format("File: %s (%d lines, %d bytes)%s\n\n",
                relativePath, lineCount, sizeBytes, truncated ? " [truncated]" : "");

            if (channel != null) {
                channel.send(CodingSseEvent.of("edit-end", filePath, null, null, "done"));
            }

            return header + content;

        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error reading file: {}", filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
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
        return "read_file";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
