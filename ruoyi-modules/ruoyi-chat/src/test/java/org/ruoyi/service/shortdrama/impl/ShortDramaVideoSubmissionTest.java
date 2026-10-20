package org.ruoyi.service.shortdrama.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.chat.factory.VideoServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.video.IVideoGenerationService;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.domain.vo.shortdrama.ShortDramaStoryboardVo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaVideoRejectionBo;
import org.ruoyi.service.video.VideoSubmissionRejectedException;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.service.shortdrama.IShortDramaVideoComposeService;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaVideoSubmissionTest {
    private static final String MODEL = "fixture/text-to-video";
    private static final String ACTUAL = "fixture/image-to-video";
    @TempDir Path directory;
    private ShortDramaServiceImpl service;
    private ShortDramaVideoSubmissionStore store;
    private ShortDramaStoryboard shot;
    private ShortDramaStoryboardMapper mapper;
    private IVideoGenerationService provider;
    private IChatModelService models;

    @BeforeAll static void metadataAndConverters() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "video-test"), ShortDramaStoryboard.class);
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        var converter = mock(Converter.class);
        when(converter.convert(any(Object.class), any(Class.class))).thenAnswer(a -> {
            var target = ((Class<?>) a.getArgument(1)).getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(a.getArgument(0), target); return target;
        });
        factory.registerSingleton("converter", converter);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
    }

    @BeforeEach void fixture() {
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        store = new ShortDramaVideoSubmissionStore(directory);
        mapper = mock(ShortDramaStoryboardMapper.class);
        provider = mock(IVideoGenerationService.class);
        models = mock(IChatModelService.class);
        var projects = mock(ShortDramaProjectMapper.class);
        var videos = mock(VideoServiceFactory.class);
        var assets = mock(ShortDramaVisualAssetService.class);
        var sounds = mock(ShortDramaSoundService.class);
        var motion = mock(ShortDramaVideoReferenceService.class);
        when(motion.selected(anyLong(), anyLong(), nullable(String.class))).thenReturn(List.of());
        when(motion.transport(anyLong(), any(), anyList())).thenReturn(List.of());
        var voices = mock(ShortDramaCharacterVoiceService.class);
        when(voices.plan(any(),nullable(String.class),anyBoolean())).thenReturn(new ShortDramaCharacterVoiceService.Plan(List.of(),List.of(),"",List.of(),false,List.of()));
        var skills = mock(ShortDramaSkillCatalog.class);
        when(skills.selectedVisual(any(),anyString())).thenReturn("");
        when(skills.effectiveArtStyle(any())).thenReturn("realistic");
        var project = new ShortDramaProject(); project.setId(10L); project.setUserId(1L); project.setStatus("storyboard_ready"); project.setComposeAspectRatio("16:9");
        shot = new ShortDramaStoryboard(); shot.setId(99L); shot.setProjectId(10L); shot.setScriptId(20L);
        shot.setSceneNo(1); shot.setDurationSeconds(6); shot.setSourceText(ShortDramaVideoPromptReviewTest.SOURCE);
        shot.setVideoPrompt(ShortDramaVideoPromptReviewTest.PROMPT); shot.setVideoStatus("pending");
        shot.setContinuityJson("{\"start_state\":\"站立\",\"end_state\":\"右手留纸上\"}");
        when(mapper.selectById(99L)).thenAnswer(a -> shot);
        when(projects.selectById(10L)).thenReturn(project);
        when(assets.readyFrame(99L)).thenReturn("https://example.com/reviewed.png");
        ShortDramaAtlasTestModels.register(models, MODEL, model(MODEL), "video");
        ShortDramaAtlasTestModels.register(models, ACTUAL, model(ACTUAL), "video");
        when(videos.getOriginalService("atlas")).thenReturn(provider);
        when(sounds.references(eq(10L), eq(1), any())).thenReturn(List.of());
        when(provider.generateVideo(any())).thenReturn(MediaGenerationResponse.builder().id("prediction-1").status("processing").build());
        when(mapper.update(isNull(), any())).thenAnswer(a -> {
            @SuppressWarnings("unchecked") var update = (LambdaUpdateWrapper<ShortDramaStoryboard>) a.getArgument(1);
            var assignments = Pattern.compile("([a-z_]+)=#\\{ew\\.paramNameValuePairs\\.(MPGENVAL\\d+)\\}").matcher(update.getSqlSet());
            while (assignments.find()) {
                // DB snake-case -> Java camel-case without requiring a live database.
                String[] parts = assignments.group(1).split("_"); var property = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) property.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
                ReflectionTestUtils.setField(shot, property.toString(), update.getParamNameValuePairs().get(assignments.group(2)));
            }
            return 1;
        });
        fields(Map.of("storyboardMapper", mapper, "projectMapper", projects, "videoServiceFactory", videos,
            "visualAssets", assets, "sounds", sounds, "chatModelService", models, "videoSubmissions", store,
            "storyboardGenerationStates", new ConcurrentHashMap<>(), "videoComposeService", mock(IShortDramaVideoComposeService.class), "skillCatalog",skills));
        ReflectionTestUtils.setField(service,"motionReferences",motion);
        ReflectionTestUtils.setField(service,"characterVoices",voices);
    }

    private void fields(Map<String, Object> fields) { fields.forEach((name, value) -> ReflectionTestUtils.setField(service, name, value)); }
    private static ChatModelVo model(String name) { var model = new ChatModelVo(); model.setModelName(name); model.setProviderCode("fixture"); return model; }
    private String request() { return UUID.randomUUID().toString(); }
    private void atlasProvider() {
        var videos = (VideoServiceFactory) ReflectionTestUtils.getField(service, "videoServiceFactory");
        var requested = model(MODEL); requested.setProviderCode("atlas");
        var actual = model(ACTUAL); actual.setProviderCode("atlas");
        ShortDramaAtlasTestModels.register(models, MODEL, requested, "video");
        ShortDramaAtlasTestModels.register(models, ACTUAL, actual, "video");
        when(videos.getOriginalService("atlas")).thenReturn(provider);
    }
    private static final String BALANCE_REJECTION = "{\"code\":402,\"msg\":\"insufficient balance\"}";

    @Test void explicitBalanceRejectionReleasesOnlyLocalClaimAndKeepsPreviousPaidFile() {
        atlasProvider();
        shot.setVideoStatus("done"); shot.setVideoId("old-id"); shot.setVideoUrl("https://example.com/approved.mp4"); shot.setLastFrameUrl("old-frame");
        when(provider.generateVideo(any())).thenThrow(VideoSubmissionRejectedException.atlasResponse(402, BALANCE_REJECTION));
        String id = request(); var result = generate(id, true);
        assertEquals("failed", result.getVideoStatus()); assertNull(result.getVideoId());
        assertEquals("https://example.com/approved.mp4", result.getVideoUrl()); assertEquals("old-frame", result.getLastFrameUrl());
        var receipt = store.read(99L, id);
        assertNull(receipt.predictionId()); assertEquals("failed", receipt.status());
        assertTrue(receipt.error().contains("HTTP 402")); assertEquals("old-id", receipt.previousVideo().videoId());
        generate(id, true); verify(provider, times(1)).generateVideo(any());
        doReturn(MediaGenerationResponse.builder().id("new-id").status("processing").build()).when(provider).generateVideo(any());
        generate(request(), false); verify(provider, times(2)).generateVideo(any());
        assertEquals("new-id", shot.getVideoId());
    }

    @Test void uncertainResponseCannotBeReclassifiedAsBalanceRejection() {
        assertNull(VideoSubmissionRejectedException.atlasResponse(504, BALANCE_REJECTION));
        assertNull(VideoSubmissionRejectedException.atlasResponse(500, BALANCE_REJECTION));
        assertNull(VideoSubmissionRejectedException.atlasResponse(402, "{\"code\":402,\"msg\":\"insufficient balance\",\"data\":{\"id\":\"accepted\"}}"));
        assertNull(VideoSubmissionRejectedException.atlasResponse(402, "not json"));
        assertNull(VideoSubmissionRejectedException.atlasResponse(402, "{\"code\":400,\"msg\":\"insufficient balance\"}"));
        when(provider.generateVideo(any())).thenThrow(new IllegalArgumentException("HTTP 504 with insufficient balance in upstream text"));
        String id = request(); generate(id, false);
        assertEquals("submission_unknown", store.read(99L, id).status());
        generate(request(), true); verify(provider, times(1)).generateVideo(any());
    }

    @Test void legacyBalanceRecoveryRequiresOwnerAndExactClaimAndDoesNotCreatePredictionOrPayment() {
        atlasProvider();
        when(provider.generateVideo(any())).thenThrow(new IllegalArgumentException("Atlas Cloud视频生成任务创建失败: 402 - " + BALANCE_REJECTION));
        String id = request(); generate(id, false);
        var evidence = new ShortDramaVideoRejectionBo(402, BALANCE_REJECTION, "Runtime log verified requestId=" + id + ": HTTP 402, no prediction ID");
        assertThrows(IllegalArgumentException.class, () -> service.confirmVideoSubmissionRejection(99L, id, 2L, evidence));
        assertThrows(IllegalArgumentException.class, () -> service.confirmVideoSubmissionRejection(99L, id, 1L,
            new ShortDramaVideoRejectionBo(402, BALANCE_REJECTION, "Runtime log refers to another request " + request())));
        assertThrows(IllegalArgumentException.class, () -> service.confirmVideoSubmissionRejection(99L, id, 1L,
            new ShortDramaVideoRejectionBo(504, BALANCE_REJECTION, evidence.evidence())));
        assertEquals("submission_unknown", shot.getVideoStatus());
        var recovered = service.confirmVideoSubmissionRejection(99L, id, 1L, evidence);
        assertEquals("failed", recovered.getVideoStatus()); assertNull(recovered.getVideoId());
        assertNull(store.read(99L, id).predictionId()); assertTrue(store.read(99L, id).error().contains(id));
        service.confirmVideoSubmissionRejection(99L, id, 1L, evidence);
        verify(provider, times(1)).generateVideo(any()); verify(provider, never()).retrieveVideo(any());
    }

    @Test void legacyBalanceEvidenceCannotOverwriteAnAcceptedPrediction() {
        atlasProvider(); String id = request(); generate(id, false);
        var evidence = new ShortDramaVideoRejectionBo(402, BALANCE_REJECTION, "Runtime log verified requestId=" + id + ": HTTP 402");
        assertThrows(IllegalStateException.class, () -> service.confirmVideoSubmissionRejection(99L, id, 1L, evidence));
        assertEquals("prediction-1", shot.getVideoId()); assertEquals("generating", store.read(99L, id).status());
        verify(provider, times(1)).generateVideo(any());
    }

    @Test void savedModelChoiceBlocksAnotherModelBeforeAnyPaidSubmission() {
        shot.setContinuityJson("{\"video_model\":\"" + MODEL + "\",\"video_seconds\":-1,\"video_resolution\":\"720p\"}");
        String wrongRequest = request();
        assertThrows(IllegalArgumentException.class, () -> service.generateVideo(99L, "other/video-model", 1L, wrongRequest, false));
        verifyNoInteractions(provider, models); assertNull(store.read(99L, wrongRequest));
        generate(request(), false);
        var capture = org.mockito.ArgumentCaptor.forClass(VideoContext.class); verify(provider).generateVideo(capture.capture());
        assertEquals(-1, capture.getValue().getSeconds()); assertEquals("720p", capture.getValue().getResolution());
        assertEquals(ACTUAL, capture.getValue().getChatModelVo().getModelName());
    }
    @Test void boundVoiceIsAttachedWithStableIdentityOnSingleRegenerateAndRetry() throws Exception {
        String referenceModel="bytedance/seedance-2.0-mini/reference-to-video";
        String requestedModel="bytedance/seedance-2.0-mini/text-to-video";
        var voices=(ShortDramaCharacterVoiceService)ReflectionTestUtils.getField(service,"characterVoices");
        var sounds=(ShortDramaSoundService)ReflectionTestUtils.getField(service,"sounds");
        var sample=new ShortDramaSoundService.Reference("voice:42:sample","阿青",directory.resolve("sample.mp3"),3.0,"immutable-hash");
        var binding=new ShortDramaCharacterVoiceService.Binding("42","阿青","sample",1,3.0,1);
        when(voices.plan(any(),nullable(String.class),anyBoolean())).thenReturn(new ShortDramaCharacterVoiceService.Plan(List.of(binding),List.of(sample),"\n阿青使用 @音频1 的音色",List.of(),true,List.of("42")));
        ShortDramaAtlasTestModels.register(models, referenceModel, model(referenceModel), "video");
        ShortDramaAtlasTestModels.register(models, requestedModel, model(requestedModel), "video");
        when(sounds.uploadReferences(anyList(),any())).thenReturn(List.of("https://example.com/voice-a.mp3"),List.of("https://example.com/voice-b.mp3"));
        String first=request();service.generateVideo(99L,requestedModel,1L,first,false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);verify(provider).generateVideo(capture.capture());
        assertEquals(referenceModel,capture.getValue().getChatModelVo().getModelName());
        assertEquals(List.of("https://example.com/voice-a.mp3"),capture.getValue().getReferenceAudios());
        assertTrue(capture.getValue().getPrompt().contains("阿青使用 @音频1"));assertNull(capture.getValue().getSeconds());
        service.generateVideo(99L,requestedModel,1L,first,false);verify(provider,times(1)).generateVideo(any());verify(sounds,times(1)).uploadReferences(anyList(),any());
        shot.setVideoStatus("done");shot.setVideoUrl("https://example.com/previous.mp4");
        service.generateVideo(99L,requestedModel,1L,request(),true);verify(provider,times(2)).generateVideo(any());
        assertTrue(java.nio.file.Files.exists(directory.resolve("99/voices/"+first+".json")));
    }
    private ShortDramaStoryboardVo generate(String request, boolean regenerate) { return service.generateVideo(99L, MODEL, 1L, request, regenerate); }

    @Test void motionReferenceIsSentOnceAndContentChangesCannotReuseThePaidRequest() {
        String referenceModel="bytedance/seedance-2.0-mini/reference-to-video";
        ShortDramaAtlasTestModels.register(models, referenceModel, model(referenceModel), "video");
        var motion=(ShortDramaVideoReferenceService)ReflectionTestUtils.getField(service,"motionReferences");
        var clip=new ShortDramaVideoReferenceService.Clip("00000000-0000-0000-0000-000000000001","白模.mp4",12,1280,720,24,1000,"immutable-hash");
        when(motion.selected(anyLong(),anyLong(),nullable(String.class))).thenReturn(List.of(clip));
        when(motion.transport(anyLong(),any(),anyList())).thenReturn(List.of("https://example.com/motion.mp4"));
        String first=request();service.generateVideo(99L,referenceModel,1L,first,false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);verify(provider).generateVideo(capture.capture());
        assertEquals(List.of("https://example.com/motion.mp4"),capture.getValue().getReferenceVideos());
        assertTrue(capture.getValue().getPrompt().contains("@video1"));
        service.generateVideo(99L,referenceModel,1L,first,false);
        verify(provider,times(1)).generateVideo(any());verify(motion,times(1)).transport(anyLong(),any(),anyList());
        var changed=new ShortDramaVideoReferenceService.Clip(clip.id(),clip.name(),clip.duration(),1280,720,24,1000,"changed-hash");
        when(motion.selected(anyLong(),anyLong(),nullable(String.class))).thenReturn(List.of(changed));
        assertThrows(IllegalArgumentException.class,()->service.generateVideo(99L,referenceModel,1L,first,false));
        verify(provider,times(1)).generateVideo(any());
    }

    @Test void repeatedRequestAndAnotherUuidNeverSubmitAnAcceptedShotAgain() {
        String id = request(); generate(id, false); generate(id, false); generate(request(), false);
        verify(provider, times(1)).generateVideo(any());
        assertEquals("prediction-1", store.read(99L, id).predictionId());
        assertEquals(ACTUAL, store.read(99L, id).actualModel());
    }

    @Test void generatedPlanningDurationIsOmittedFromPaidContext() {
        shot.setDurationSeconds(1); // Deliberately shorter than speech estimates: never a provider limit.
        generate(request(), false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);
        verify(provider).generateVideo(capture.capture());
        assertNull(capture.getValue().getSeconds());
        assertFalse(capture.getValue().getPrompt().contains("视频秒数"));
        assertFalse(capture.getValue().getPrompt().contains("【目标时长】1秒"));
    }

    @Test void reviewedFrameCanBeExplicitlyOmittedWithoutDeletingIt() {
        shot.setContinuityJson("{\"start_state\":\"站立\",\"end_state\":\"右手留纸上\",\"video_use_start_frame\":false}");
        generate(request(), false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);
        verify(provider).generateVideo(capture.capture());
        assertNull(capture.getValue().getReferenceImages());
        assertEquals(MODEL, capture.getValue().getChatModelVo().getModelName());
        verify((ShortDramaVisualAssetService) ReflectionTestUtils.getField(service, "visualAssets"), never()).readyFrame(anyLong());
    }

    @Test void noFrameDoesNotPreventVideoGeneration() {
        var assets=(ShortDramaVisualAssetService) ReflectionTestUtils.getField(service,"visualAssets");
        when(assets.readyFrame(99L)).thenReturn(null);
        generate(request(),false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);
        verify(provider).generateVideo(capture.capture());
        assertNull(capture.getValue().getReferenceImages());
    }

    @Test void explicitSecondsArePassedAndClearingRestoresProviderDefault() {
        shot.setContinuityJson("{\"start_state\":\"站立\",\"end_state\":\"右手留纸上\",\"video_seconds\":6}");
        generate(request(), false);
        var capture=org.mockito.ArgumentCaptor.forClass(VideoContext.class);
        verify(provider).generateVideo(capture.capture());
        assertEquals(6,capture.getValue().getSeconds());
        assertFalse(capture.getValue().getPrompt().contains("【用户指定视频时长】"));
        shot.setVideoStatus("done");shot.setVideoUrl("https://example.com/approved.mp4");
        shot.setContinuityJson("{\"start_state\":\"站立\",\"end_state\":\"右手留纸上\"}");
        generate(request(),true);
        verify(provider,times(2)).generateVideo(capture.capture());
        assertNull(capture.getValue().getSeconds());
    }

    @Test void concurrentDifferentRequestsSubmitExactlyOnce() throws Exception {
        var start = new CountDownLatch(1); var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { start.await(); return generate(request(), false); });
            var second = executor.submit(() -> { start.await(); return generate(request(), false); });
            start.countDown(); first.get(5, TimeUnit.SECONDS); second.get(5, TimeUnit.SECONDS);
            verify(provider, times(1)).generateVideo(any());
        } finally { executor.shutdownNow(); }
    }

    @Test void sameUuidClaimAlsoSurvivesANewStoreInstance() {
        String id = request(); generate(id, false);
        var receipt = store.read(99L, id);
        assertFalse(new ShortDramaVideoSubmissionStore(directory).claim(receipt));
        assertEquals(receipt, new ShortDramaVideoSubmissionStore(directory).read(99L, id));
        verify(provider, times(1)).generateVideo(any());
    }

    @Test void databaseCasLoserCannotReachTheProvider() {
        doReturn(0).when(mapper).update(isNull(), any());
        String id = request(); generate(id, false); generate(id, false);
        verifyNoInteractions(provider); assertEquals("not_submitted", store.read(99L, id).status());
    }

    @Test void unknownSubmissionRetainsLocalClaimAndNeverBlindlyRetries() {
        when(provider.generateVideo(any())).thenThrow(new IllegalStateException("network timeout"));
        String id = request(); var result = generate(id, false);
        assertEquals("submission_unknown", result.getVideoStatus()); assertEquals("local:" + id, result.getVideoId());
        generate(id, false); generate(request(), true);
        verify(provider, times(1)).generateVideo(any());
        assertEquals("submission_unknown", new ShortDramaVideoSubmissionStore(directory).read(99L, id).status());
    }

    @Test void historyRecoveryConfirmsFailureBeforeANewUuidCanRetry() {
        when(provider.generateVideo(any())).thenThrow(new IllegalStateException("gateway timeout"));
        String first = request(); generate(first, false);
        when(provider.retrieveVideo(any())).thenReturn(MediaGenerationResponse.builder().id("history-id").status("failed")
            .rawResponse("{\"data\":{\"id\":\"history-id\",\"status\":\"failed\"}}").build());
        var recovered = service.resolveVideoSubmission(99L, first, "history-id", 1L);
        assertEquals("failed", recovered.getVideoStatus()); assertEquals("history-id", recovered.getVideoId());
        assertEquals("history-id", store.read(99L, first).predictionId());
        doReturn(MediaGenerationResponse.builder().id("retry-id").status("processing").build()).when(provider).generateVideo(any());
        generate(request(), false);
        verify(provider, times(2)).generateVideo(any());
        assertEquals("failed", store.read(99L, first).status());
    }

    @Test void unconfirmedHistoryIdKeepsUnknownClaimAndDoesNotPayAgain() {
        when(provider.generateVideo(any())).thenThrow(new IllegalStateException("gateway timeout"));
        String first = request(); generate(first, false);
        when(provider.retrieveVideo(any())).thenReturn(MediaGenerationResponse.builder().id("history-id").status("failed")
            .rawResponse("{\"error\":\"not found\"}").build());
        assertThrows(IllegalStateException.class, () -> service.resolveVideoSubmission(99L, first, "history-id", 1L));
        assertEquals("local:" + first, shot.getVideoId()); assertEquals("submission_unknown", store.read(99L, first).status());
        generate(request(), true); verify(provider, times(1)).generateVideo(any());
        assertThrows(IllegalArgumentException.class, () -> service.resolveVideoSubmission(99L, first, "../invalid", 1L));
    }

    @Test void ownerConfirmedHistoryFailureCanResolveAQueryTimeoutButIsNeverAnAutomaticFallback() {
        when(provider.generateVideo(any())).thenThrow(new IllegalStateException("submit timeout"));
        String first = request(); generate(first, false);
        when(provider.retrieveVideo(any())).thenThrow(new IllegalStateException("query timeout"));
        assertThrows(IllegalStateException.class, () -> service.resolveVideoSubmission(99L, first, "history-id", 1L));
        assertEquals("submission_unknown", store.read(99L, first).status());
        var resolved = service.resolveVideoSubmission(99L, first, "history-id", 1L,
            "Owner checked provider history history-id: failed, matching prompt and assets; retry authorized");
        assertEquals("failed", resolved.getVideoStatus()); assertEquals("history-id", resolved.getVideoId());
        assertTrue(store.read(99L, first).error().contains("Owner checked"));
        verify(provider, times(1)).generateVideo(any());
    }

    @Test void uploadedReferenceChangesOnlyTransportAndParametersFingerprint() {
        shot.setContinuityJson("{\"video_reference_mirrors\":{\"https://example.com/reviewed.png\":\"https://provider.test/upload.png\"}}");
        generate(request(), false);
        var capture = org.mockito.ArgumentCaptor.forClass(VideoContext.class); verify(provider).generateVideo(capture.capture());
        assertEquals(List.of("https://provider.test/upload.png"), capture.getValue().getReferenceImages());
        verify((ShortDramaVisualAssetService) ReflectionTestUtils.getField(service, "visualAssets"), atLeastOnce()).readyFrame(99L);
    }

    @Test void videoReferenceUploadUsesTheSelectedModelsLimitWithoutRaisingImageLimits() throws Exception {
        String mini = "bytedance/seedance-2.0-mini/reference-to-video";
        ShortDramaAtlasTestModels.register(models, mini, model(mini), "video");
        var images = mock(org.ruoyi.common.chat.factory.ImageServiceFactory.class);
        var imageProvider = mock(org.ruoyi.common.chat.service.image.IImageGenerationService.class);
        ReflectionTestUtils.setField(service, "imageServiceFactory", images);
        when(images.getOriginalService("atlas")).thenReturn(imageProvider);
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getSize()).thenReturn(12L * 1024 * 1024); when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("original.png"); when(file.getBytes()).thenReturn(new byte[]{1,2,3});
        when(imageProvider.uploadMedia(any(), any(), anyString(), anyString())).thenReturn("https://provider.test/copy.png");
        assertEquals("https://provider.test/copy.png", service.uploadReferenceImage(file, mini, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.uploadReferenceImage(file, MODEL, 1L));
        when(file.getSize()).thenReturn(30L * 1024 * 1024);
        assertThrows(IllegalArgumentException.class, () -> service.uploadReferenceImage(file, mini, 1L));
        verify(imageProvider, times(1)).uploadMedia(any(), any(), anyString(), anyString());
    }

    @Test void doneIsReturnedWithoutEvenResolvingNewModelOrReviewingAnOldDraft() {
        shot.setVideoStatus("done"); shot.setVideoUrl("https://example.com/existing.mp4"); shot.setVideoPrompt("旧短稿");
        assertEquals(shot.getVideoUrl(), generate(request(), false).getVideoUrl());
        verifyNoInteractions(provider, models);
    }

    @Test void doneWithoutAFileQueriesTheExistingPredictionRatherThanPayingAgain() {
        shot.setVideoStatus("done"); shot.setVideoId("old-id");
        when(provider.retrieveVideo(any())).thenReturn(MediaGenerationResponse.builder().id("old-id").status("completed").build());
        var result = generate(request(), false);
        assertEquals("old-id", result.getVideoId()); assertEquals("submission_unknown", result.getVideoStatus());
        verify(provider).retrieveVideo(any()); verify(provider, never()).generateVideo(any());
    }

    @Test void completedUpstreamWithoutOutputRetainsItsPredictionForRecovery() {
        String id = request(); generate(id, false);
        when(provider.retrieveVideo(any())).thenReturn(MediaGenerationResponse.builder().status("completed").build());
        var result = service.retrieveVideo(99L, MODEL, 1L);
        assertEquals("submission_unknown", result.getVideoStatus()); assertEquals("prediction-1", result.getVideoId());
        assertEquals("prediction-1", store.read(99L, id).predictionId());
        generate(request(), true); verify(provider, times(1)).generateVideo(any());
    }

    @Test void sameRequestWithDifferentParametersIsRejected() {
        String id = request(); generate(id, false); shot.setVideoPrompt(shot.getVideoPrompt() + "\n【补充】保留衣纹。");
        var error = assertThrows(IllegalArgumentException.class, () -> generate(id, false));
        assertTrue(error.getMessage().contains("不同视频参数")); verify(provider, times(1)).generateVideo(any());
    }

    @Test void synchronousUrlStillSavesTheActualPredictionId() {
        when(provider.generateVideo(any())).thenReturn(MediaGenerationResponse.builder().id("sync-id").url("https://example.com/sync.mp4").lastFrameUrl("frame").build());
        String id = request(); var result = generate(id, false);
        assertEquals("done", result.getVideoStatus()); assertEquals("sync-id", result.getVideoId());
        assertEquals("sync-id", store.read(99L, id).predictionId()); assertEquals(result.getVideoUrl(), store.read(99L, id).videoUrl());
    }

    @Test void queryNetworkFailureNeverLosesIdAndUsesRecordedSwitchedModel() {
        String id = request(); generate(id, false);
        when(provider.retrieveVideo(any())).thenThrow(new IllegalStateException("query timeout"));
        var result = service.retrieveVideo(99L, "wrong-client-model", 1L);
        assertEquals("prediction-1", result.getVideoId()); assertEquals("generating", result.getVideoStatus());
        var context = org.mockito.ArgumentCaptor.forClass(VideoContext.class); verify(provider).retrieveVideo(context.capture());
        assertEquals(ACTUAL, context.getValue().getChatModelVo().getModelName());
        assertEquals("prediction-1", store.read(99L, id).predictionId());
    }

    @Test void explicitRegenerationKeepsOldSnapshotAndFileUntilAcceptance() {
        shot.setVideoStatus("done"); shot.setVideoId("old-id"); shot.setVideoUrl("https://example.com/old.mp4"); shot.setLastFrameUrl("old-frame");
        when(provider.generateVideo(any())).thenAnswer(a -> {
            assertEquals("https://example.com/old.mp4", shot.getVideoUrl()); assertEquals("old-frame", shot.getLastFrameUrl());
            return MediaGenerationResponse.builder().id("new-id").status("processing").build();
        });
        String id = request(); generate(id, true);
        var receipt = store.read(99L, id); assertEquals("old-id", receipt.previousVideo().videoId());
        assertEquals("https://example.com/old.mp4", receipt.previousVideo().videoUrl());
        assertEquals("new-id", shot.getVideoId()); assertNull(shot.getVideoUrl()); assertNull(shot.getLastFrameUrl());
        assertThrows(IllegalArgumentException.class, () -> service.generateVideo(99L, MODEL, 1L, null, true));
    }

    @Test void emptyPromptCannotTriggerAPaidSubmissionOrEraseAnApprovedFrame() {
        shot.setVideoPrompt(" ");
        assertThrows(IllegalStateException.class, () -> generate(request(), false));
        verifyNoInteractions(provider); assertEquals("pending", shot.getVideoStatus());
        assertEquals("{\"start_state\":\"站立\",\"end_state\":\"右手留纸上\"}", shot.getContinuityJson());
    }

    @Test void unknownStopsAContinuousBatchBeforeTheNextShot() {
        var unknown = new ShortDramaStoryboardVo(); unknown.setVideoStatus("submission_unknown"); unknown.setVideoId("local:" + request());
        doReturn(unknown).when(service).generateVideo(99L, MODEL, 1L, null);
        var next = new ShortDramaStoryboard(); next.setId(100L); next.setSceneNo(2);
        assertThrows(IllegalStateException.class, () -> service.generateGroupSerial(List.of(shot, next), MODEL, 1L));
        verify(service, never()).generateVideo(eq(100L), anyString(), anyLong(), nullable(String.class));
    }

    @Test void rangeUsesActualSceneNumberAndExistingRowCount() {
        var next = new ShortDramaStoryboard(); next.setId(100L); next.setSceneNo(3);
        assertEquals(List.of(next), ShortDramaServiceImpl.selectVideoRange(List.of(shot, next), 2, 1));
        assertEquals(List.of(shot), ShortDramaServiceImpl.selectVideoRange(List.of(shot, next), 1, 1));
    }

    @Test void rangedBatchCarriesTheCompletedPredecessorFrameWithoutSubmittingIt() {
        var next = new ShortDramaStoryboard(); BeanUtils.copyProperties(shot, next); next.setId(100L); next.setSceneNo(2);
        when(mapper.selectList(any())).thenReturn(List.of(shot, next));
        var prior = new ShortDramaStoryboardVo(); prior.setVideoStatus("done"); prior.setLastFrameUrl("prior-last-frame");
        doReturn(prior).when(service).retrieveVideo(99L, MODEL, 1L);
        var complete = new ShortDramaStoryboardVo(); complete.setSceneNo(2); complete.setVideoStatus("done");
        doReturn(List.of(complete)).when(service).generateGroupSerial(List.of(next), MODEL, 1L, "prior-last-frame");
        assertEquals(List.of(complete), service.generateAllVideos(10L, MODEL, 1L, 2, 1));
        verify(service).generateGroupSerial(List.of(next), MODEL, 1L, "prior-last-frame");
        verifyNoInteractions(provider);
    }

    @Test void incompletePredecessorStopsRangedBatchBeforeAnyPayment() {
        var next = new ShortDramaStoryboard(); BeanUtils.copyProperties(shot, next); next.setId(100L); next.setSceneNo(2);
        when(mapper.selectList(any())).thenReturn(List.of(shot, next));
        var error = assertThrows(IllegalStateException.class, () -> service.generateAllVideos(10L, MODEL, 1L, 2, 1));
        assertTrue(error.getMessage().contains("先完成上一镜")); verifyNoInteractions(provider);
    }
}
