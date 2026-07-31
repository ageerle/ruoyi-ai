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
 * 写文件工具
 * 新建或覆盖文件，自动创建父目录
 *
 * <p>编程能力专用：通过构造注入工作目录与 SSE 事件通道，操作前后推送 add-start/add-end 事件。
 * 不注册为 BuiltinToolProvider（无需进 BuiltinToolRegistry），仅由 CodingServiceImpl 按会话 new。
 *
 * @author ageerle
 */
@Component
public class WriteFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Creates or overwrites a file with the given content. " +
        "Creates parent directories if missing. Overwrites existing file. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    private final CodingEventChannel channel;

    public WriteFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        this.channel = null;
    }

    /**
     * 编程能力专用构造。
     */
    public WriteFileTool(Path root, CodingEventChannel channel) {
        this.rootDirectory = root.toAbsolutePath().normalize().toString();
        this.channel = channel;
    }

    /**
     * 写文件
     *
     * @param filePath 文件绝对路径
     * @param content  文件内容
     * @return 操作结果
     */
    @Tool(DESCRIPTION)
    public String writeFile(String filePath, String content) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
            }
            if (content == null) {
                content = "";
            }

            Path path = Paths.get(filePath);

            if (!path.isAbsolute()) {
                return "Error: File path must be absolute: " + filePath;
            }

            if (!WorkspaceGuard.isWithinWorkspace(Paths.get(rootDirectory), path)) {
                return "Error: File path must be within the workspace directory (" + rootDirectory + "): " + filePath;
            }

            if (channel != null) {
                channel.send(CodingSseEvent.thinking("正在新建/写入文件：" + filePath));
                channel.send(CodingSseEvent.of("add-start", filePath, null, null, "running"));
            }

            // 创建父目录
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // 写入文件
            Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String relativePath = getRelativePath(path);
            int bytes = content.getBytes(StandardCharsets.UTF_8).length;

            if (channel != null) {
                channel.send(CodingSseEvent.of("add-end", filePath, null,
                    "写入 " + bytes + " 字节", "done"));
            }
            return String.format("Successfully wrote %d bytes to %s", bytes, relativePath);

        } catch (IOException e) {
            logger.error("Error writing file: {}", filePath, e);
            if (channel != null) {
                channel.send(CodingSseEvent.of("add-end", filePath, null,
                    "Error: " + e.getMessage(), "done"));
            }
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error writing file: {}", filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
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
        return "write_file";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
