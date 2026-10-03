package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@Tag("dev")
class ShortDramaPlanningProgressTest {
    @Test void nestedProgressWriteRestoresOuterTenantOnSuccessAndFailure() {
        var current = new java.util.concurrent.atomic.AtomicReference<>("outer-tenant");
        try (var tenant = mockStatic(org.ruoyi.common.tenant.helper.TenantHelper.class)) {
            tenant.when(org.ruoyi.common.tenant.helper.TenantHelper::getDynamic).thenAnswer(call -> current.get());
            tenant.when(() -> org.ruoyi.common.tenant.helper.TenantHelper.setDynamic(anyString())).thenAnswer(call -> { current.set(call.getArgument(0)); return null; });
            tenant.when(() -> org.ruoyi.common.tenant.helper.TenantHelper.dynamic(anyString(), any(Runnable.class))).thenAnswer(call -> {
                current.set(call.getArgument(0));
                try { ((Runnable)call.getArgument(1)).run(); } finally { current.set(null); }
                return null;
            });
            ShortDramaPlanningProgress.persistInTenant("callback-tenant", () -> assertEquals("callback-tenant", current.get()));
            assertEquals("outer-tenant", current.get());
            assertThrows(IllegalStateException.class, () -> ShortDramaPlanningProgress.persistInTenant("callback-tenant", () -> { throw new IllegalStateException("cache unavailable"); }));
            assertEquals("outer-tenant", current.get());
            current.set(null);
            ShortDramaPlanningProgress.persistInTenant("callback-tenant", () -> {});
            assertNull(current.get());
        }
    }
    @Test void tracksRealActivityWithoutPersistingReasoningAndTerminalIgnoresLateCallbacks() {
        var snapshots = new ArrayList<ShortDramaPlanningProgress.Snapshot>();
        var tracker = new ShortDramaPlanningProgress.Tracker("original-request", 123, snapshots::add);
        String id = tracker.startCall("storyboard_plan_parallel", "第1场规划", 2000);
        tracker.activity(id, "private reasoning", true); tracker.activity(id, "正文", false);
        var call = tracker.finishCall(id, true);
        assertEquals(2, call.contentChars()); assertEquals(17, call.thinkingChars());
        assertTrue(call.firstContentAt() > 0); assertEquals(2000, call.promptChars());
        assertFalse(tracker.snapshot().toString().contains("private reasoning"));
        tracker.terminal("done"); int count = snapshots.size();
        tracker.phase("storyboard_plan", "late"); tracker.activity(id, "late", false); tracker.heartbeat();
        assertEquals(count, snapshots.size()); assertEquals("done", tracker.snapshot().phase());
    }
    @Test void sceneLocalCardsBecomeReadyUsingGlobalNumbersAndCannotRegress() throws Exception {
        var tracker = new ShortDramaPlanningProgress.Tracker("request", 1, state -> {});
        var node = AtlasMediaSupport.OBJECT_MAPPER.readTree("{\"panel_number\":1,\"description\":\"动作\",\"duration\":7,\"shot_design\":{\"focus\":\"手\"}}");
        tracker.card(2, 1, "planned", node); tracker.bindGlobal(9, 2, 1); tracker.ready(9, node);
        tracker.card(2, 1, "draft", node); tracker.card(1, 1, "draft", node); tracker.clearDrafts(1);
        assertEquals(1, tracker.snapshot().cards().size());
        assertEquals("ready", tracker.snapshot().cards().get(0).stage()); assertEquals("2:1", tracker.snapshot().cards().get(0).key());
        assertEquals(7, tracker.snapshot().cards().get(0).panel().path("duration").asInt());
    }
}
