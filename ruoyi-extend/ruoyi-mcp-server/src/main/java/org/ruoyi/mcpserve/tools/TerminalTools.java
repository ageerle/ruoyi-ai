package org.ruoyi.mcpserve.tools;

import org.ruoyi.mcpserve.config.ToolsProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 终端命令工具类
 *
 * @author OpenX
 */
@Component
public class TerminalTools implements McpTool {

    public static final String TOOL_NAME = "terminal";

    private final ToolsProperties toolsProperties;

    public TerminalTools(ToolsProperties toolsProperties) {
        this.toolsProperties = toolsProperties;
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "在终端中执行命令")
    public String executeTerminalCommand(
            @ToolParam(description = "要执行的终端命令") String command) {
        StringBuilder output = new StringBuilder();
        try {
            String projectRoot = System.getProperty("user.dir");
            String fileDir = toolsProperties.getFile().getSaveDir() + "/file";
            File workingDir = new File(projectRoot, fileDir);

            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }

            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
            }
            processBuilder.directory(workingDir);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
        }
        return output.toString();
    }
}
