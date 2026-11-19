package org.ruoyi.chat.service.coding.harness.store;

public class HarnessOptimisticLockException extends HarnessStoreException {
    public HarnessOptimisticLockException(String resource, long expected, long actual) {
        super("Concurrent update for " + resource + ": expected revision " + expected
            + " but found " + actual);
    }
}
