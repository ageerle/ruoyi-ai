package com.xmzs.common.chat.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.entity.chat.ChatChoice;
import com.xmzs.common.chat.entity.chat.ChatCompletionResponse;
import com.xmzs.common.core.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Objects;

/**
 * 描述：OpenAIEventSourceListener
 *
 * @author https:www.unfbx.com
 * @date 2023-02-22
 */
@Slf4j
@AllArgsConstructor
public class SSEEventSourceListener extends EventSourceListener {

    private static final String DONE_SIGNAL = "[DONE]";

    private final ResponseBodyEmitter emitter;

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
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        try {
            log.info("响应数据{}=========",data);
            emitter.send(data);
            if (data.equals(DONE_SIGNAL)) {
                //成功响应
                emitter.complete();
            }
        } catch (Exception e) {
            log.error("sse信息推送失败！");
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
