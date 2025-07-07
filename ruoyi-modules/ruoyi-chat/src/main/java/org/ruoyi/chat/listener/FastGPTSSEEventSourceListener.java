package org.ruoyi.chat.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastGPTSSEEventSourceListener extends EventSourceListener {

    private SseEmitter emitter;

    @Autowired(required = false)
    public FastGPTSSEEventSourceListener(SseEmitter emitter) {
        this.emitter = emitter;
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
            if ("flowResponses".equals(type)){
                emitter.send(data);
                emitter.complete();
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
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            log.error("FastGPT  sse连接异常data：{}，异常：{}", body.string(), t);
        } else {
            log.error("FastGPT sse连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }
}
