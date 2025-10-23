package org.ruoyi.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.embedding.model.ModalityType;

import java.util.Set;

/**
 * BaseEmbedModelService 接口，扩展了 EmbeddingModel 接口
 * 该接口定义了嵌入模型服务的基本配置和功能方法
 */
public interface BaseEmbedModelService extends EmbeddingModel {
    /**
     * 根据配置信息配置嵌入模型
     * @param config 包含模型配置信息的 ChatModelVo 对象
     */
    void configure(ChatModelVo config);

    /**
     * 获取当前嵌入模型支持的所有模态类型
     * @return 返回支持的模态类型集合
     */
    Set<ModalityType> getSupportedModalities();

}
