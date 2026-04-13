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
        log.info("初始化重排模型，供应商代码: {}", providerCode);

        // TODO: 在这里通过 switch 或反射具体实例化支持的各种 ScoringModel (例如 CohereScoringModel, DascScope 等)
        // 目前返回 null 代表暂时没有加载特定的重排底座，这不会影响流程，Aggregator 会忽略它返回原样结果

        return null;
    }
}
