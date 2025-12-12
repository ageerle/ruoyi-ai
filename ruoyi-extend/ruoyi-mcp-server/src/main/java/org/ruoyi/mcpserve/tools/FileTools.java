package org.ruoyi.mcpserve.tools;

import cn.hutool.core.io.FileUtil;
import org.ruoyi.mcpserve.config.ToolsProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 文件操作工具类
 *
 * @author OpenX
 */
@Component
public class FileTools implements McpTool {

    public static final String TOOL_NAME = "file";

    private final ToolsProperties toolsProperties;

    public FileTools(ToolsProperties toolsProperties) {
        this.toolsProperties = toolsProperties;
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "读取文件内容")
    public String readFile(@ToolParam(description = "要读取的文件名") String fileName) {
        String fileDir = toolsProperties.getFile().getSaveDir() + "/file";
        String filePath = fileDir + "/" + fileName;
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "写入内容到文件")
    public String writeFile(
            @ToolParam(description = "要写入的文件名") String fileName,
            @ToolParam(description = "要写入的内容") String content) {
        String fileDir = toolsProperties.getFile().getSaveDir() + "/file";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}
