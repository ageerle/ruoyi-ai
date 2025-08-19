package org.ruoyi.chat.support;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 失败回调通知器：基于 sessionId 绑定回调，底层失败时按 sessionId 通知上层重试调度器。
 */
public class RetryNotifier {

    private static final Map<Long, Runnable> FAILURE_CALLBACKS = new ConcurrentHashMap<>();

    public static void setFailureCallback(Long sessionId, Runnable callback) {
        if (sessionId == null || callback == null) {
            return;
        }
        FAILURE_CALLBACKS.put(sessionId, callback);
    }

    public static void clear(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        FAILURE_CALLBACKS.remove(sessionId);
    }

    public static void notifyFailure(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        Runnable cb = FAILURE_CALLBACKS.get(sessionId);
        if (Objects.nonNull(cb)) {
            cb.run();
        }
    }
}


