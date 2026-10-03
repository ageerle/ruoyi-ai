package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaSceneCandidateDiagnosticsTest {
    @Test void rawResponseRemainsUnreviewedAfterFailedOrPassedAutomaticChecks() {
        var memory = new ShortDramaSceneCheckpointTest.Memory(); var clock = new AtomicLong(1000);
        var store = new ShortDramaSceneCandidateDiagnostics(memory, clock::get);
        var identity = ShortDramaSceneCheckpointTest.identity("工匠试水");
        String id = store.recordResponse(identity, "request-1", 1, "plan", "not parseable JSON，朱承晏：2026程序员");
        assertEquals("pending", store.read(identity).get(0).validationStatus());
        store.checked(identity, id, "本场原文未安排出镜角色：精灵");
        var failed = store.read(identity).get(0);
        assertEquals("unreviewed_candidate", failed.state()); assertFalse(failed.validated());
        assertTrue(failed.validationError().contains("精灵")); assertTrue(failed.modelResponse().contains("not parseable JSON"));
        assertEquals("request-1", failed.requestId()); assertEquals(identity, failed.identity());
        var passedId = store.recordResponse(identity, "request-1", 2, "repair", "[]"); store.checked(identity, passedId, null);
        assertFalse(store.read(identity).get(1).validated());
        assertEquals("automatic_checks_passed_unreviewed", store.read(identity).get(1).validationStatus());
        assertNull(new ShortDramaSceneCheckpoint(memory).compatible(identity, ShortDramaSceneCheckpointTest.assets(), true));
    }
    @Test void responseLengthAttemptCountExpiryAndIdentityAreBoundedWithoutCredentialLeaks() {
        var memory = new ShortDramaSceneCheckpointTest.Memory(); var clock = new AtomicLong(1000);
        var store = new ShortDramaSceneCandidateDiagnostics(memory, clock::get); var identity = ShortDramaSceneCheckpointTest.identity("原场");
        String response = "api_key=sample-private-key Bearer provider-private-token sk-sampleLongSecret " + "光".repeat(70_000);
        for (int i = 1; i <= 25; i++) store.recordResponse(identity, "request-" + i, 1, "plan", response);
        var entries = store.read(identity); assertEquals(20, entries.size()); assertEquals("request-6", entries.get(0).requestId());
        var c = entries.get(0); assertTrue(c.responseTruncated()); assertTrue(c.credentialsRedacted());
        assertEquals(response.length(), c.originalResponseChars()); assertTrue(c.modelResponse().length() <= 65_536);
        assertFalse(c.modelResponse().contains("sample-private-key")); assertFalse(c.modelResponse().contains("provider-private-token"));
        assertFalse(c.modelResponse().contains("sampleLongSecret"));
        assertTrue(store.read(ShortDramaSceneCheckpointTest.identity("已改场")).isEmpty());
        clock.addAndGet(ShortDramaSceneCandidateDiagnostics.RETENTION_MILLIS); assertTrue(store.read(identity).isEmpty());
    }
}
