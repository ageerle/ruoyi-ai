package org.ruoyi.service.graph.impl;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.graph.IGraphLLMService;
import org.springframework.stereotype.Service;


/**
 * OpenAI 图谱LLM服务实现
 * 支持 OpenAI 兼容的模型（GPT-3.5, GPT-4, 自定义OpenAI兼容接口等）
 *
 * @author ruoyi
 * @date 2025-10-11
 */
@Slf4j
@Service
public class OpenAIGraphLLMServiceImpl implements IGraphLLMService {

    @Override
    public String extractGraph(String prompt, ChatModelVo chatModel) {
        log.info("OpenAI模型调用: model={}, apiHost={}, 提示词长度={}",
                chatModel.getModelName(), chatModel.getApiHost(), prompt.length());
        try {
            OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(chatModel.getApiHost())
                .apiKey(chatModel.getApiKey())
                .modelName(chatModel.getModelName())
                .build();
            String content = model.chat(prompt);
            String responseText = content != null ? content : "";
            log.info("OpenAI模型响应成功:, 响应长度={}", responseText.length());
            return responseText;
        } catch (Exception e) {
            log.error("OpenAI模型调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCategory() {
        return "openai";  // 对应 ChatModel 表中的 category 字段
    }
}

