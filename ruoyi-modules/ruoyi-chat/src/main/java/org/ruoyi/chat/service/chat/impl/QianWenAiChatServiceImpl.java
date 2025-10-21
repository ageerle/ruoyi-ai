package org.ruoyi.chat.service.chat.impl;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
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


/**
 * 阿里通义千问
 */
@Service
@Slf4j
public class QianWenAiChatServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;


    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        StreamingChatModel model = QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();


        // 发送流式消息
        try {
            model.chat(chatRequest.getPrompt(), new StreamingChatResponseHandler() {
                @SneakyThrows
                @Override
                public void onPartialResponse(String partialResponse) {
                    emitter.send(partialResponse);
                    log.info("收到消息片段: {}", partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    emitter.complete();
                    log.info("消息结束，完整消息ID: {}", completeResponse);
                    org.ruoyi.chat.support.RetryNotifier.clear(emitter);
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                    ChatServiceHelper.onStreamError(emitter, error.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("千问请求失败：{}", e.getMessage());
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

        StreamingChatModel model = QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();

        try {
            // 将 ruoyi-ai 的 ChatRequest 转换为 langchain4j 的格式
            dev.langchain4j.model.chat.request.ChatRequest chatRequest = convertToLangchainRequest(request);
            model.chat(chatRequest, handler);
        } catch (Exception e) {
            log.error("workflow 千问请求失败：{}", e.getMessage(), e);
            throw new RuntimeException("QianWen workflow chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCategory() {
        return ChatModeType.QIANWEN.getCode();
    }


}
