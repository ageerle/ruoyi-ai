package org.ruoyi.service.coding.harness.journal;

/** Signals an export failure to the caller; no run state is mutated by journal projection. */
public class JournalProjectionException extends Exception {

    public JournalProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
