package org.ruoyi.graph.service.llm;

import org.ruoyi.domain.vo.ChatModelVo;

/**
 * 图谱LLM服务接口
 * 参考 ruoyi-chat 的 IChatService 设计
 * 支持多种LLM模型（OpenAI、Qwen、Zhipu等）
 *
 * @author ruoyi
 * @date 2025-10-11
 */
public interface IGraphLLMService {

    /**
     * 调用LLM进行图谱实体关系抽取
     *
     * @param prompt    提示词（包含文本和抽取指令）
     * @param chatModel 模型配置
     * @return LLM响应文本
     */
    String extractGraph(String prompt, ChatModelVo chatModel);

    /**
     * 获取此服务支持的模型类别
     * 例如: "openai", "qwen", "zhipu", "ollama"
     *
     * @return 模型类别标识
     */
    String getCategory();
}

