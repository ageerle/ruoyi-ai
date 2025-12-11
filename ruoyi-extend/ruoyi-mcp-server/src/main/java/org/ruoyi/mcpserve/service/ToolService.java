package org.ruoyi.mcpserve.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import okhttp3.*;
import org.ruoyi.mcpserve.config.ToolsProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MCP工具服务类
 * 整合了文件操作、图片搜索、PlantUML生成、网页搜索、终端命令、文档解析等功能
 * 
 * @author ageer,OpenX
 */
@Service
public class ToolService {

    private final ToolsProperties toolsProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ToolService(ToolsProperties toolsProperties) {
        this.toolsProperties = toolsProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 基础工具 ====================

    @Tool(description = "获取一个指定前缀的随机数")
    public String add(@ToolParam(description = "字符前缀") String prefix) {
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        //根据当前时间获取yyMMdd格式的时间字符串
        String format = LocalDate.now().format(formatter);
        //生成随机数
        String replace = prefix + UUID.randomUUID().toString().replace("-", "");
        return format + replace;
    }

    @Tool(description = "获取当前时间")
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    // ==================== 文件操作工具 ====================

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        String fileDir = toolsProperties.getFile().getSaveDir() + "/file";
        String filePath = fileDir + "/" + fileName;
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
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

    // ==================== 图片搜索工具 ====================

    @Tool(description = "Search for images from Pexels")
    public String searchImage(@ToolParam(description = "Image search keywords") String query) {
        try {
            String apiKey = toolsProperties.getPexels().getApiKey();
            String apiUrl = toolsProperties.getPexels().getApiUrl();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", apiKey);

            Map<String, Object> params = new HashMap<>();
            params.put("query", query);

            String response = HttpUtil.createGet(apiUrl)
                    .addHeaders(headers)
                    .form(params)
                    .execute()
                    .body();

            List<String> images = JSONUtil.parseObj(response)
                    .getJSONArray("photos")
                    .stream()
                    .map(photoObj -> (JSONObject) photoObj)
                    .map(photoObj -> photoObj.getJSONObject("src"))
                    .map(photo -> photo.getStr("medium"))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());

            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    // ==================== PlantUML工具 ====================

    @Tool(description = "Generate a PlantUML diagram and return SVG code")
    public String generatePlantUmlSvg(
            @ToolParam(description = "UML diagram source code") String umlCode) {
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

    // ==================== 网页搜索工具 ====================

    @Tool(description = "Search for information from web search engines")
    public String webSearch(
            @ToolParam(description = "Search query text") String query,
            @ToolParam(description = "Max results count") int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String apiKey = toolsProperties.getTavily().getApiKey();
            String baseUrl = toolsProperties.getTavily().getBaseUrl();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(RequestBody.create(MediaType.parse("application/json"), 
                            objectMapper.writeValueAsString(requestBody)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "搜索请求失败: " + response;
                }

                JsonNode jsonNode = objectMapper.readTree(response.body().string()).get("results");
                if (jsonNode != null && !jsonNode.isEmpty()) {
                    jsonNode.forEach(data -> {
                        Map<String, String> processedResult = new HashMap<>();
                        processedResult.put("title", data.get("title").asText());
                        processedResult.put("url", data.get("url").asText());
                        processedResult.put("content", data.get("content").asText());
                        results.add(processedResult);
                    });
                }
            }
        } catch (Exception e) {
            return "搜索时发生错误: " + e.getMessage();
        }
        return JSONUtil.toJsonStr(results);
    }

    // ==================== 终端命令工具 ====================

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(
            @ToolParam(description = "Command to execute in the terminal") String command) {
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

    // ==================== 文档解析工具 ====================

    @Tool(description = "Parse the content of a document from URL")
    public String parseDocumentFromUrl(
            @ToolParam(description = "URL of the document to parse") String fileUrl) {
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

    // ==================== 网页内容加载工具 ====================

    @Tool(description = "Load and extract text content from a web page URL")
    public String loadWebPage(@ToolParam(description = "The URL of the web page to load") String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Error: URL is empty. Please provide a valid URL.";
        }

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "Error: Failed to load web page, status: " + response.code();
                }
                
                String html = response.body().string();
                // 简单的HTML文本提取
                String text = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                        .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                        .replaceAll("<[^>]+>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                
                return text;
            }
        } catch (Exception e) {
            return "Error loading web page: " + e.getMessage();
        }
    }
}
