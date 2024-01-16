package com.xmzs.system.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.entity.chat.BaseChatCompletion;
import com.xmzs.common.chat.entity.chat.ChatCompletionResponse;
import com.xmzs.common.chat.utils.TikTokensUtil;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.service.ChatService;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.TextReviewService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Objects;

/**
 * 描述：OpenAIEventSourceListener
 *
 * @author https:www.unfbx.com
 * @date 2023-02-22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SSEEventSourceListener extends EventSourceListener {

    private ResponseBodyEmitter emitter;

    private StringBuilder stringBuffer = new StringBuilder();

    @Autowired(required = false)
    public SSEEventSourceListener(ResponseBodyEmitter emitter) {
        this.emitter = emitter;
    }

    private String modelName;
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
            if (data.equals("[DONE]")) {
                //成功响应
                emitter.complete();
                if(StringUtils.isNotEmpty(modelName)){
                    ChatService chatService = SpringUtils.context().getBean(ChatService.class);
                    // 扣除余额
                    int tokens = TikTokensUtil.tokens(modelName,stringBuffer.toString());
                    ChatMessageBo chatMessageBo = new ChatMessageBo();
                    chatMessageBo.setModelName(modelName);
                    chatMessageBo.setContent(stringBuffer.toString());
                    chatMessageBo.setTotalTokens(tokens);
                    Long userId = (Long)LocalCache.CACHE.get("userId");
                    chatMessageBo.setUserId(userId);
                    chatService.deductToken(chatMessageBo);
                }
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            ChatCompletionResponse completionResponse = mapper.readValue(data, ChatCompletionResponse.class);
            if(completionResponse == null){
                return;
            }
            String content = completionResponse.getChoices().get(0).getDelta().getContent();
            if(StringUtils.isEmpty(content)){
                return;
            }
            if(StringUtils.isEmpty(modelName)){
                modelName = completionResponse.getModel();
            }
            stringBuffer.append(content);
            emitter.send(data);
        } catch (Exception e) {
            log.error("sse信息推送失败{}内容：{}",e.getMessage(),data);
            eventSource.cancel();
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
