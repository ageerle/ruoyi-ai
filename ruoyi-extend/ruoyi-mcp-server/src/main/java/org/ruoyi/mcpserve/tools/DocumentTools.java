package org.ruoyi.mcpserve.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析工具类
 *
 * @author OpenX
 */
@Component
public class DocumentTools implements McpTool {

    public static final String TOOL_NAME = "document";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "从URL解析文档内容，支持PDF、Word、Excel等格式")
    public String parseDocumentFromUrl(
            @ToolParam(description = "要解析的文档URL地址") String fileUrl) {
        try {
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new UrlResource(fileUrl));
            List<Document> documents = tikaDocumentReader.read();
            if (documents.isEmpty()) {
                return "No content found in the document.";
            }
            return documents.get(0).getText();
        } catch (Exception e) {
            return "Error parsing document: " + e.getMessage();
        }
    }
}
