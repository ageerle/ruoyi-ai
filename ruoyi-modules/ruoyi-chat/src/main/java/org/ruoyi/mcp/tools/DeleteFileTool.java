package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.ruoyi.service.coding.CodingEventChannel;
import org.ruoyi.service.coding.CodingSseEvent;
import org.ruoyi.service.coding.WorkspaceGuard;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 删除文件/目录工具
 *
 * <p>编程能力专用：通过构造注入工作目录与 SSE 事件通道，操作前后推送 delete-start/delete-end 事件。
 *
 * @author ageerle
 */
@Component
public class DeleteFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Deletes a file or directory. " +
        "Set recursive=true to delete a non-empty directory. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    private final CodingEventChannel channel;

    public DeleteFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        this.channel = null;
    }

    /**
     * 编程能力专用构造。
     */
    public DeleteFileTool(Path root, CodingEventChannel channel) {
        this.rootDirectory = root.toAbsolutePath().normalize().toString();
        this.channel = channel;
    }

    /**
     * 删除文件或目录
     *
     * @param filePath  路径绝对路径
     * @param recursive 是否递归删除非空目录（可选，默认 false）
     * @return 操作结果
     */
    @Tool(DESCRIPTION)
    public String deleteFile(String filePath, Boolean recursive) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
            }

            Path path = Paths.get(filePath);
            boolean rec = recursive != null && recursive;

            if (!path.isAbsolute()) {
                return "Error: File path must be absolute: " + filePath;
            }

            if (!WorkspaceGuard.isWithinWorkspace(Paths.get(rootDirectory), path)) {
                return "Error: File path must be within the workspace directory (" + rootDirectory + "): " + filePath;
            }

            if (!Files.exists(path)) {
                return "Error: Path not found: " + filePath;
            }

            if (channel != null) {
                channel.send(CodingSseEvent.of("delete-start", filePath, null, null, "running"));
            }

            String relativePath = getRelativePath(path);

            if (Files.isDirectory(path)) {
                if (rec) {
                    AtomicLong count = new AtomicLong(0);
                    Files.walkFileTree(path, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            count.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    if (channel != null) {
                        channel.send(CodingSseEvent.of("delete-end", filePath, null,
                            "删除目录及 " + count.get() + " 个文件", "done"));
                    }
                    return String.format("Successfully deleted directory: %s (%d files)", relativePath, count.get());
                } else {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        if (channel != null) {
                            channel.send(CodingSseEvent.of("delete-end", filePath, null,
                                "Error: 非空目录需 recursive=true", "done"));
                        }
                        return "Error: Directory not empty, set recursive=true: " + filePath;
                    }
                    if (channel != null) {
                        channel.send(CodingSseEvent.of("delete-end", filePath, null, null, "done"));
                    }
                    return String.format("Successfully deleted directory: %s", relativePath);
                }
            } else {
                Files.delete(path);
                if (channel != null) {
                    channel.send(CodingSseEvent.of("delete-end", filePath, null, null, "done"));
                }
                return String.format("Successfully deleted file: %s", relativePath);
            }

        } catch (IOException e) {
            logger.error("Error deleting file: {}", filePath, e);
            if (channel != null) {
                channel.send(CodingSseEvent.of("delete-end", filePath, null,
                    "Error: " + e.getMessage(), "done"));
            }
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error deleting file: {}", filePath, e);
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
        return "delete_file";
    }

    @Override
    public String getDisplayName() {
        return "删除文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
