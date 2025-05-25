package org.ruoyi.chat.service.chat.impl;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


/**
 * 智谱AI
 */
@Service
@Slf4j
public class ZhipuAiChatServiceImpl  implements IChatService {

    @Autowired
    private IChatModelService chatModelService;


    ToolSpecification currentTime = ToolSpecification.builder()
            .name("currentTime")
            .description("currentTime")
            .build();


    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter){
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        // 发送流式消息
        try {
            StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
                @SneakyThrows
                @Override
                public void onPartialResponse(String token) {
                    System.out.println(token);
                    emitter.send(token);
                }

                @SneakyThrows
                @Override
                public void onError(Throwable error) {
                    System.out.println(error.getMessage());
                    emitter.send(error.getMessage());
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    emitter.complete();
                    log.info("消息结束，完整消息ID: {}", response.aiMessage());
                }
            };

            StreamingChatModel model = ZhipuAiStreamingChatModel.builder()
                    .model(chatModelVo.getModelName())
                    .apiKey(chatModelVo.getApiKey())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            model.chat(chatRequest.getPrompt(), handler);
        } catch (Exception e) {
            log.error("智谱清言请求失败：{}", e.getMessage());
        }

        return emitter;
    }

    @Override
    public String getCategory() {
        return ChatModeType.ZHIPU.getCode();
    }
}
