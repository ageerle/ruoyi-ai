package org.ruoyi.mcpserve.tools;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * PlantUML工具类
 *
 * @author OpenX
 */
@Component
public class PlantUmlTools implements McpTool {

    public static final String TOOL_NAME = "plantuml";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "生成PlantUML图表并返回SVG代码")
    public String generatePlantUmlSvg(
            @ToolParam(description = "UML图表源代码") String umlCode) {
        try {
            if (umlCode == null || umlCode.trim().isEmpty()) {
                return "Error: UML代码不能为空";
            }

            System.setProperty("PLANTUML_LIMIT_SIZE", "32768");
            System.setProperty("java.awt.headless", "true");

            String normalizedUmlCode = normalizeUmlCode(umlCode);

            SourceStringReader reader = new SourceStringReader(normalizedUmlCode);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            reader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG));

            byte[] svgBytes = outputStream.toByteArray();
            if (svgBytes.length == 0) {
                return "Error: 生成的SVG内容为空，请检查UML语法是否正确";
            }

            return new String(svgBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Error generating PlantUML: " + e.getMessage();
        }
    }

    private String normalizeUmlCode(String umlCode) {
        umlCode = umlCode.trim();
        if (umlCode.contains("@startuml")) {
            int startIndex = umlCode.indexOf("@startuml");
            int endIndex = umlCode.lastIndexOf("@enduml");
            if (endIndex > startIndex) {
                String startPart = umlCode.substring(startIndex);
                int firstNewLine = startPart.indexOf('\n');
                String content = firstNewLine > 0 ? startPart.substring(firstNewLine + 1) : "";
                if (content.contains("@enduml")) {
                    content = content.substring(0, content.lastIndexOf("@enduml")).trim();
                }
                umlCode = content;
            }
        }

        StringBuilder normalizedCode = new StringBuilder();
        normalizedCode.append("@startuml\n");
        normalizedCode.append("!pragma layout smetana\n");
        normalizedCode.append("skinparam charset UTF-8\n");
        normalizedCode.append("skinparam defaultFontName SimHei\n");
        normalizedCode.append("skinparam defaultFontSize 12\n");
        normalizedCode.append("skinparam dpi 150\n");
        normalizedCode.append("\n");
        normalizedCode.append(umlCode);
        if (!umlCode.endsWith("\n")) {
            normalizedCode.append("\n");
        }
        normalizedCode.append("@enduml");
        return normalizedCode.toString();
    }
}
