package org.ruoyi.graph.service.llm.impl;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.graph.service.llm.IGraphLLMService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

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
            // 创建 OpenAiStreamClient
            OpenAiStreamClient client = ChatConfig.createOpenAiStreamClient(
                    chatModel.getApiHost(),
                    chatModel.getApiKey()
            );

            // 构建消息
            List<Message> messages = Collections.singletonList(
                    Message.builder()
                            .role(Message.Role.USER)
                            .content(prompt)
                            .build()
            );

            // 构建请求（非流式，同步调用）
            ChatCompletion completion = ChatCompletion.builder()
                    .messages(messages)
                    .model(chatModel.getModelName())
                    .stream(false)  // 同步调用
                    .build();

            // 同步调用 LLM
            long startTime = System.currentTimeMillis();
            ChatCompletionResponse response = client.chatCompletion(completion);
            long duration = System.currentTimeMillis() - startTime;

            // 提取响应文本
            Object content = response.getChoices().get(0).getMessage().getContent();
            String responseText = content != null ? content.toString() : "";

            log.info("OpenAI模型响应成功: 耗时={}ms, 响应长度={}", duration, responseText.length());

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

