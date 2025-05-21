package org.ruoyi.chain.loader;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.ruoyi.chain.split.TextSplitter;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.oss.core.OssClient;
import org.ruoyi.common.oss.entity.UploadResult;
import org.ruoyi.common.oss.factory.OssFactory;
import org.ruoyi.config.properties.PdfProperties;
import org.ruoyi.system.domain.SysOss;
import org.ruoyi.system.mapper.SysOssMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pdf mineru文件加载器
 *
 * @author zpx
 */
@Slf4j
@Component
@AllArgsConstructor
public class PdfMinerUFileLoader implements ResourceLoader {
    private final TextSplitter characterTextSplitter;
    private final PdfProperties properties;
    private final SysOssMapper sysOssMapper;
    // 预编译正则表达式
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[(.*?)]\\((.*?)(\\s*=\\d+)?\\)");


    @Override
    public String getContent(InputStream inputStream) {
        String content = "";
        File tempPdf = null;
        Path outputPath = null;
        try {
            // 创建临时文件
            tempPdf = createTempFile(inputStream);
            //构建输出路径
            outputPath = buildOutputPath();
            // 执行转换命令
            Process process = buildProcess(properties.getTransition().getCondaEnvPath(), tempPdf, outputPath);
            //打印执行日志
            logProcessOutput(process);
            int exitCode = process.waitFor();
            //验证转换结果
            String verifyResult = verifyResult(tempPdf, outputPath, exitCode);

            // 获取生成的.md文件路径
            Path mdFilePath = Paths.get(verifyResult);
            if (Files.exists(mdFilePath)) {
                log.info("找到Markdown文件: " + mdFilePath);
                DocumentParser documentParser = new ApacheTikaDocumentParser();
                Document document = FileSystemDocumentLoader.loadDocument(mdFilePath.toString(), documentParser);
                if (null != document) {
                    content = document.text();
                    // 判断是否md文档
                    String fileType = FilenameUtils.getExtension(mdFilePath.getFileName().toString());
                    if ("md".contains(fileType)) {
                        // 如果是md文件，查找所有图片语法，如果是本地图片，替换成网络图片
                        StringBuffer sb = replaceImageUrl(content, mdFilePath);
                        content = sb.toString();
                    }
                } else {
                    log.warn("无法解析文档内容");
                }
            } else {
                log.warn("未找到预期的 .md 文件");
            }
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (tempPdf != null) {
                try {
                    // 清理临时文件
                    Files.deleteIfExists(tempPdf.toPath());
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempPdf.getAbsolutePath(), e);
                }
            }
            //清理输出目录
            if (outputPath != null) {
                cleanOutputDirectory(outputPath);
            }
        }
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return characterTextSplitter.split(content, kid);
    }

    /**
     * 创建临时PDF文件
     *
     * @param is 输入流
     * @return
     * @throws IOException
     */
    private static File createTempFile(InputStream is) throws IOException {
        File tempFile = File.createTempFile("upload_", ".pdf");
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }


    /**
     * 构建跨平台文件输出路径
     *
     * @return
     * @throws IOException
     */
    private static Path buildOutputPath() throws IOException {
        Path basePath = isWindows() ?
                Paths.get(new File("").getCanonicalPath().substring(0, 3)).resolve("minerUOutPut") :
                Paths.get("/var/minerUOutPut");

        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }
        return basePath;
    }

    /**
     * 判断当前操作系统是否为Windows
     *
     * @return
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 执行命令
     *
     * @param condaEnv   conda环境路径
     * @param inputFile  输入文件
     * @param outputPath 输出路径
     * @return
     * @throws IOException
     */
    private static Process buildProcess(String condaEnv, File inputFile, Path outputPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        String[] command;

        if (isWindows()) {
            command = new String[]{
                    "cmd", "/c",
                    "call", "conda", "activate",
                    condaEnv.replace("\"", ""),
                    "&&", "magic-pdf",
                    "-p", inputFile.getAbsolutePath(),
                    "-o", outputPath.toString()
            };
        } else {
            command = new String[]{
                    "bash", "-c",
                    String.format("source '%s/bin/activate' && magic-pdf -p '%s' -o '%s'",
                            condaEnv,
                            inputFile.getAbsolutePath(),
                            outputPath.toString())
            };
        }

        return pb.command(command)
                .redirectErrorStream(true)
                .start();
    }


    /**
     * 实时日志输出
     *
     * @param process 进程
     */
    private static void logProcessOutput(Process process) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[PROCESS LOG] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 验证转换结果
     *
     * @param inputFile  输入文件
     * @param outputPath 输出路径
     * @param exitCode   退出码
     * @return
     */
    private static String verifyResult(File inputFile, Path outputPath, int exitCode) {
        String baseName = FilenameUtils.removeExtension(inputFile.getName());
        Path expectedMd = outputPath
                .resolve(baseName)
                .resolve("auto")
                .resolve(baseName + ".md");

        if (exitCode == 0 && Files.exists(expectedMd)) {
            log.info("转换成功：{}", expectedMd.toString());
            return expectedMd.toString();
        }
        return String.format("转换失败（退出码%d）| 预期文件：%s", exitCode, expectedMd);
    }

    /**
     * 正则匹配图片语法
     * @param content 文本内容
     * @param basePath 图片路径
     * @return
     */
    private StringBuffer replaceImageUrl(String content, Path basePath)  {
        // 正则表达式匹配md文件中的图片语法 ![alt text](image url)
        Matcher matcher = MD_IMAGE_PATTERN.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String imageUrl = matcher.group(2);
            // 检查是否是本地图片路径
            if (!imageUrl.startsWith("http")) {
                // 获取图片完整路径，上传到Oss中
                Path imagePath = basePath.getParent().resolve(imageUrl);
                if (!Files.exists(imagePath)) {
                    log.error("图片路径不存在: {}", imagePath);
                }
                // 获取原始文件名和后缀
                String originalfileName = imagePath.getFileName().toString();
                // 获取文件后缀
                String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."),
                        originalfileName.length());
                // 读取文件字节流
                try (InputStream inputStream = Files.newInputStream(imagePath)) {
                    // 使用 OssClient 直接上传字节流
                    OssClient storage = OssFactory.instance();
                    UploadResult uploadResult = storage.uploadSuffix(inputStream, suffix, FileUtils.getMimeType(suffix));

                    // 构建 SysOss 对象并保存数据库记录
                    SysOss sysOss = new SysOss();
                    sysOss.setUrl(uploadResult.getUrl());
                    sysOss.setFileSuffix(suffix);
                    sysOss.setFileName(uploadResult.getFilename());
                    sysOss.setOriginalName(originalfileName);
                    sysOss.setService(storage.getConfigKey());

                    // 插入数据库
                    sysOssMapper.insert(sysOss);

                    // OCR 处理 & 替换图片链接
                    String networkImageUrl = uploadResult.getUrl();
                    //⚠️ 注意：确保 URL 是公网可访问的，否则模型无法加载图片。
                    //另一种解决方案：使用base64 但是需要申请apikey , 使用demo会出现token超出长度问题。
                    String imageUrlOCR = imageUrlOCR(networkImageUrl);
                    matcher.appendReplacement(sb, "![" + matcher.group(1) + imageUrlOCR + "](" + networkImageUrl + ")");
            } catch (IOException e) {
                log.error("读取或上传图片失败", e);
                matcher.appendReplacement(sb, matcher.group(0)); // 保留原图语法
            }
            } else {
                //多模态OCR识别图片内容
                String imageUrlOCR = imageUrlOCR(imageUrl);
                matcher.appendReplacement(sb, "![" + matcher.group(1) + imageUrlOCR + "](" + imageUrl + ")");
            }
        }
        matcher.appendTail(sb);
        return sb;
    }

    /**
     * 多模态OCR识别图片内容
     * @param imageUrl 图片URL
     * @return
     */
    private static String imageUrlOCR(String imageUrl) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        UserMessage userMessage = UserMessage.from(
                TextContent.from(
                        "请按以下逻辑处理图片：\n" +
                                "1. 文字检测：识别图中所有可见文字（包括水印/标签），若无文字则跳至步骤3\n" +
                                "2. 文字处理：\n" +
                                "   a. 按出现顺序完整提取文字（非中文立即翻译）\n" +
                                "   b. 用20字内总结核心信息，禁止补充解释\n" +
                                "   c. 描述文字位置(如'顶部居中')、字体特征(颜色/大小)\n" +
                                "3. 视觉描述：\n" +
                                "   a. 客观说明主体对象、场景、色彩搭配与画面氛围\n" +
                                "4. 输出规则：\n" +
                                "   - 最终输出为纯文本，格式：'[文字总结] 视觉描述 关键词：xx,xx'\n" +
                                "   - 关键词从内容中提取3个最具代表性的名词"
                ),
                ImageContent.from(imageUrl)
        );
        ChatResponse chat = model.chat(userMessage);
        AiMessage answer = chat.aiMessage();
        return answer.text();
    }

    /**
     * 清理输出目录
     * @param outputPath 输出目录
     */
    private static void cleanOutputDirectory(Path outputPath) {
        if (Files.exists(outputPath)) {
            try {
                Files.walk(outputPath)
                       // 按逆序删除（子目录先删）
                        .sorted((p1, p2) -> -p1.compareTo(p2))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("清理输出目录失败: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                log.error("遍历输出目录失败", e);
            }
        }
    }
}
