package org.ruoyi.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重排序模型工厂服务类
 * 参考设计模式：EmbeddingModelFactory
 * 负责创建和管理重排序模型实例
 *
 * @author yang
 * @date 2026-04-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RerankModelFactory {

    private final ApplicationContext applicationContext;

    private final IChatModelService chatModelService;

    /**
     * 模型缓存，使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, RerankModelService> modelCache = new ConcurrentHashMap<>();

    /**
     * 创建重排序模型实例
     * 如果模型已存在于缓存中，则直接返回；否则创建新的实例
     *
     * @param rerankModelName 重排序模型名称
     */
    public RerankModelService createModel(String rerankModelName) {
        return modelCache.computeIfAbsent(rerankModelName, name -> {
            ChatModelVo modelConfig = chatModelService.selectModelByName(rerankModelName);

            if (modelConfig == null) {
                throw new IllegalArgumentException("未找到重排序模型配置，name=" + name);
            }
            return createModelInstance(modelConfig.getProviderCode(), modelConfig);
        });
    }

    /**
     * 刷新模型缓存
     * 根据给定的模型ID从缓存中移除对应的模型
     *
     * @param modelId 模型的唯一标识ID
     */
    public void refreshModel(Long modelId) {
        modelCache.remove(modelId);
    }

    /**
     * 获取所有支持模型工厂的列表
     *
     * @return 支持的模型工厂名称列表
     */
    public List<String> getSupportedFactories() {
        return new ArrayList<>(applicationContext.getBeansOfType(RerankModelService.class)
                .keySet());
    }

    /**
     * 创建具体的模型实例
     * 根据提供的工厂名称和配置信息创建并配置模型实例
     *
     * @param factory 工厂名称，用于标识模型类型（providerCode）
     * @param config  模型配置信息
     * @return RerankModelService 配置好的模型实例
     * @throws IllegalArgumentException 当无法获取指定的模型实例时抛出
     */
    private RerankModelService createModelInstance(String factory, ChatModelVo config) {
        try {
            // 优先尝试使用 providerCode + "Rerank" 作为 Bean 名称
            // 例如：zhipu -> zhipuRerank，jina -> jinaRerank
            String rerankBeanName = factory + "Rerank";
            RerankModelService model = applicationContext.getBean(rerankBeanName, RerankModelService.class);
            model.configure(config);
            log.info("成功创建重排序模型: factory={}, modelName={}", rerankBeanName, config.getModelName());
            return model;
        } catch (NoSuchBeanDefinitionException e) {
            // 如果找不到，尝试使用原始的 providerCode
            try {
                RerankModelService model = applicationContext.getBean(factory, RerankModelService.class);
                model.configure(config);
                log.info("成功创建重排序模型: factory={}, modelName={}", factory, config.getModelName());
                return model;
            } catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalArgumentException("获取不到重排序模型: " + factory + " 或 " + factory + "Rerank", ex);
            }
        }
    }
}
