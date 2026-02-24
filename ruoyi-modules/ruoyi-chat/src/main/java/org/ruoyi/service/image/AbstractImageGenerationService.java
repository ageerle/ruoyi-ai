package org.ruoyi.service.image;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.Service.IImageGenerationService;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.domain.entity.image.ImageContext;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
public abstract class AbstractImageGenerationService implements IImageGenerationService {

    /**
     * 根据文字生成图片
     * @param imageContext 文生图上下文对象
     * @return 生成的图片URL
     */
    @Override
    public String generateImage(ImageContext imageContext){
        // 获取模型管理视图对象
        ChatModelVo chatModelVo = imageContext.getChatModelVo();
        // 获取提示词
        String prompt = imageContext.getPrompt();
        // 获取图片尺寸大小
        String size = imageContext.getSize();
        // 获取随机数种子
        Integer seed = imageContext.getSeed();
        return doGenerateImage(chatModelVo, prompt, size, seed);
    }

    /**
     * 执行生成图片（钩子方法 - 子类必须实现）
     *
     * @param prompt 提示词
     */
    protected abstract String doGenerateImage(ChatModelVo chatModelVo, String prompt, String size, Integer seed);

    /**
     * 构建具体厂商的 ImageModel（原生SDK 非langchain4j-dashscope版）
     * 子类必须实现此方法，返回对应厂商的模型实例
     */
    protected abstract Object buildImageModel(ChatModelVo chatModelVo);
}
