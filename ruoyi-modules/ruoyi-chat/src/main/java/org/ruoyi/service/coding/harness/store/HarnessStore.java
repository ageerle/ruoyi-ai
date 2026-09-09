package org.ruoyi.service.coding.harness.store;

import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for the durable Harness. */
public interface HarnessStore {

    HarnessSessionState createSession(HarnessSessionState session);

    Optional<HarnessSessionState> findSession(HarnessOwner owner, String sessionId);

    List<HarnessSessionState> listSessions(HarnessOwner owner);

    HarnessSessionState setSessionPinned(HarnessOwner owner, String sessionId, boolean pinned);

    HarnessSessionState setSessionDeleted(HarnessOwner owner, String sessionId, boolean deleted);

    HarnessSessionState saveSession(HarnessOwner owner, HarnessSessionState session, long expectedRevision);

    HarnessRunState createRun(HarnessOwner owner, HarnessRunState run);

    Optional<HarnessRunState> findRun(HarnessOwner owner, String sessionId, String runId);

    List<HarnessRunState> listRuns(HarnessOwner owner, String sessionId);

    HarnessRunState saveRun(HarnessOwner owner, HarnessRunState run, long expectedRevision);

    /**
     * Saves a run only if both its revision and the session message-ledger high-watermark still
     * match the caller's observation. Implementations must validate both conditions and persist
     * the snapshot inside the same session-level critical section used by message append.
     */
    HarnessRunState saveRunIfMessageLedgerUnchanged(HarnessOwner owner, HarnessRunState run,
                                                    long expectedRevision,
                                                    long expectedMessageSequence);

    /**
     * Internal, read-only enumeration for process startup recovery. Implementations must return a
     * stable page ordered by an opaque cursor and verify that each snapshot belongs to the owner,
     * session, and run encoded by its physical storage location.
     */
    HarnessRunScanPage scanRunsForRecovery(String afterCursor, int limit);

    HarnessMessage appendMessage(HarnessOwner owner, HarnessMessage message);

    List<HarnessMessage> readMessages(HarnessOwner owner, String sessionId, long afterSequence, int limit);

    HarnessEvent appendEvent(HarnessOwner owner, HarnessEvent event);

    List<HarnessEvent> readEvents(HarnessOwner owner, String sessionId, String runId,
                                  long afterSequence, int limit);
}
