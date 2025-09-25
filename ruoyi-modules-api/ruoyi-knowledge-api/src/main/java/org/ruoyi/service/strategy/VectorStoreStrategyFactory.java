package org.ruoyi.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量库策略工厂
 * 根据配置动态选择向量库实现
 *
 * @author ageer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreStrategyFactory implements ApplicationContextAware {

    private final ConfigService configService;
    private final Map<String, VectorStoreStrategy> strategyMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        initStrategies();
    }

    /**
     * 初始化所有策略实现
     */
    private void initStrategies() {
        Map<String, VectorStoreStrategy> strategies = applicationContext.getBeansOfType(VectorStoreStrategy.class);
        for (VectorStoreStrategy strategy : strategies.values()) {
            if (strategy instanceof AbstractVectorStoreStrategy) {
                AbstractVectorStoreStrategy abstractStrategy = (AbstractVectorStoreStrategy) strategy;
                strategyMap.put(abstractStrategy.getVectorStoreType(), strategy);
                log.info("注册向量库策略: {}", abstractStrategy.getVectorStoreType());
            }
        }
    }

    /**
     * 获取当前配置的向量库策略
     */
    public VectorStoreStrategy getStrategy() {
        String vectorStoreType = configService.getConfigValue("vector", "store_type");
        if (vectorStoreType == null || vectorStoreType.isEmpty()) {
            vectorStoreType = "weaviate"; // 默认使用weaviate
        }
        
        VectorStoreStrategy strategy = strategyMap.get(vectorStoreType);
        if (strategy == null) {
            log.warn("未找到向量库策略: {}, 使用默认策略: weaviate", vectorStoreType);
            strategy = strategyMap.get("weaviate");
        }
        
        if (strategy == null) {
            throw new RuntimeException("未找到可用的向量库策略实现");
        }
        
        return strategy;
    }

    /**
     * 根据类型获取特定的向量库策略
     */
    public VectorStoreStrategy getStrategy(String vectorStoreType) {
        VectorStoreStrategy strategy = strategyMap.get(vectorStoreType);
        if (strategy == null) {
            throw new RuntimeException("未找到向量库策略: " + vectorStoreType);
        }
        return strategy;
    }

    /**
     * 获取所有可用的向量库类型
     */
    public String[] getAvailableTypes() {
        return strategyMap.keySet().toArray(new String[0]);
    }
}