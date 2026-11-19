package org.ruoyi.chat.service.chat;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.ruoyi.chat.mapper.ChatMessageImageStore;
import org.ruoyi.chat.mapper.ChatMessageImageStore.StoredImage;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.enums.ModelType;
import org.ruoyi.chat.service.media.MediaOcrService;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatImageService {

    static final String VISION_MODELS_KEY = "chat.image.visionModels";
    private static final String VISION_MODELS_NAME = "聊天默认视觉模型";
    private static final Set<String> NATIVE_IMAGE_PROVIDERS = Set.of(
        ChatModeType.OPEN_AI.getCode(), ChatModeType.ARK.getCode(), ChatModeType.CUSTOM_API.getCode(),
        ChatModeType.CUSTOM_ANTHROPIC.getCode(), ChatModeType.ZHI_PU.getCode(), ChatModeType.QIAN_WEN.getCode(),
        ChatModeType.OLLAMA.getCode(), ChatModeType.DEEP_SEEK.getCode(), ChatModeType.PPIO.getCode(),
        ChatModeType.ATLAS.getCode(), ChatModeType.MINIMAX.getCode(), ChatModeType.XIAOMI.getCode()
    );
    private static final Pattern OCR_IMAGE_PLACEHOLDER = Pattern.compile(
        "!\\[[^\\]\\r\\n]*]\\(\\s*page\\s*=\\s*\\d+\\s*,\\s*bbox\\s*=\\s*[^)\\r\\n]*\\)",
        Pattern.CASE_INSENSITIVE);

    private final ChatMessageImageStore images;
    private final ConfigService config;
    private final IChatModelService models;

    // Tasks refer to stored image IDs, so queued work keeps no image buffers in memory.
    private final ExecutorService executor = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16), new BasicThreadFactory.Builder().daemon(true).namingPattern("chat-image-%d").build());

    public StoredImage decode(ChatRequest.Image image) {
        String base64 = image.getDataBase64();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("请添加 JPG 或 PNG 图片");
        }
        if (base64.length() > ChatRequest.Image.MAX_BASE64_LENGTH) {
            throw new IllegalArgumentException("图片不能超过 10 MB");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无法读取图片，请重新添加");
        }
        return new StoredImage(MediaOcrService.validateImage(bytes), bytes);
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public String defaultVisionModelName() {
        return Arrays.stream(StringUtils.defaultString(config.getConfigValue(VISION_MODELS_KEY)).split(","))
            .map(String::strip)
            .filter(name -> !name.isEmpty())
            .findFirst()
            .orElse(null);
    }

    public VisionDefault visionDefault() {
        ChatModelBo query = new ChatModelBo();
        query.setCategory(ModelType.CHAT.getKey());
        List<VisionOption> options = models.queryAvailableList(query).stream()
            .filter(ChatImageService::supportsNativeImageInput)
            .map(ChatModelVo::getModelName)
            .distinct()
            .map(this::resolveDefaultVisionModel)
            .filter(Objects::nonNull)
            .map(model -> new VisionOption(model.getModelName(), model.getModelDescribe(), model.getProviderCode()))
            .toList();
        return new VisionDefault(defaultVisionModelName(), options);
    }

    public boolean updateVisionDefault(String modelName) {
        ChatModelVo model = resolveDefaultVisionModel(modelName);
        if (model == null) {
            return false;
        }
        config.upsertConfigValue(VISION_MODELS_KEY, VISION_MODELS_NAME, model.getModelName());
        return true;
    }

    private ChatModelVo resolveDefaultVisionModel(String modelName) {
        if (StringUtils.isBlank(modelName) || modelName.indexOf(',') >= 0 || !modelName.equals(modelName.strip())) {
            return null;
        }
        try {
            ChatModelVo model = models.selectModelByName(modelName);
            return supportsNativeImageInput(model) && modelName.equals(model.getModelName()) ? model : null;
        } catch (ServiceException e) {
            // selectModelByName rejects a disabled provider.
            return null;
        }
    }

    public static boolean supportsNativeImageInput(ChatModelVo model) {
        return model != null && Boolean.TRUE.equals(model.getSupportsVision())
            && ModelType.CHAT.getKey().equals(model.getCategory()) && model.getProviderCode() != null
            && NATIVE_IMAGE_PROVIDERS.contains(model.getProviderCode());
    }

    public record VisionDefault(String modelName, List<VisionOption> options) {}

    public record VisionOption(String modelName, String modelDescribe, String providerCode) {}

    public StoredImage load(Long messageId) {
        return images.find(messageId).orElseThrow(NoSuchElementException::new);
    }

    public static String imagePrompt(String originalContent) {
        return StringUtils.defaultIfBlank(originalContent, "请描述这张图片。");
    }

    public static String modelInput(String originalContent, String recognizedText) {
        // Older messages may contain GLM's layout markers alongside the recognized text.
        String text = readableOcrText(recognizedText);
        if (text.isEmpty()) {
            return originalContent;
        }
        String prompt = StringUtils.defaultIfBlank(originalContent, "请说明这张图片里写了什么。");
        return prompt + "\n\n以下是从图片里提取的文字，请结合上面的问题回答：\n" + text;
    }

    private static String readableOcrText(String text) {
        String result = OCR_IMAGE_PLACEHOLDER.matcher(StringUtils.defaultString(text)).replaceAll("").strip();
        return result.codePoints().anyMatch(Character::isLetterOrDigit) ? result : "";
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
