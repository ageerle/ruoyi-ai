package org.ruoyi.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EmbeddingModelFactory {

    private final ApplicationContext applicationContext;

    private final IChatModelService chatModelService;

    // 模型缓存，使用ConcurrentHashMap保证线程安全
    private final Map<Long, BaseEmbedModelService> modelCache = new ConcurrentHashMap<>();

    /**
     * 创建嵌入模型实例
     * 如果模型已存在于缓存中，则直接返回；否则创建新的实例
     *
     * @param embeddingModelId 嵌入模型的唯一标识ID
     * @return BaseEmbedModelService 嵌入模型服务实例
     */
    public BaseEmbedModelService createModel(Long embeddingModelId) {
        return modelCache.computeIfAbsent(embeddingModelId, id -> {
            ChatModelVo modelConfig = chatModelService.queryById(id);
            if (modelConfig == null) {
                throw new IllegalArgumentException("未找到模型配置，ID=" + id);
            }
            return createModelInstance(modelConfig.getProviderName(), modelConfig);
        });
    }

    /**
     * 检查模型是否支持多模态
     *
     * @param embeddingModelId 嵌入模型的唯一标识ID
     * @return boolean 如果模型支持多模态则返回true，否则返回false
     */
    public boolean isMultimodalModel(Long embeddingModelId) {
        return createModel(embeddingModelId) instanceof MultiModalEmbedModelService;
    }

    /**
     * 创建多模态嵌入模型实例
     *
     * @param tenantId 租户ID
     * @return MultiModalEmbedModelService 多模态嵌入模型服务实例
     * @throws IllegalArgumentException 当模型不支持多模态时抛出
     */
    public MultiModalEmbedModelService createMultimodalModel(Long tenantId) {
        BaseEmbedModelService model = createModel(tenantId);
        if (model instanceof MultiModalEmbedModelService) {
            return (MultiModalEmbedModelService) model;
        }
        throw new IllegalArgumentException("该模型不支持多模态");
    }

    /**
     * 刷新模型缓存
     * 根据给定的嵌入模型ID从缓存中移除对应的模型
     *
     * @param embeddingModelId 嵌入模型的唯一标识ID
     */
    public void refreshModel(Long embeddingModelId) {
    // 从模型缓存中移除指定ID的模型
        modelCache.remove(embeddingModelId);
    }

    /**
     * 获取所有支持模型工厂的列表
     *
     * @return List<String> 支持的模型工厂名称列表
     */
    public List<String> getSupportedFactories() {
        return new ArrayList<>(applicationContext.getBeansOfType(BaseEmbedModelService.class)
                .keySet());
    }

    /**
     * 创建具体的模型实例
     * 根据提供的工厂名称和配置信息创建并配置模型实例
     *
     * @param factory 工厂名称，用于标识模型类型
     * @param config 模型配置信息
     * @return BaseEmbedModelService 配置好的模型实例
     * @throws IllegalArgumentException 当无法获取指定的模型实例时抛出
     */
    private BaseEmbedModelService createModelInstance(String factory, ChatModelVo config) {
        try {
            // 从Spring上下文中获取模型实例
            BaseEmbedModelService model = applicationContext.getBean(factory, BaseEmbedModelService.class);
            // 配置模型参数
            model.configure(config);
            log.info("成功创建嵌入模型: factory={}, modelId={}", config.getProviderName(), config.getId());

            return model;
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException("获取不到嵌入模型: " + factory, e);
        }
    }
}