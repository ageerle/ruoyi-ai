package org.ruoyi.service.image.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.image.AbstractImageGenerationService;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("atlasImage")
public class AtlasImageGenerationServiceImpl extends AbstractImageGenerationService {
    private final AtlasPredictionService predictions;
    public AtlasImageGenerationServiceImpl(AtlasPredictionService predictions) { this.predictions = predictions; }

    /** 比例 → 像素映射（AtlasCloud 要求 width*height，min 768 max 1360） */
    private static final Map<String, String> SIZE_MAP = Map.of(
        "1:1", "1024*1024",
        "3:2", "1152*768",
        "4:3", "1152*864",
        "16:9", "1360*768",
        "9:16", "768*1360"
    );
    private static final Map<String, String> GPT_IMAGE_SIZE_MAP = Map.of(
        "1:1", "1024x1024",
        "3:2", "1536x1024",
        "4:3", "1536x1024",
        "16:9", "1536x1024",
        "9:16", "1024x1536"
    );
    private static final String SEEDREAM_47_TEXT_TO_IMAGE = "bytedance/seedream-v4.7/text-to-image";
    /** Atlas's documented 2K presets; the exact T2I variant produces one image, without references. */
    private static final Map<String, String> SEEDREAM_47_SIZE_MAP = Map.of(
        "1:1", "2048*2048", "4:3", "2304*1728", "3:4", "1728*2304",
        "16:9", "2848*1600", "9:16", "1600*2848", "3:2", "2496*1664",
        "2:3", "1664*2496", "21:9", "3136*1344"
    );

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    public String generateImage(ImageContext context) {
        if (!isSeedream47TextToImage(context.getChatModelVo().getModelName())) return super.generateImage(context);
        // The official 4.7 schema is asynchronous. A synchronous caller submits exactly once,
        // then queries that prediction; it does not rely on an undocumented sync-mode flag.
        var started = startImageGeneration(context);
        return awaitSeedream47Image(context.getChatModelVo(), started.getId());
    }

    String awaitSeedream47Image(ChatModelVo model, String predictionId) {
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5);
        try {
            while (System.nanoTime() < deadline) {
                var result = predictions.retrieve(model, predictionId);
                if (result != null && ("completed".equals(result.getStatus()) || "succeeded".equals(result.getStatus())) && StrUtil.isNotBlank(result.getUrl())) return result.getUrl();
                if (result != null && "failed".equals(result.getStatus())) throw new IllegalStateException("Seedream 4.7图片任务失败，predictionId=" + predictionId);
                Thread.sleep(1500);
            }
            throw new IllegalStateException("Seedream 4.7图片任务尚未完成，请查询原任务而勿重新提交：predictionId=" + predictionId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("图片等待被中断，请查询原任务：predictionId=" + predictionId, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Seedream 4.7原任务查询未完成，predictionId=" + predictionId + "；" + e.getMessage(), e);
        }
    }

    @Override
    protected String doGenerateImage(ChatModelVo chatModelVo, String prompt, String size, Integer seed, String image) {
        if (isSeedream47TextToImage(chatModelVo.getModelName())) return generateImage(ImageContext.builder()
            .chatModelVo(chatModelVo).prompt(prompt).size(size).seed(seed).image(image).build());
        ObjectNode payload = buildPayload(chatModelVo, prompt, size, seed, image);
        payload.put("enable_sync_mode", true);

        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(chatModelVo.getApiHost(), "/model/generateImage"))
            .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString(), AtlasMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                log.error("AtlasCloud 图片生成 HTTP {}: {}", response.code(), responseText);
                throw new IllegalArgumentException("Atlas Cloud图片生成失败: " + response.code() + " - " + responseText);
            }
            log.info("AtlasCloud 图片生成完成，响应长度: {}", responseText.length());
            String url = extractImageUrl(responseText);
            log.info("AtlasCloud 提取图片 URL: {}", url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud图片生成失败: " + e.getMessage(), e);
        }
    }

    /** 从 AtlasCloud 同步模式响应中提取图片 URL */
    private String extractImageUrl(String responseText) {
        if (StrUtil.isBlank(responseText)) return "";
        String preview = responseText.length() > 500 ? responseText.substring(0, 500) + "..." : responseText;
        log.info("AtlasCloud 原始响应(前500字符): {}", preview);
        try {
            JsonNode root = AtlasMediaSupport.OBJECT_MAPPER.readTree(responseText);
            JsonNode data = root.path("data");
            String status = AtlasMediaSupport.text(data, "status");
            log.info("AtlasCloud 任务状态: {}, 任务ID: {}", status, AtlasMediaSupport.text(data, "id"));
            JsonNode outputs = data.path("outputs");
            if (outputs.isArray() && !outputs.isEmpty()) {
                String url = outputs.get(0).asText();
                log.info("AtlasCloud outputs[0]: {}", url);
                if (StrUtil.isNotBlank(url)) return url;
            }
            if ("processing".equals(status) || "pending".equals(status)) {
                log.warn("AtlasCloud 图片为异步任务，请轮询 prediction/{}", AtlasMediaSupport.text(data, "id"));
            }
        } catch (IOException e) {
            log.warn("解析 AtlasCloud 图片响应失败: {}", e.getMessage());
        }
        return "";
    }

    static ObjectNode buildPayload(ChatModelVo chatModelVo, String prompt, String size, Integer seed, String image) {
        String modelName = chatModelVo.getModelName();
        boolean gptImageModel = isGptImageModel(modelName);
        boolean seedream47T2i = isSeedream47TextToImage(modelName);
        if (seedream47T2i && StrUtil.isNotBlank(image)) {
            throw seedream47ReferenceError();
        }
        ObjectNode payload = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", modelName);
        payload.put("prompt", prompt);
        if (StrUtil.isNotBlank(size)) {
            payload.put("size", resolveSize(modelName, size));
        }
        if (seed != null && !gptImageModel && !seedream47T2i) {
            payload.put("seed", seed);
        }
        if (seedream47T2i) {
            payload.put("prompt_expansion_mode", "standard");
            payload.put("enable_base64_output", false);
        }
        if (gptImageModel) {
            payload.put("enable_base64_output", false);
            payload.put("output_format", "jpeg");
            payload.put("quality", "high");
            payload.put("moderation", "low");
            if (modelName.startsWith("openai/gpt-image-2.5-")) {
                payload.remove("enable_base64_output");
                payload.remove("moderation");
                payload.put("background", "opaque");
                payload.put("n", 1);
            }
        }
        if (StrUtil.isNotBlank(image)) {
            if (gptImageModel) {
                payload.putArray("images").add(image);
            } else {
                // Atlas Cloud 图生图接口使用 image_url 接收 uploadMedia 返回的临时 URL。
                payload.put("image_url", image);
            }
        }
        return payload;
    }

    static boolean isGptImageModel(String modelName) {
        return StrUtil.isNotBlank(modelName) && modelName.startsWith("openai/gpt-image-");
    }

    static boolean isSeedream47TextToImage(String modelName) {
        return SEEDREAM_47_TEXT_TO_IMAGE.equals(modelName);
    }

    private static IllegalArgumentException seedream47ReferenceError() {
        return new IllegalArgumentException("Seedream 4.7 文生图型号不接收参考图；请使用无参考图的新资产候选流程。身份绑定或多参考首帧需要另行选择支持参考图的编辑型号，不会自动切换模型。");
    }

    /** 根据 AtlasCloud 模型将比例转换为该模型支持的像素格式。 */
    static String resolveSize(String modelName, String size) {
        if (StrUtil.isBlank(size)) return null;
        String normalizedSize = size.trim();
        if (isSeedream47TextToImage(modelName)) {
            String mapped = SEEDREAM_47_SIZE_MAP.get(normalizedSize);
            return mapped != null ? mapped : resolveSeedream47PixelSize(normalizedSize);
        }
        if (isGptImageModel(modelName)) {
            if (modelName.startsWith("openai/gpt-image-2/") || modelName.startsWith("openai/gpt-image-2.5-")) {
                if ("16:9".equals(normalizedSize)) return "1536x864";
                if ("9:16".equals(normalizedSize)) return "864x1536";
                if ("4:3".equals(normalizedSize)) return "1536x1152";
                if ("3:4".equals(normalizedSize)) return "1152x1536";
            }
            String mappedSize = GPT_IMAGE_SIZE_MAP.get(normalizedSize);
            if (mappedSize != null) return mappedSize;
            return resolveGptImagePixelSize(normalizedSize);
        }
        if (normalizedSize.contains("*") || normalizedSize.contains("x")) return normalizedSize;
        return SIZE_MAP.getOrDefault(normalizedSize, "1024*1024");
    }

    private static String resolveSeedream47PixelSize(String size) {
        String[] dimensions = size.toLowerCase(java.util.Locale.ROOT).replace('x', '*').split("\\*", -1);
        try {
            if (dimensions.length != 2) throw new NumberFormatException();
            int width = Integer.parseInt(dimensions[0].trim()), height = Integer.parseInt(dimensions[1].trim());
            long pixels = (long) width * height;
            if (width <= 0 || height <= 0 || pixels < 921600L || pixels > 16777216L
                || (long) width > (long) height * 16L || (long) height > (long) width * 16L) {
                throw new NumberFormatException();
            }
            return width + "*" + height;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Seedream 4.7 尺寸须为有效比例或WIDTH*HEIGHT：总像素921600–16777216，长宽比例在1:16至16:1之间");
        }
    }

    private static String resolveGptImagePixelSize(String size) {
        String[] dimensions = size.toLowerCase().replace('*', 'x').split("x");
        if (dimensions.length != 2) return "1024x1024";
        try {
            int width = Integer.parseInt(dimensions[0].trim());
            int height = Integer.parseInt(dimensions[1].trim());
            if (width == height) return "1024x1024";
            return width > height ? "1536x1024" : "1024x1536";
        } catch (NumberFormatException e) {
            return "1024x1024";
        }
    }

    @Override
    public MediaGenerationResponse startImageGeneration(ImageContext imageContext) {
        ChatModelVo chatModelVo = imageContext.getChatModelVo();
        String prompt = imageContext.getPrompt();
        String size = imageContext.getSize();
        Integer seed = imageContext.getSeed();
        String image = imageContext.getImage();

        ObjectNode payload = buildPayload(chatModelVo, prompt, size, seed, image);
        if (!isSeedream47TextToImage(chatModelVo.getModelName())) payload.put("enable_sync_mode", false);
        applyReferenceImages(payload, imageContext);

        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(chatModelVo.getApiHost(), "/model/generateImage"))
            .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString(), AtlasMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                log.error("AtlasCloud 异步图片生成 HTTP {}: {}", response.code(), responseText);
                throw new IllegalArgumentException("Atlas Cloud图片生成失败: " + response.code() + " - " + responseText);
            }
            log.info("AtlasCloud 异步图片生成响应: {}", responseText.length() > 300 ? responseText.substring(0, 300) + "..." : responseText);
            JsonNode root = AtlasMediaSupport.OBJECT_MAPPER.readTree(responseText);
            String predictionId = AtlasMediaSupport.text(root.path("data"), "id");
            if (StrUtil.isBlank(predictionId)) predictionId = AtlasMediaSupport.text(root, "id");
            if (StrUtil.isBlank(predictionId)) {
                throw new IllegalStateException("Atlas Cloud图片任务启动成功但响应中没有任务ID: "
                    + (responseText.length() > 500 ? responseText.substring(0, 500) : responseText));
            }
            log.info("AtlasCloud 异步任务 predictionId: {}", predictionId);
            return MediaGenerationResponse.builder()
                .type("image")
                .id(predictionId)
                .status("pending")
                .rawResponse(responseText)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud图片生成失败: " + e.getMessage(), e);
        }
    }

    static void applyReferenceImages(ObjectNode payload, ImageContext context) {
        var references = context.getReferenceImages();
        if (references == null || references.isEmpty()) return;
        if (isSeedream47TextToImage(context.getChatModelVo().getModelName())) throw seedream47ReferenceError();
        int limit = context.getChatModelVo().getModelName().startsWith("openai/gpt-image-2.5-") ? 16 : 10;
        if (references.size() > limit) throw new IllegalArgumentException("当前模型最多绑定" + limit + "张参考图，请减少不必要道具");
        if (!isGptImageModel(context.getChatModelVo().getModelName())) {
            throw new IllegalArgumentException("当前多参考图资产流程需要支持 images 的 GPT Image 模型");
        }
        var images = payload.putArray("images");
        references.stream().filter(StrUtil::isNotBlank).distinct().forEach(images::add);
    }

    @Override
    public String uploadMedia(ChatModelVo model, byte[] content, String fileName, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        MediaType mediaType = MediaType.parse(StrUtil.blankToDefault(contentType, "application/octet-stream"));
        RequestBody fileBody = RequestBody.create(content, mediaType);
        MultipartBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", StrUtil.blankToDefault(fileName, "reference-image"), fileBody)
            .build();
        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(model.getApiHost(), "/model/uploadMedia"))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .post(requestBody)
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Atlas Cloud文件上传失败: " + response.code() + " - " + responseText);
            }
            JsonNode root = AtlasMediaSupport.OBJECT_MAPPER.readTree(responseText);
            String url = findUploadedMediaUrl(root);
            if (StrUtil.isBlank(url)) {
                String preview = responseText.length() > 800 ? responseText.substring(0, 800) + "..." : responseText;
                log.error("Atlas Cloud上传响应中未识别到媒体URL: {}", preview);
                throw new IllegalStateException("Atlas Cloud文件上传成功但响应中没有可识别的媒体URL: " + preview);
            }
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud文件上传失败: " + e.getMessage(), e);
        }
    }

    /** 兼容 Atlas 不同网关版本的上传响应字段。 */
    private static String findUploadedMediaUrl(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.isTextual()) {
            String value = node.asText();
            return isHttpUrl(value) ? value : null;
        }
        if (node.isObject()) {
            for (String key : new String[]{"url", "file_url", "fileUrl", "image_url", "imageUrl", "media_url", "mediaUrl", "download_url", "downloadUrl"}) {
                String value = node.path(key).asText(null);
                if (isHttpUrl(value)) return value;
            }
            for (String key : new String[]{"data", "output", "outputs", "urls", "result", "file", "media"}) {
                String value = findUploadedMediaUrl(node.path(key));
                if (StrUtil.isNotBlank(value)) return value;
            }
            java.util.Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                String value = findUploadedMediaUrl(children.next());
                if (StrUtil.isNotBlank(value)) return value;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findUploadedMediaUrl(child);
                if (StrUtil.isNotBlank(value)) return value;
            }
        }
        return null;
    }

    private static boolean isHttpUrl(String value) {
        return StrUtil.isNotBlank(value)
            && (value.startsWith("https://") || value.startsWith("http://"));
    }

    @Override
    protected Object buildImageModel(ChatModelVo chatModelVo) {
        return chatModelVo;
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }
}
