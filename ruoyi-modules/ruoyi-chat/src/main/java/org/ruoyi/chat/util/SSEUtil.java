package org.ruoyi.chat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * sse工具类
 *
 * @author WangLe
 */
@Slf4j
public class SSEUtil {

    /**
     * 发送SSE错误事件的封装方法
     *
     * @param sseEmitter sse事件对象
     * @param errorMessage 错误信息
     */
    public static void sendErrorEvent(ResponseBodyEmitter sseEmitter, String errorMessage) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("error")
                .data(errorMessage);
        try {
            sseEmitter.send(event);
        } catch (IOException e) {
            log.error("SSE发送失败: {}", e.getMessage());
        }
        sseEmitter.complete();
    }
}
