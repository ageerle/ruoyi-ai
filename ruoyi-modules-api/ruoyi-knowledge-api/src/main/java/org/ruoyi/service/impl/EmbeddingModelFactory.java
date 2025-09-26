package org.ruoyi.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @Author: Robust_H
 * @Date: 2025-09-25-下午4:52
 * @Description:  嵌入模型抽象服务类，统一管理嵌入模型的动态配置与构建
 */
@Slf4j
@Component
public class EmbeddingModelFactory{

    private final IChatModelService iChatModelService = SpringUtils.getBean(IChatModelService.class);

    public EmbeddingModel getEmbeddingModel(String modelName) {
        Assert.isTrue(StrUtil.isNotBlank(modelName), "模型名称不能为空");

        // 从服务获取模型配置
        ChatModelVo model = iChatModelService.selectModelByName(modelName);
        Assert.notNull(model, "未找到模型配置: {}");
        Assert.isTrue(StrUtil.isNotBlank(model.getModelName()), "模型配置错误：模型名称为空");
        Assert.isTrue(StrUtil.isNotBlank(model.getApiHost()), "模型配置错误：API Host为空");

        // 3. 创建模型实例
        EmbeddingModel embeddingModel = createEmbeddingModel(model);

        log.info("嵌入模型 [{}] 创建成功，类型: {}", modelName, model.getModelName());

        return embeddingModel;
    }

    /**
     * 根据模型类型动态创建EmbeddingModel
     */
    private EmbeddingModel createEmbeddingModel(ChatModelVo modelConfig) {
        String lowerModelName = modelConfig.getModelName().toLowerCase();

        // Ollama系列模型
        if (lowerModelName.contains("ollama") || lowerModelName.contains("llama") || lowerModelName.startsWith("quentinz/")) {
            return buildOllamaModel(modelConfig);
        }

        // OpenAI或类OpenAI模型（BGE等）
        if (lowerModelName.contains("openai") || lowerModelName.contains("bge") || lowerModelName.startsWith("baai/")) {
            return buildOpenAiModel(modelConfig);
        }

        throw new IllegalArgumentException("不支持的嵌入模型类型: " + modelConfig.getModelName());
    }

    // 构建Ollama模型
    private EmbeddingModel buildOllamaModel(ChatModelVo model) {
        log.debug("构建 Ollama 嵌入模型, Host: {}, Model: {}", model.getApiHost(), model.getModelName());
        return OllamaEmbeddingModel.builder()
                .baseUrl(model.getApiHost())
                .modelName(model.getModelName())
                .build();
    }

    // 构建OpenAi模型
    private EmbeddingModel buildOpenAiModel(ChatModelVo model) {
        log.debug("构建 OpenAI 嵌入模型, Host: {}, Model: {}", model.getApiHost(), model.getModelName());
        return OpenAiEmbeddingModel.builder()
                .apiKey(model.getApiKey())
                .baseUrl(model.getApiHost())
                .modelName(model.getModelName())
                .build();
    }

}