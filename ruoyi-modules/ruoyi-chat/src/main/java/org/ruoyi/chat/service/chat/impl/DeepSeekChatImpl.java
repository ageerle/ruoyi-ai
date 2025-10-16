package org.ruoyi.chat.service.chat.impl;


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.support.ChatServiceHelper;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
/**
 * deepseek
 */
@Service
@Slf4j
public class DeepSeekChatImpl  implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .logRequests(true)
                .logResponses(true)
                .temperature(0.8)
                .build();
        // 发送流式消息
        try {
            chatModel.chat(chatRequest.getPrompt(), new StreamingChatResponseHandler() {
                @SneakyThrows
                @Override
                public void onPartialResponse(String partialResponse) {
                    emitter.send(partialResponse);
                    log.info("收到消息片段: {}", partialResponse);
                    System.out.print(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    emitter.complete();
                    log.info("消息结束，完整消息ID: {}", completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("错误: " + error.getMessage());
                    ChatServiceHelper.onStreamError(emitter, error.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("deepseek请求失败：{}", e.getMessage());
            // 同步异常直接通知失败
            ChatServiceHelper.onStreamError(emitter, e.getMessage());
        }

        return emitter;
    }

    /**
     * 工作流场景：支持 langchain4j handler
     */
    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        log.info("workflow chat, model: {}", request.getModel());

        ChatModelVo chatModelVo = chatModelService.selectModelByName(request.getModel());

        StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .logRequests(true)
                .logResponses(true)
                .temperature(0.8)
                .build();

        try {
            // 将 ruoyi-ai 的 ChatRequest 转换为 langchain4j 的格式
            dev.langchain4j.model.chat.request.ChatRequest langchainRequest = convertToLangchainRequest(request);
            chatModel.chat(langchainRequest, handler);
        } catch (Exception e) {
            log.error("workflow deepseek请求失败：{}", e.getMessage(), e);
            throw new RuntimeException("DeepSeek workflow chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * 转换请求格式
     */
    private dev.langchain4j.model.chat.request.ChatRequest convertToLangchainRequest(ChatRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        for (org.ruoyi.common.chat.entity.chat.Message msg : request.getMessages()) {
            // 简单转换，您可以根据实际需求调整
            if ("user".equals(msg.getRole())) {
                messages.add(UserMessage.from(msg.getContent().toString()));
            } else if ("system".equals(msg.getRole())) {
                messages.add(SystemMessage.from(msg.getContent().toString()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(AiMessage.from(msg.getContent().toString()));
            }
        }
        return dev.langchain4j.model.chat.request.ChatRequest.builder().messages(messages).build();
    }

    @Override
    public String getCategory() {
        return ChatModeType.DEEPSEEK.getCode();
    }
}
