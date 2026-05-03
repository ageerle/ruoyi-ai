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

/**
 * 创建文件工具
 * 在工作区内创建新文件，可选择是否覆盖已有文件
 */
@Component
public class CreateFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Creates a new file with provided content. " +
        "Supports creating parent directories automatically. " +
        "Use absolute paths within the workspace directory. " +
        "Set overwrite to true to replace existing file content.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public CreateFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    @Tool(DESCRIPTION)
    public String createFile(String filePath, String content, Boolean overwrite) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
            }

            Path path = Paths.get(filePath);
            if (!path.isAbsolute()) {
                return "Error: File path must be absolute: " + filePath;
            }

            if (!isWithinWorkspace(path)) {
                return "Error: File path must be within the workspace directory (" + rootDirectory + "): " + filePath;
            }

            if (Files.exists(path) && Files.isDirectory(path)) {
                return "Error: Path is a directory, not a file: " + filePath;
            }

            boolean allowOverwrite = overwrite != null && overwrite;
            if (Files.exists(path) && !allowOverwrite) {
                return "Error: File already exists. Set overwrite=true to replace content: " + filePath;
            }

            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            String safeContent = content == null ? "" : content;
            if (allowOverwrite) {
                Files.writeString(path, safeContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } else {
                Files.writeString(path, safeContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }

            return "Successfully created file: " + getRelativePath(path);
        } catch (IOException e) {
            logger.error("Error creating file: {}", filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error creating file: {}", filePath, e);
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
        return "create_file";
    }

    @Override
    public String getDisplayName() {
        return "创建文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
