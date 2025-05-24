package org.ruoyi.chat.listener;


import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

/**
 *  OpenAIEventSourceListener
 *
 * @author https:www.unfbx.com
 * @date 2023-02-22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SSEEventSourceListener extends EventSourceListener {

    private SseEmitter emitter;

    private Long userId;

    private Long sessionId;

    @Autowired(required = false)
    public SSEEventSourceListener(SseEmitter emitter,Long userId,Long sessionId) {
        this.emitter = emitter;
        this.userId = userId;
        this.sessionId = sessionId;
    }


    private StringBuilder stringBuffer = new StringBuilder();

    private String modelName;

    private static final IChatCostService chatCostService = SpringUtils.getBean(IChatCostService.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("OpenAI建立sse连接...");
    }

    /**
     * {@inheritDoc}
     */
    @SneakyThrows
    @Override
    public void onEvent(@NotNull EventSource eventSource, String id, String type, String data) {
        try {
            if ("[DONE]".equals(data)) {
                //成功响应
                emitter.complete();
                // 扣除费用
                ChatRequest chatRequest = new ChatRequest();
                // 设置对话角色
                chatRequest.setRole(Message.Role.ASSISTANT.getName());
                chatRequest.setModel(modelName);
                chatRequest.setUserId(userId);
                chatRequest.setSessionId(sessionId);
                chatRequest.setPrompt(stringBuffer.toString());
                chatCostService.deductToken(chatRequest);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            ChatCompletionResponse completionResponse = mapper.readValue(data, ChatCompletionResponse.class);
            if(completionResponse == null || CollectionUtil.isEmpty(completionResponse.getChoices())){
                return;
            }
            Object content = completionResponse.getChoices().get(0).getDelta().getContent();

            if(content != null ){
                if(StringUtils.isEmpty(modelName)){
                    modelName = completionResponse.getModel();
                }
                stringBuffer.append(content);
                emitter.send(data);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.info("OpenAI关闭sse连接...");
    }

    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (Objects.isNull(response)) {
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", body.string(), t);
        } else {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }

}
