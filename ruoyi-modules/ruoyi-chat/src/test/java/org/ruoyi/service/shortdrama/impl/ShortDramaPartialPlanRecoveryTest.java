package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.bo.shortdrama.ShortDramaReviewedPlanBo;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaPartialPlanRecoveryTest {
    private static final Map<String, Object> LEGACY = new ConcurrentHashMap<>();
    private ShortDramaServiceImpl service;
    private ShortDramaSceneCheckpointTest.Memory memory;
    private ShortDramaSceneCheckpoint.Assets assets;
    private ShortDramaScript script;
    private ShortDramaReviewedPlanBo review;

    @BeforeAll @SuppressWarnings("unchecked") static void springAndRedis() {
        var context = new GenericApplicationContext();
        var redis = mock(RedissonClient.class);
        when(redis.getBucket(anyString())).thenAnswer(call -> {
            String key = call.getArgument(0); RBucket<Object> bucket = mock(RBucket.class);
            when(bucket.get()).thenAnswer(ignored -> LEGACY.get(key));
            doAnswer(c -> { LEGACY.put(key, c.getArgument(0)); return null; }).when(bucket).set(any(), any(Duration.class));
            return bucket;
        });
        context.getBeanFactory().registerSingleton("redisson", redis);
        context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules()); context.refresh();
        new cn.hutool.extra.spring.SpringUtil().setApplicationContext(context);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(context.getBeanFactory());
    }

    @BeforeEach void fixture() {
        LEGACY.clear(); memory = new ShortDramaSceneCheckpointTest.Memory(); assets = ShortDramaSceneCheckpointTest.assets();
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "sceneCheckpoints", new ShortDramaSceneCheckpoint(memory));
        ReflectionTestUtils.setField(service, "sceneCandidates", new ShortDramaSceneCandidateDiagnostics(new ShortDramaSceneCheckpointTest.Memory(), System::currentTimeMillis));
        ReflectionTestUtils.setField(service, "planningStatuses", new ShortDramaPlanningStatus(new ShortDramaSceneCheckpointTest.Memory()));
        ReflectionTestUtils.setField(service, "activeEmitters", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(service, "storyboardGenerationStates", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(service, "assetGenerationStates", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(service, "planningSkillSnapshots", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(service, "planningProgress", new ConcurrentHashMap<>());
        var catalog = mock(ShortDramaSkillCatalog.class);
        when(catalog.snapshot(any())).thenReturn(new ShortDramaSkillCatalog.Snapshot(10L, "fixture", ""));
        when(catalog.effectiveArtStyle(any())).thenReturn("chinese-3d");
        when(catalog.selectedVisual(any(), anyString())).thenReturn("");
        when(catalog.selected(any(), anyString())).thenReturn("");
        ReflectionTestUtils.setField(service, "skillCatalog", catalog);
        var projects = mock(ShortDramaProjectMapper.class); var scripts = mock(ShortDramaScriptMapper.class);
        var chars = mock(ShortDramaCharacterMapper.class); var appearances = mock(ShortDramaCharacterAppearanceMapper.class);
        var locations = mock(ShortDramaLocationMapper.class); var boards = mock(ShortDramaStoryboardMapper.class); var models = mock(IChatModelService.class);
        ReflectionTestUtils.setField(service, "projectMapper", projects); ReflectionTestUtils.setField(service, "scriptMapper", scripts);
        ReflectionTestUtils.setField(service, "characterMapper", chars); ReflectionTestUtils.setField(service, "characterAppearanceMapper", appearances);
        ReflectionTestUtils.setField(service, "locationMapper", locations); ReflectionTestUtils.setField(service, "storyboardMapper", boards);
        ReflectionTestUtils.setField(service, "chatModelService", models);
        var project = new ShortDramaProject(); project.setId(10L); project.setUserId(1L); project.setArtStyle("chinese-3d"); project.setComposeAspectRatio("16:9");
        when(projects.selectById(10L)).thenReturn(project);
        script = ShortDramaSceneCheckpointTest.script(); script.setScriptText("外景 修渠处（预计5秒）\n工匠：「可以试水了。」\n外景 修渠处（预计5秒）\n工匠：「再看一眼。」");
        when(scripts.selectById(20L)).thenReturn(script);
        when(chars.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(assets.characters());
        when(appearances.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(assets.appearances());
        when(locations.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(assets.locations());
        when(boards.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        var model = new ChatModelVo(); model.setModelName("writer"); when(models.selectModelByName("writer")).thenReturn(model);
        review = new ShortDramaReviewedPlanBo(); review.setScriptId(20L); review.setModel("writer"); review.setExpectedScriptText(script.getScriptText());
        review.setMinimumShotSeconds(4); review.setSceneNumbers(List.of(1)); review.setExpectedAssetSignature(assets.signature());
        review.setPanels(List.of(ShortDramaSceneCheckpointTest.panel()));
    }

    @Test void explicitPartialImportStoresOnlyApprovedSceneAndReadOnlyStatusProvesIt() {
        assertEquals(1, service.importReviewedPlan(10L, review, 1L)); assertEquals(1, memory.values.size());
        var result = service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4);
        assertEquals(1L, result.get("validatedSceneCount")); assertEquals(4, result.get("minimumShotSeconds"));
        assertEquals(0L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 1).get("validatedSceneCount"));
        assertEquals(1, memory.values.size()); // status never writes/promotes/submits.
        assertTrue(LEGACY.keySet().stream().noneMatch(k -> k.endsWith(":scene:2")));
    }

    @Test void refusedImportCannotWriteAnyCheckpointAndLockIsReleased() {
        review.setExpectedAssetSignature("0".repeat(64)); assertThrows(IllegalStateException.class, () -> service.importReviewedPlan(10L, review, 1L));
        assertTrue(memory.values.isEmpty()); assertTrue(LEGACY.isEmpty());
        review.setExpectedAssetSignature(assets.signature()); assertEquals(1, service.importReviewedPlan(10L, review, 1L));
    }

    @Test void ownershipScriptPositiveDurationAndSelectedScenesRemainMandatoryWhileDialogueIsAdvisory() {
        assertThrows(IllegalArgumentException.class, () -> service.importReviewedPlan(10L, review, 2L));
        review.setExpectedScriptText("old script"); assertThrows(IllegalStateException.class, () -> service.importReviewedPlan(10L, review, 1L));
        review.setExpectedScriptText(script.getScriptText()); review.getPanels().get(0).setSourceText("工匠观察水渠");
        assertFalse(ShortDramaServiceImpl.scenePlanIssues(ShortDramaServiceImpl.splitScriptScenes(script.getScriptText()).get(0), review.getPanels()).isEmpty());
        review.getPanels().get(0).setSourceText("工匠：「可以试水了。」"); review.getPanels().get(0).setDuration(0);
        assertThrows(IllegalStateException.class, () -> service.importReviewedPlan(10L, review, 1L));
        review.getPanels().get(0).setDuration(5); review.setSceneNumbers(List.of(2));
        assertThrows(IllegalArgumentException.class, () -> service.importReviewedPlan(10L, review, 1L)); assertTrue(memory.values.isEmpty());
    }

    @Test void newImagesAndUnrelatedSpaceDoNotEraseProofButCurrentAppearanceChangesDo() {
        service.importReviewedPlan(10L, review, 1L); assets.appearances().get(0).setReferenceImageUrl("https://example.test/new.png");
        var extra = new ShortDramaLocation(); extra.setId(9L); extra.setName("别处"); extra.setDescriptions("无关场次"); assets.locations().add(extra);
        assertEquals(1L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4).get("validatedSceneCount"));
        assets.appearances().get(0).setDescription("已改红衣");
        assertEquals(0L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4).get("validatedSceneCount"));
    }

    @Test void nativePlannerReusesApprovedSceneAndOnlyCallsModelForMissingScene() throws Exception {
        service.importReviewedPlan(10L, review, 1L);
        var model = mock(ChatModel.class); var second = ShortDramaSceneCheckpointTest.panel(); second.setSceneNumber(2); second.setSourceText("工匠：「再看一眼。」");
        when(model.chat(anyString())).thenReturn(new ObjectMapper().writeValueAsString(List.of(second)));
        List<ShortDramaServiceImpl.StoryboardPanelData> result = ReflectionTestUtils.invokeMethod(service, "executePhase3_StoryboardPlan", model, null, script, 10L, null, "writer", 4);
        assertNotNull(result); assertEquals(2, result.size()); assertEquals(1, result.get(0).getSceneNumber()); assertEquals(2, result.get(1).getSceneNumber());
        verify(model, times(1)).chat(anyString()); assertEquals(2, memory.values.size());
    }

    @Test void oldGlobalHashWithoutProofIsNotScannedOrSilentlyMigrated() {
        LEGACY.put("short-drama:checkpoint:v1:10:20:old-unrelated-hash:scene:1", "old JSON");
        assertEquals(0L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4).get("validatedSceneCount")); assertTrue(memory.values.isEmpty());
    }
    @Test void nativeBindingFailurePreservesEveryActualResponseBeforeValidationAndGetDoesNotPromote() throws Exception {
        script.setScriptText("外景 修渠处（预计5秒）\n工匠：「可以试水了。」");
        var model = mock(ChatModel.class); var invalid = ShortDramaSceneCheckpointTest.panel();
        invalid.getCharacters().get(0).setAppearance("2026不存在的形象");
        String raw = new ObjectMapper().writeValueAsString(List.of(invalid)); when(model.chat(anyString())).thenReturn(raw);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(service, "executePhase3_StoryboardPlan", model, null, script, 10L, null, "writer", 4, "actual-test-request"));
        var status = service.storyboardCandidates(10L, 20L, "writer", 1L, null);
        assertEquals(4, status.get("minimumShotSeconds")); assertFalse((boolean) status.get("validated"));
        @SuppressWarnings("unchecked") var scenes = (List<Map<String, Object>>) status.get("scenes");
        @SuppressWarnings("unchecked") var candidates = (List<ShortDramaSceneCandidateDiagnostics.Candidate>) scenes.get(0).get("candidates");
        assertEquals(2, candidates.size());
        for (var c : candidates) {
            assertEquals(raw, c.modelResponse()); assertEquals("actual-test-request", c.requestId());
            assertEquals("automatic_checks_failed", c.validationStatus()); assertFalse(c.validated());
            assertTrue(c.validationError().contains("2026不存在的形象"));
        }
        assertTrue(memory.values.isEmpty()); assertTrue(LEGACY.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.storyboardCandidates(10L, 20L, "writer", 2L, 4));
        assertThrows(IllegalArgumentException.class, () -> service.storyboardCandidates(10L, 999L, "writer", 1L, 4));
        @SuppressWarnings("unchecked") var incompatible = (List<Map<String, Object>>) service.storyboardCandidates(10L, 20L, "writer", 1L, 1).get("scenes");
        assertTrue(((List<?>) incompatible.get(0).get("candidates")).isEmpty()); verify(model, times(2)).chat(anyString());
    }
    @Test void queueAckAndLockExistBeforeExecutorRunsAndDisconnectCannotResubmitOrCancel() {
        var jobs = new ArrayList<Runnable>();
        ReflectionTestUtils.setField(service, "storyboardPlanningExecutor", (java.util.concurrent.Executor) jobs::add);
        var emitter = service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "test-queue-request");
        var queued = service.storyboardPlanningStatus(10L, 20L, null, 1L);
        assertEquals("queued", queued.get("state")); assertEquals("test-queue-request", queued.get("requestId"));
        assertEquals(true, queued.get("activeLock")); assertEquals(1, jobs.size());
        assertFalse(((Collection<?>) ReflectionTestUtils.getField(emitter, "earlySendAttempts")).isEmpty());
        ReflectionTestUtils.invokeMethod(service, "closeEmitter", emitter);
        assertEquals("queued", service.storyboardPlanningStatus(10L, 20L, null, 1L).get("state"));
        service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "test-queue-request"); // receipt replay
        service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "different-request"); // active lock
        assertEquals(1, jobs.size());
        var compose = mock(org.ruoyi.service.shortdrama.IShortDramaVideoComposeService.class);
        doAnswer(call -> {
            assertEquals("running", service.storyboardPlanningStatus(10L, 20L, null, 1L).get("state"));
            throw new IllegalStateException("synthetic pre-model test failure");
        }).when(compose).invalidateComposition(10L);
        ReflectionTestUtils.setField(service, "videoComposeService", compose); jobs.get(0).run();
        var terminal = service.storyboardPlanningStatus(10L, 20L, "test-queue-request", 1L);
        assertEquals("error", terminal.get("state")); assertEquals(false, terminal.get("activeLock"));
        assertTrue((boolean) terminal.get("terminal")); assertTrue(terminal.get("error").toString().contains("synthetic pre-model"));
        service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "test-queue-request"); assertEquals(1, jobs.size());
    }
    @Test void statusOwnerScriptChecksAndLegacyRunningWithoutReceiptRemainTruthful() {
        assertThrows(IllegalArgumentException.class, () -> service.storyboardPlanningStatus(10L, 20L, null, 2L));
        assertThrows(IllegalArgumentException.class, () -> service.storyboardPlanningStatus(10L, 999L, null, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.planStoryboardStream(10L, 20L, "writer", 2L, 4, "wrong-owner"));
        @SuppressWarnings("unchecked") var locks = (Map<Long, java.util.concurrent.atomic.AtomicBoolean>) ReflectionTestUtils.getField(service, "storyboardGenerationStates");
        locks.put(20L, new java.util.concurrent.atomic.AtomicBoolean(true));
        var result = service.storyboardPlanningStatus(10L, 20L, null, 1L);
        assertEquals("running", result.get("state")); assertNull(result.get("requestId")); assertEquals(false, result.get("retrySafe"));
    }
    @Test void restartedRuntimePreservesTheOldReceiptAndQueuesAnExplicitNewRequest() {
        var statusMemory = new ShortDramaSceneCheckpointTest.Memory(); var oldRuntime = new ShortDramaPlanningStatus(statusMemory);
        oldRuntime.queued(10L, 20L, "original-request", "hash", "writer", 4);
        ReflectionTestUtils.setField(service, "planningStatuses", new ShortDramaPlanningStatus(statusMemory));
        var jobs = new ArrayList<Runnable>(); ReflectionTestUtils.setField(service, "storyboardPlanningExecutor", (java.util.concurrent.Executor) jobs::add);
        var status = service.storyboardPlanningStatus(10L, 20L, null, 1L);
        assertEquals("submission_unknown", status.get("state")); assertFalse((boolean) status.get("terminal"));
        service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "another-request");
        assertEquals(1, jobs.size());
        var queued = service.storyboardPlanningStatus(10L, 20L, null, 1L);
        assertEquals("queued", queued.get("state")); assertEquals("another-request", queued.get("requestId"));
        assertEquals(true, queued.get("activeLock"));
        var oldReceipt = oldRuntime.read(10L, 20L, "original-request");
        assertTrue(oldReceipt.terminal()); assertEquals("error", oldReceipt.state());
    }
    @Test void schedulingRejectionIsVisibleAndReleasesOnlyTheUnstartedLocalQueueLock() {
        ReflectionTestUtils.setField(service, "storyboardPlanningExecutor", (java.util.concurrent.Executor) job -> {
            throw new java.util.concurrent.RejectedExecutionException("synthetic executor rejection");
        });
        service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "unstarted-request");
        var status = service.storyboardPlanningStatus(10L, 20L, null, 1L);
        assertEquals("error", status.get("state")); assertEquals(false, status.get("activeLock"));
        assertTrue(status.get("error").toString().contains("后台任务未接受排队请求"));
    }
    @Test void asynchronousFailureAfterTenantScopeClearsIsWrittenBackToTheSubmissionTenant() {
        var tenant = new ThreadLocal<String>(); tenant.set("test-owner-tenant");
        var namespaceMemory = new ShortDramaSceneCheckpointTest.Memory() {
            public String get(String key) { return super.get(Objects.toString(tenant.get(), "default") + ":" + key); }
            public void put(String key, String value) { super.put(Objects.toString(tenant.get(), "default") + ":" + key, value); }
        };
        try (var scope = mockStatic(org.ruoyi.common.tenant.helper.TenantHelper.class)) {
            scope.when(org.ruoyi.common.tenant.helper.TenantHelper::getTenantId).thenAnswer(call -> tenant.get());
            scope.when(() -> org.ruoyi.common.tenant.helper.TenantHelper.dynamic(anyString(), any(Runnable.class)))
                .thenAnswer(call -> { tenant.set(call.getArgument(0)); try { ((Runnable) call.getArgument(1)).run(); } finally { tenant.remove(); } return null; });
            ReflectionTestUtils.setField(service, "planningStatuses", new ShortDramaPlanningStatus(namespaceMemory));
            var jobs = new ArrayList<Runnable>(); ReflectionTestUtils.setField(service, "storyboardPlanningExecutor", (java.util.concurrent.Executor) jobs::add);
            var compose = mock(org.ruoyi.service.shortdrama.IShortDramaVideoComposeService.class);
            doThrow(new IllegalStateException("synthetic tenant-scoped failure")).when(compose).invalidateComposition(10L);
            ReflectionTestUtils.setField(service, "videoComposeService", compose);
            service.planStoryboardStream(10L, 20L, "writer", 1L, 4, "tenant-request");
            tenant.remove(); // The executor's initial environment does not contain the request tenant.
            jobs.get(0).run(); assertNull(tenant.get());
            tenant.set("test-owner-tenant");
            var terminal = service.storyboardPlanningStatus(10L, 20L, "tenant-request", 1L);
            assertEquals("error", terminal.get("state")); assertEquals(true, terminal.get("terminal")); assertEquals(false, terminal.get("activeLock"));
            assertTrue(terminal.get("error").toString().contains("tenant-scoped failure"));
            assertTrue(namespaceMemory.values.keySet().stream().allMatch(key -> key.startsWith("test-owner-tenant:")));
        } finally { tenant.remove(); }
    }
    @Test void nativeAnonymousPlanIsValidatedAndCheckpointedWithoutImportingTheGlobalCast() throws Exception {
        script.setScriptText("外景 修渠处（预计5秒）\n乱兵群在梯架两侧停步。");
        var p = ShortDramaSceneCheckpointTest.panel(); p.setCharacters(List.of()); p.setPresentCharacters(List.of());
        p.setSourceText("乱兵群在梯架两侧停步。"); p.setBackgroundExtras(ShortDramaBackgroundExtrasTest.EXTRAS);
        var model = mock(ChatModel.class); when(model.chat(anyString())).thenReturn(new ObjectMapper().writeValueAsString(List.of(p)));
        List<ShortDramaServiceImpl.StoryboardPanelData> result = ReflectionTestUtils.invokeMethod(service, "executePhase3_StoryboardPlan", model, null, script, 10L, null, "writer", 4);
        assertNotNull(result); assertEquals(ShortDramaBackgroundExtrasTest.EXTRAS, result.get(0).getBackgroundExtras());
        assertTrue(result.get(0).getCharacters().isEmpty()); assertTrue(result.get(0).getPresentCharacters().isEmpty());
        assertEquals(1L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4).get("validatedSceneCount"));
        verify(model).chat(argThat((String prompt) -> prompt.contains("暂无本场明确登记角色") && prompt.contains("background_extras")));
    }
    @Test void normalImportApiExposesStrictSourceFailureAndValidFirstSceneDoesNotWriteBeforeARejectedSecondScene() throws Exception {
        var second = ShortDramaSceneCheckpointTest.panel(); second.setSceneNumber(2); second.setPanelNumber(2); second.setSourceText("工匠：「再看一眼。」");
        second.setBackgroundExtras(ShortDramaBackgroundExtrasTest.EXTRAS.replace("6-8", "2-3")); // No crowd exists in the source; a visible invented crowd must be rejected.
        review.setSceneNumbers(List.of(1, 2)); review.setPanels(List.of(ShortDramaSceneCheckpointTest.panel(), second));
        var rejected = assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> service.importReviewedPlan(10L, review, 1L));
        assertTrue(rejected.getMessage().contains("本场原文未安排匿名群演")); assertTrue(memory.values.isEmpty()); assertTrue(LEGACY.isEmpty());
        var controller = mock(org.ruoyi.controller.shortdrama.ShortDramaController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controller, "shortDramaService", service);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new org.ruoyi.common.web.handler.GlobalExceptionHandler()).build();
        try (var login = mockStatic(org.ruoyi.common.satoken.utils.LoginHelper.class)) {
            login.when(org.ruoyi.common.satoken.utils.LoginHelper::getUserId).thenReturn(1L);
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/short-drama/10/import-reviewed-plan")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(review)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(500))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.msg", org.hamcrest.Matchers.containsString("本场原文未安排匿名群演")));
        }
        assertTrue(memory.values.isEmpty()); assertTrue(LEGACY.isEmpty());
        assertEquals(false, service.storyboardPlanningStatus(10L, 20L, null, 1L).get("activeLock"));
        second.setBackgroundExtras(null); assertEquals(2, service.importReviewedPlan(10L, review, 1L));
        assertEquals(2L, service.storyboardCheckpointStatus(10L, 20L, "writer", 1L, 4).get("validatedSceneCount"));
    }
}
