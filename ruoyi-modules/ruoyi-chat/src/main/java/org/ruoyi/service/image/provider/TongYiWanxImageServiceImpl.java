package org.ruoyi.service.image.provider;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.enums.ImageModeType;
import org.ruoyi.service.image.AbstractImageGenerationService;
import org.springframework.stereotype.Service;

/**
 * 万相文生图AI调用
 *
 * @author Zengxb
 * @date 2026/02/14
 */
@Service
@Slf4j
public class TongYiWanxImageServiceImpl extends AbstractImageGenerationService {

    /**
     * 默认图片数量（1张）
     */
    private final static int IMAGE_DEFAULT_SIZE = 1;

    /**
     * 默认图片分辨率（1280*1280）
     */
    private final static String IMAGE_DEFAULT_RESOLUTION = "1280*1280";

    @Override
    protected String doGenerateImage(ChatModelVo chatModelVo, String prompt, String size, Integer seed) {
        // 构建万相模型对象
        var param = (ImageSynthesisParam) buildImageModel(chatModelVo);
        // 设置图片大小和提示词以及随机数种子
        param.setSize(StringUtils.isEmpty(size) ? IMAGE_DEFAULT_RESOLUTION : size);
        param.setPrompt(prompt);
        param.setSeed(seed);
        // 同步调用 AI 大模型，生成图片
        var imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result;
        try {
            log.info("同步调用通义万相文生图接口中....");
            result = imageSynthesis.call(param);
        } catch (ApiException | NoApiKeyException e) {
            log.error("同步调用通义万相文生图接口失败", e);
            return "";
        }
        // 直接提取图片URL
        var output = result.getOutput();
        var results = output.getResults();
        return results.isEmpty() ? "" : results.get(0).get("url");
    }

    @Override
    protected Object buildImageModel(ChatModelVo chatModelVo) {
        return ImageSynthesisParam.builder()
            .prompt("")
            .apiKey(chatModelVo.getApiKey())
            .model(chatModelVo.getModelName())
            .n(IMAGE_DEFAULT_SIZE)
            .build();
    }

    @Override
    public String getProviderName() {
        return ImageModeType.TONGYI_WANX.getCode();
    }
}
