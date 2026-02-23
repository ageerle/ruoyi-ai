package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
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
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public ReadFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
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

            // 读取文件内容
            String content = Files.readString(path, StandardCharsets.UTF_8);

            // 获取相对路径
            String relativePath = getRelativePath(path);
            long sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
            long lineCount = content.lines().count();

            return String.format("File: %s (%d lines, %d bytes)\n\n%s",
                relativePath, lineCount, sizeBytes, content);

        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error reading file: {}", filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
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
