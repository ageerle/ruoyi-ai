package org.ruoyi.chat.service.coding.harness.plan;

/** Optimistic-concurrency failure for scheduler/evidence mutations. */
public final class StalePlanRevisionException extends IllegalStateException {

    private final long expectedRevision;
    private final long actualRevision;

    public StalePlanRevisionException(long expectedRevision, long actualRevision) {
        super("Stale plan revision: expected " + expectedRevision + " but current revision is "
            + actualRevision);
        this.expectedRevision = expectedRevision;
        this.actualRevision = actualRevision;
    }

    public long expectedRevision() {
        return expectedRevision;
    }

    public long actualRevision() {
        return actualRevision;
    }
}
