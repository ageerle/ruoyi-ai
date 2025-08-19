package org.ruoyi.chat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

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
        try {
            sseEmitter.send(errorMessage);
        } catch (IOException e) {
            log.error("SSE发送失败: {}", e.getMessage());
        }
        // 不立即关闭，由上层策略决定是否继续重试或降级
    }
}
