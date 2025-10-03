package org.ruoyi.embedding;

import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 嵌入模型工厂服务类
 * 负责创建和管理各种嵌入模型实例
 */
@Service
@RequiredArgsConstructor
public class EmbeddingModelFactory {
    private final ApplicationContext applicationContext;

    private final IChatModelService iChatModelService;

    private final Map<String, BaseEmbedModelService> modelCache = new ConcurrentHashMap<>();

    public BaseEmbedModelService createModel(Long embeddingModelId) {
        ChatModelVo chatModelVo = iChatModelService.queryById(embeddingModelId);

        return createModelInstance(chatModelVo.getProviderName(), chatModelVo);
    }

    private BaseEmbedModelService createModelInstance(String factory, ChatModelVo config) {
        try {
            BaseEmbedModelService model = applicationContext.getBean(factory, BaseEmbedModelService.class);
            // TODO 缓存设置
            model.configure(config);

            return model;
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException("获取不到嵌入模型: " + factory, e);
        }
    }

    // 检查模型是否支持多模态
    public boolean isMultimodalModel(Long tenantId) {
        BaseEmbedModelService model = createModel(tenantId);
        return model instanceof MultiModalEmbedModelService;
    }

    // 获取多模态模型（如果支持）
    public MultiModalEmbedModelService createMultimodalModel(Long tenantId) {
        BaseEmbedModelService model = createModel(tenantId);
        if (model instanceof MultiModalEmbedModelService) {
            return (MultiModalEmbedModelService) model;
        }
        throw new IllegalArgumentException("该模型不支持多模态");
    }

    public void refreshModel(String tenantId, String factory) {
        String cacheKey = tenantId + ":" + factory;
        modelCache.remove(cacheKey);
    }

    public List<String> getSupportedFactories() {
        return new ArrayList<>(applicationContext.getBeansOfType(BaseEmbedModelService.class)
                .keySet());
    }
}