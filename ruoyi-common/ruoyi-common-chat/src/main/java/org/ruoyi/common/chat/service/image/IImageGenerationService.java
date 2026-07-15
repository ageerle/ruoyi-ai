package org.ruoyi.common.chat.service.image;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * 公共文生图接口
 */
public interface IImageGenerationService {

    /**
     * 根据文字生成图片（同步阻塞，返回 URL）
     */
    String generateImage(@Valid ImageContext imageContext);

    /**
     * 异步启动图片生成（返回 prediction 信息，用于轮询进度）
     * 默认回退到同步模式
     */
    default MediaGenerationResponse startImageGeneration(@Valid ImageContext imageContext) {
        String url = generateImage(imageContext);
        return MediaGenerationResponse.builder()
            .type("image")
            .url(url)
            .status("completed")
            .build();
    }

    /**
     * 上传图生图/图生视频使用的临时媒体文件。
     * 不支持上传的供应商保持默认异常，避免静默回退到错误存储。
     */
    default String uploadMedia(ChatModelVo model, byte[] content, String fileName, String contentType) {
        throw new UnsupportedOperationException("当前图片供应商不支持临时媒体上传");
    }

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
