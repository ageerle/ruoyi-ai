package org.ruoyi.chat.support;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 失败回调通知器：基于发射器实例（SseEmitter 等对象地址）绑定回调，
 * 避免与业务标识绑定，且能跨线程正确关联。
 */
public class RetryNotifier {

    private static final Map<Integer, Runnable> FAILURE_CALLBACKS = new ConcurrentHashMap<>();

    private static int keyOf(Object obj) {
        return System.identityHashCode(obj);
    }

    public static void setFailureCallback(Object emitterLike, Runnable callback) {
        if (emitterLike == null || callback == null) {
            return;
        }
        FAILURE_CALLBACKS.put(keyOf(emitterLike), callback);
    }

    public static void clear(Object emitterLike) {
        if (emitterLike == null) {
            return;
        }
        FAILURE_CALLBACKS.remove(keyOf(emitterLike));
    }

    public static void notifyFailure(Object emitterLike) {
        if (emitterLike == null) {
            return;
        }
        Runnable cb = FAILURE_CALLBACKS.get(keyOf(emitterLike));
        if (Objects.nonNull(cb)) {
            cb.run();
        }
    }
}


