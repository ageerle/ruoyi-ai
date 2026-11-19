package org.ruoyi.chat.service.coding.harness.runtime;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Fixed-size striped command gate prevents an unbounded session-lock cache. */
@Component
public class HarnessSessionGate {

    private static final int STRIPE_COUNT = 256;
    private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

    public HarnessSessionGate() {
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new ReentrantLock(true);
        }
    }

    public <T> T withSession(HarnessOwner owner, String sessionId, Supplier<T> operation) {
        int hash = 31 * owner.hashCode() + sessionId.hashCode();
        ReentrantLock lock = stripes[(hash & Integer.MAX_VALUE) % stripes.length];
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
