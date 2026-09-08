package org.ruoyi.service.coding.harness.store;

/** The immutable session message ledger advanced after it was inspected. */
public final class HarnessMessageLedgerConflictException extends HarnessStoreException {

    public HarnessMessageLedgerConflictException(String sessionId, long expected, long actual) {
        super("Concurrent message append for session " + sessionId
            + ": expected ledger high-watermark " + expected + " but found " + actual);
    }
}
