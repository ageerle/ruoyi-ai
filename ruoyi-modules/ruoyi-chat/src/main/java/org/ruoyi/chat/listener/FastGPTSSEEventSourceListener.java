package org.ruoyi.chat.listener;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.ruoyi.chat.support.RetryNotifier;
import org.ruoyi.chat.util.SSEUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastGPTSSEEventSourceListener extends EventSourceListener {

    private SseEmitter emitter;
    private Long sessionId;

    @Autowired(required = false)
    public FastGPTSSEEventSourceListener(SseEmitter emitter) {
        this.emitter = emitter;
    }

    public FastGPTSSEEventSourceListener(SseEmitter emitter, Long sessionId) {
        this.emitter = emitter;
        this.sessionId = sessionId;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("FastGPT  sse连接成功");
    }

    @Override
    public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
        try {
            log.debug("事件类型为: {}", type);
            log.debug("事件数据为: {}", data);
            if ("flowResponses".equals(type)) {
                emitter.send(data);
                emitter.complete();
                RetryNotifier.clear(emitter);
            } else {
                emitter.send(data);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.info("FastGPT  sse连接关闭");
    }

    @Override
    @SneakyThrows
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (Objects.isNull(response)) {
            SSEUtil.sendErrorEvent(emitter, t != null ? t.getMessage() : "SSE连接失败");
            RetryNotifier.notifyFailure(emitter);
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            String msg = body.string();
            log.error("FastGPT  sse连接异常data：{}，异常：{}", msg, t);
            SSEUtil.sendErrorEvent(emitter, msg);
            RetryNotifier.notifyFailure(emitter);
        } else {
            log.error("FastGPT sse连接异常data：{}，异常：{}", response, t);
            SSEUtil.sendErrorEvent(emitter, String.valueOf(response));
            RetryNotifier.notifyFailure(emitter);
        }
        eventSource.cancel();
    }
}
