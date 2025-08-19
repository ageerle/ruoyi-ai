package org.ruoyi.chat.support;

import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.chat.util.SSEUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 抽取各聊天实现类的通用逻辑：
 * - 创建带开关的 SSE 监听器
 * - 统一的流错误处理（根据是否在重试场景决定通知或直接结束）
 * - 统一的完成处理（清理回调并 complete）
 */
public class ChatServiceHelper {

    public static SSEEventSourceListener createOpenAiListener(SseEmitter emitter, ChatRequest chatRequest) {
        boolean retryEnabled = Boolean.TRUE.equals(chatRequest.getAutoSelectModel());
        return new SSEEventSourceListener(
                emitter,
                chatRequest.getUserId(),
                chatRequest.getSessionId(),
                chatRequest.getToken(),
                retryEnabled
        );
    }

    public static void onStreamError(SseEmitter emitter, String errorMessage) {
        SSEUtil.sendErrorEvent(emitter, errorMessage);
        if (RetryNotifier.hasCallback(emitter)) {
            RetryNotifier.notifyFailure(emitter);
        } else {
            emitter.complete();
        }
    }

    public static void onStreamComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } finally {
            RetryNotifier.clear(emitter);
        }
    }
}


