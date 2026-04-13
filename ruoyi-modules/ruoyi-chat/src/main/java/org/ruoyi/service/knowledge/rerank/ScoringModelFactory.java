package org.ruoyi.service.knowledge.rerank;

import dev.langchain4j.model.scoring.ScoringModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.stereotype.Component;

/**
 * 重排模型提供商工厂
 * 用于将来无缝拓展硅基流动、百炼等支持重排的模型厂商
 *
 * @author RobustH
 */
@Slf4j
@Component
public class ScoringModelFactory {

    /**
     * 根据后台传递的模型配置创建具体的重排模型
     *
     * @param rerankModelConfig 重排模型的配置 (例如其 providerCode, apiUrl, apiKey 等)
     * @return 标准的 LangChain4j ScoringModel
     */
    public ScoringModel createScoringModel(ChatModelVo rerankModelConfig) {
        if (rerankModelConfig == null) {
            return null;
        }

        String providerCode = rerankModelConfig.getProviderCode();
        log.info("初始化重排模型，供应商代码: {}, 模型名称: {}", providerCode, rerankModelConfig.getModelName());

        try {
            if ("alibailian".equalsIgnoreCase(providerCode)) {
                return DashScopeScoringModel.builder()
                        .apiKey(rerankModelConfig.getApiKey())
                        .modelName(rerankModelConfig.getModelName())
                        .build();
            }

            if ("siliconflow".equalsIgnoreCase(providerCode)) {
                return SiliconFlowScoringModel.builder()
                        .apiKey(rerankModelConfig.getApiKey())
                        .modelName(rerankModelConfig.getModelName())
                        // 如果后台配置了不同的 API Host，可以在此传递，否则使用默认值
                        .baseUrl(rerankModelConfig.getApiHost())
                        .build();
            }
        } catch (Exception e) {
            log.error("创建重排模型失败: {}", e.getMessage(), e);
        }

        return null;
    }
}
