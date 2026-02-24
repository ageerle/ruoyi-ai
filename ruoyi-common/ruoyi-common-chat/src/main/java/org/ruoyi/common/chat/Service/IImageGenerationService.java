package org.ruoyi.common.chat.Service;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.domain.entity.image.ImageContext;

/**
 * 公共文生图接口
 */
public interface IImageGenerationService {

    /**
     * 根据文字生成图片
     */
    String generateImage(@Valid ImageContext imageContext);

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
