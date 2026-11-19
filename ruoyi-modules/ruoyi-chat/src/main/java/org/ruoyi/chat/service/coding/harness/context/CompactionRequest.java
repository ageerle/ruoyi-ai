package org.ruoyi.chat.service.coding.harness.context;

/** Metadata for a normal pressure compaction or one provider-overflow recovery attempt. */
public record CompactionRequest(
    String modelIdentity,
    long sourceUsageTimestamp,
    String overflowId,
    boolean emergency,
    long now
) {

    public CompactionRequest {
        if (modelIdentity == null || modelIdentity.isBlank() || sourceUsageTimestamp < 0 || now <= 0) {
            throw new IllegalArgumentException("Invalid compaction request");
        }
        overflowId = overflowId == null || overflowId.isBlank() ? null : overflowId;
        if (emergency && overflowId == null) {
            throw new IllegalArgumentException("Emergency compaction requires an overflow id");
        }
    }

    public static CompactionRequest pressure(String modelIdentity, long sourceUsageTimestamp,
                                             long now) {
        return new CompactionRequest(modelIdentity, sourceUsageTimestamp, null, false, now);
    }

    public static CompactionRequest emergency(String modelIdentity, long sourceUsageTimestamp,
                                              String overflowId, long now) {
        return new CompactionRequest(modelIdentity, sourceUsageTimestamp, overflowId, true, now);
    }
}
