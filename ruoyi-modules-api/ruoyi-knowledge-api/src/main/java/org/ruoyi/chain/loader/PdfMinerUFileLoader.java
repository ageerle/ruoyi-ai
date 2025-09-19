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
import org.ruoyi.chain.split.TextSplitter;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.oss.core.OssClient;
import org.ruoyi.common.oss.entity.UploadResult;
import org.ruoyi.common.oss.factory.OssFactory;
import org.ruoyi.config.properties.PdfProperties;
import org.ruoyi.system.domain.SysOss;
import org.ruoyi.system.mapper.SysOssMapper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
    // OCR图片识别线程池
    private final ThreadPoolExecutor ocrExecutor = new ThreadPoolExecutor(
            // 核心线程数
            5,
            // 最大线程数
            10,
            // 空闲线程存活时间
            60L, TimeUnit.SECONDS,
            // 任务队列容量
            new LinkedBlockingQueue<>(100),
            // 拒绝策略
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

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
                    //判断是否需要进行图片OCR识别
                    if ("md".contains(fileType) && properties.getTransition().isEnableOcr()) {
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
        } catch (Exception e) {
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
                //  Windows C盘用户路径下 minerUOutPut，避免其他盘符权限问题
                Paths.get(System.getProperty("user.home")).resolve("minerUOutPut") :
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
     * 正则匹配图片语法,多线程进行处理
     *
     * @param content  文本内容
     * @param basePath 图片路径
     * @return
     */
    private StringBuffer replaceImageUrl(String content, Path basePath) throws Exception {
        List<ImageMatch> matches = new ArrayList<>();
        Matcher matcher = MD_IMAGE_PATTERN.matcher(content);

        // 收集所有匹配的图片项
        while (matcher.find()) {
            ImageMatch imgMatch = new ImageMatch();
            imgMatch.altText = matcher.group(1);
            imgMatch.imageUrl = matcher.group(2);
            imgMatch.start = matcher.start();
            imgMatch.end = matcher.end();
            matches.add(imgMatch);
        }

        if (matches.isEmpty()) {
            return new StringBuffer(content);
        }

        // 提交任务到线程池
        List<Future<String>> futures = new ArrayList<>();
        for (ImageMatch imgMatch : matches) {
            // 为每个图片项创建独立任务
            Future<String> future = ocrExecutor.submit(() -> processImage(imgMatch, basePath));
            futures.add(future);
        }

        // 按原始顺序拼接结果
        StringBuffer sb = new StringBuffer();
        int previousEnd = 0;

        for (int i = 0; i < matches.size(); i++) {
            ImageMatch imgMatch = matches.get(i);
            // 阻塞等待结果
            String replacement = futures.get(i).get();

            // 插入未匹配的原始文本和处理后的结果
            sb.append(content.substring(previousEnd, imgMatch.start));
            sb.append(replacement);
            previousEnd = imgMatch.end;
        }
        // 添加剩余文本
        sb.append(content.substring(previousEnd));
        return sb;
    }


    /**
     * 图片处理任务
     *
     * @param imgMatch 图片匹配结果
     * @param basePath 本地图片路径
     * @return
     */
    private String processImage(ImageMatch imgMatch, Path basePath) {
        try {
            if (!imgMatch.imageUrl.startsWith("http")) {
                // 处理本地图片
                Path imagePath = basePath.getParent().resolve(imgMatch.imageUrl).normalize();

                if (!Files.exists(imagePath)) {
                    log.error("图片路径不存在: {}", imagePath);
                    return String.format("![%s](%s)", imgMatch.altText, imgMatch.imageUrl);
                }

                // 文件后缀安全提取
                String originalFileName = imagePath.getFileName().toString();
                String suffix = "";
                int lastDotIndex = originalFileName.lastIndexOf(".");
                if (lastDotIndex != -1) {
                    suffix = originalFileName.substring(lastDotIndex);
                }

                // 上传OSS
                try (InputStream inputStream = Files.newInputStream(imagePath)) {
                    OssClient storage = OssFactory.instance();
                    UploadResult uploadResult = storage.uploadSuffix(inputStream, suffix, FileUtils.getMimeType(suffix));

                    // 保存数据库记录
                    SysOss sysOss = new SysOss();
                    sysOss.setUrl(uploadResult.getUrl());
                    sysOss.setFileSuffix(suffix);
                    sysOss.setFileName(uploadResult.getFilename());
                    sysOss.setOriginalName(originalFileName);
                    sysOss.setService(storage.getConfigKey());
                    sysOssMapper.insert(sysOss);

                    // OCR处理
                    String networkUrl = uploadResult.getUrl();
                    //⚠️ 注意：确保 URL 是公网可访问的，否则模型无法加载图片。
                    //另一种解决方案：使用base64 但是需要申请apikey , 使用demo会出现token超出长度问题。
                    String ocrResult = safeImageUrlOCR(networkUrl);
                    return String.format("![%s%s](%s)", imgMatch.altText, ocrResult, networkUrl);
                }
            } else {
                // 处理远程图片
                String ocrResult = safeImageUrlOCR(imgMatch.imageUrl);
                return String.format("![%s%s](%s)", imgMatch.altText, ocrResult, imgMatch.imageUrl);
            }
        } catch (Exception e) {
            log.error("图片处理失败: {}", imgMatch.imageUrl, e);
            return String.format("![%s](%s)", imgMatch.altText, imgMatch.imageUrl);
        }
    }

    /**
     * OCR调用
     *
     * @param imageUrl 图片URL
     * @return
     */
    private String safeImageUrlOCR(String imageUrl) {
        try {
            return imageUrlOCR(imageUrl);
        } catch (Exception e) {
            log.warn("OCR处理失败: {}", imageUrl, e);
            // OCR失败时返回空字符串
            return "";
        }
    }


    /**
     * 多模态OCR识别图片内容
     *
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
                                "   a. 对识别到的文字进行❗核心信息提炼\n" +
                                "   b. ❗禁止直接输出原文内容\n" +
                                "   c. 描述文字位置(如'顶部居中')、字体特征(颜色/大小)\n" +
                                "3. 视觉描述：\n" +
                                "   a. 若无文字则用❗50字内简洁描述主体对象、场景、色彩搭配与画面氛围\n" +
                                "   b. 若有文字则补充说明文字与画面的关系\n" +
                                "4. 输出规则：\n" +
                                "   - 最终输出为纯文本，格式：'[文字总结] 视觉描述 关键词：xx,xx'\n" +
                                "   - 关键词从内容中提取3个最具代表性的名词\n" +
                                "   - 无文字时格式：'[空] 简洁描述 关键词：xx,xx'"
                ),
                ImageContent.from(imageUrl)
        );
        ChatResponse chat = model.chat(userMessage);
        AiMessage answer = chat.aiMessage();
        return answer.text();
    }

    /**
     * 静态内部类保存图片匹配信息
     */
    private static class ImageMatch {
        String altText; // 替换文本
        String imageUrl; // 图片地址
        int start; // 匹配起始位置
        int end; // 匹配结束位置
    }


    /**
     * 清理输出目录
     *
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
