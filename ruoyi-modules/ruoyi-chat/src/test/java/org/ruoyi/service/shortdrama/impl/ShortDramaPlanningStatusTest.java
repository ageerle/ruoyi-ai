package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaPlanningStatusTest {
    @Test void assetAndStoryboardReceiptsUseTheSameLifecycleInSeparateNamespaces() {
        var memory = new ShortDramaSceneCheckpointTest.Memory();
        var assets = new ShortDramaPlanningStatus(memory, "asset-status-v1");
        var shots = new ShortDramaPlanningStatus(memory);
        var queued = assets.queued(10L, 20L, "same-request", "hash", "flash", 0);
        shots.queued(10L, 20L, "same-request", "hash", "flash", 1);
        assets.update(queued, "done", 8, null);
        assertEquals("done", assets.read(10L, 20L, "same-request").state());
        assertEquals("queued", shots.read(10L, 20L, "same-request").state());
    }
    @Test void receiptSurvivesConnectionIndependentlyAndAnotherRuntimeCannotClaimAnUpstreamFailure() {
        var memory = new ShortDramaSceneCheckpointTest.Memory(); var store = new ShortDramaPlanningStatus(memory);
        var queued = store.queued(10L, 20L, "request-1", "script-hash", "writer", 4);
        assertEquals("queued", store.read(10L, 20L, null).state());
        store.update(queued, "running", null, null); assertEquals("running", store.read(10L, 20L, "request-1").state());
        var anotherRuntime = new ShortDramaPlanningStatus(memory);
        assertFalse(anotherRuntime.currentRuntime(anotherRuntime.read(10L, 20L, null)));
        assertFalse(anotherRuntime.read(10L, 20L, null).terminal());
        store.update(queued, "done", 31, null);
        assertTrue(anotherRuntime.read(10L, 20L, null).terminal()); assertEquals(31, anotherRuntime.read(10L, 20L, null).panelCount());
        assertNull(store.read(11L, 20L, "request-1"));
    }
    @Test void unsafeRequestIdsAndCredentialBearingErrorsAreNotRecorded() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaPlanningStatus.checkedRequestId("../../other-script"));
        var store = new ShortDramaPlanningStatus(new ShortDramaSceneCheckpointTest.Memory());
        var queued = store.queued(10L, 20L, "request-1", "hash", "writer", 4);
        var failed = store.update(queued, "error", null, "Authorization: Bearer private-provider-token");
        assertFalse(failed.error().contains("private-provider-token")); assertTrue(failed.terminal());
    }
    @Test void terminalReceiptWinsOverLateCallbacksHoldingTheOriginalQueuedReceipt() {
        var store = new ShortDramaPlanningStatus(new ShortDramaSceneCheckpointTest.Memory());
        var queued = store.queued(10L, 20L, "request-1", "hash", "writer", 4);
        store.update(queued, "running", null, null);
        var failed = store.update(queued, "error", null, "binding failure");
        assertEquals(failed, store.update(queued, "running", null, null));
        assertEquals(failed, store.update(queued, "done", 80, null));
        assertEquals("error", store.read(10L, 20L, null).state());
        assertEquals("binding failure", store.read(10L, 20L, "request-1").error());
        assertThrows(IllegalStateException.class, () -> store.queued(10L, 20L, "request-1", "hash", "writer", 4));
    }
    @Test void lateOlderCallbacksDoNotReplaceTheLatestSubmissionAndRunningCannotRegressToQueued() {
        var store = new ShortDramaPlanningStatus(new ShortDramaSceneCheckpointTest.Memory());
        var old = store.queued(10L, 20L, "older", "hash", "writer", 4);
        store.update(old, "done", 2, null);
        var next = store.queued(10L, 20L, "newer", "hash", "writer", 4);
        store.update(next, "running", null, null); store.update(next, "queued", null, null);
        store.update(old, "error", null, "late error");
        assertEquals("newer", store.read(10L, 20L, null).requestId());
        assertEquals("running", store.read(10L, 20L, null).state());
        assertEquals("done", store.read(10L, 20L, "older").state());
    }
    @Test void completedReceiptBindsOrderedStoryboardIdsAndContentHash() {
        var store = new ShortDramaPlanningStatus(new ShortDramaSceneCheckpointTest.Memory());
        var queued = store.queued(10L, 20L, "request-1", "hash", "writer", 4);
        var completed = store.complete(queued, java.util.List.of(101L, 102L), "a".repeat(64));
        assertEquals(java.util.List.of(101L, 102L), completed.storyboardIds());
        assertEquals("a".repeat(64), completed.storyboardHash());
        assertEquals(2, completed.panelCount());
        assertThrows(IllegalArgumentException.class, () -> {
            var next = store.queued(10L, 20L, "request-2", "hash", "writer", 4);
            store.complete(next, java.util.List.of(), "b".repeat(64));
        });
    }
}
