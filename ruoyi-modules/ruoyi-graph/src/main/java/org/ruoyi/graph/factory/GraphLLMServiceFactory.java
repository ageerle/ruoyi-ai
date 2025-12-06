package org.ruoyi.graph.factory;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.graph.service.llm.IGraphLLMService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图谱LLM服务工厂类
 * 参考 ruoyi-chat 的 ChatServiceFactory 设计
 * 根据模型类别自动选择对应的LLM服务实现
 *
 * @author ruoyi
 * @date 2025-10-11
 */
@Slf4j
@Component
public class GraphLLMServiceFactory implements ApplicationContextAware {

    private final Map<String, IGraphLLMService> llmServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有 IGraphLLMService 的实现
        Map<String, IGraphLLMService> serviceMap = applicationContext.getBeansOfType(IGraphLLMService.class);

        for (IGraphLLMService service : serviceMap.values()) {
            if (service != null) {
                String category = service.getCategory();
                llmServiceMap.put(category, service);
                log.info("注册图谱LLM服务: category={}, service={}",
                        category, service.getClass().getSimpleName());
            }
        }

        log.info("图谱LLM服务工厂初始化完成，共注册 {} 个服务", llmServiceMap.size());
    }

    /**
     * 根据模型类别获取对应的LLM服务实现
     *
     * @param category 模型类别（如: openai, qwen, zhipu）
     * @return LLM服务实现
     * @throws IllegalArgumentException 如果不支持该类别
     */
    public IGraphLLMService getLLMService(String category) {
        IGraphLLMService service = llmServiceMap.get(category);

        if (service == null) {
            log.error("不支持的模型类别: {}, 可用类别: {}", category, llmServiceMap.keySet());
            throw new IllegalArgumentException("不支持的模型类别: " + category +
                    ", 可用类别: " + llmServiceMap.keySet());
        }

        return service;
    }

    /**
     * 获取所有支持的模型类别
     *
     * @return 模型类别集合
     */
    public java.util.Set<String> getSupportedCategories() {
        return llmServiceMap.keySet();
    }

    /**
     * 检查是否支持指定的模型类别
     *
     * @param category 模型类别
     * @return 是否支持
     */
    public boolean isSupported(String category) {
        return llmServiceMap.containsKey(category);
    }
}

