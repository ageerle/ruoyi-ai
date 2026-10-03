package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaVideoPromptReviewTest {
    @BeforeAll static void provideProductionJsonMapper() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
    }
    static final String SOURCE = "朱承晏说：「请父皇准我查账。」";
    static final String PROMPT = """
        【参考与边界】朱承晏为年轻男子，沿用靛青丝绸定妆及御书房首帧，保持站立与纸张在右手。
        【镜头与运镜】平视中近景，桌角外机位固定，不越父子轴线。
        【时间节拍】0-2秒：右指停在回执栏，视线转向父皇；2-6秒：说原话，父皇静听，右手留在纸上，句末闭口等回应。
        【对白与口型】朱承晏在2秒说「请父皇准我查账。」低声清楚，口型同步。父皇不开口。
        【光色与材质】左前烛光暖黄，右侧窗光冷蓝，丝绸暗纹保持柔光，不换装。
        【声音】烛芯轻响与纸张接触声在室内近处，无配乐，声音不盖对白。
        【结束与承接】停在右指压住纸、目光等待父皇的姿态，下一镜从父皇接收请求开始。
        【约束】不新增人物、不镜像、不提前发现银子去处，不生成全知投影。
        """;

    @Test void acceptsNaturalDirectorProseAndStillPreservesDialogueAndSpeakers() {
        String prose = "御书房，朱承晏站在桌前，左侧烛光照亮纸面。中景固定在桌角外侧，朱承晏把文书放下，抬眼向父皇低声说「请父皇准我查账。」，父皇先看纸，再看他。环境声是纸页与轻微呼吸声，朱的右手留在纸边。";
        assertTrue(ShortDramaVideoPromptReview.issues(prose, 6, SOURCE).isEmpty());
        assertTrue(ShortDramaVideoPromptReview.issues(prose.replace("请父皇准我查账。", "先查三笔。"), 6, SOURCE)
            .stream().anyMatch(s -> s.contains("对白缺失")));
        assertTrue(ShortDramaVideoPromptReview.issues(prose.replace("朱承晏", "李四"), 6, SOURCE)
            .stream().anyMatch(s -> s.contains("发言人朱承晏")));
        assertFalse(ShortDramaVideoPromptReview.issues("中景缓推。胜利已经实现，局势得到改变。无对白。日光。", 6, "").isEmpty());
    }

    @Test void reviewsContentAndPreservesWordsWithoutMinimumLength() {
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT, 6, SOURCE).isEmpty());
        assertFalse(ShortDramaVideoPromptReview.issues("固定中景，青年查账，暖光。", 6, SOURCE).isEmpty());
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT.replace("请父皇准我查账。", "先查三笔。"), 6, SOURCE)
            .stream().anyMatch(s -> s.contains("对白缺失")));
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT, 6, "朱承晏：请父皇准我查账。").isEmpty());
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT, 6, "朱承晏：“请父皇准我查账。”").isEmpty());
        assertEquals(List.of("今天什么日子？", "甲申，三月十四。"), ShortDramaVideoPromptReview.sourceDialogue(
            "朱承晏：今天什么日子？\\n顾怀安：甲申，三月十四。"));
        assertEquals(List.of(), ShortDramaVideoPromptReview.sourceDialogue(
            "屏幕上的AI对话界面查询栏显示“明崇祯十二年山东运河粮道运力与溃兵情况”（后期叠字）。"));
        assertDoesNotThrow(() -> ShortDramaTiming.validate(1, 6,
            "屏幕查询栏显示“明崇祯十二年山东运河粮道运力与溃兵情况”（后期叠字）。", """
                {"timing":{"spoken_text":"","action_seconds":5,"pause_seconds":1,"strict_fill":true}}
                """));
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT.replace("朱承晏在2秒说", "朱廷照在2秒说"), 6, SOURCE)
            .stream().anyMatch(s -> s.contains("发言人朱承晏")));
    }

    @Test void timeGapsOverlapsEmptyBeatsAndWrongEndAreVisible() {
        for (String range : new String[]{"3-6秒", "1-6秒", "2-7秒"}) {
            assertTrue(ShortDramaVideoPromptReview.issues(PROMPT.replace("2-6秒", range), 6, SOURCE)
                .stream().anyMatch(s -> s.contains("时间节拍")));
        }
        assertTrue(ShortDramaVideoPromptReview.issues(PROMPT.replace("0-2秒：右指停在回执栏，视线转向父皇；", "0-2秒：；"), 6, SOURCE)
            .stream().anyMatch(s -> s.contains("缺少具体动作")));
    }

    @Test void acceptsSynonymsCombinedSectionsAndOneShortSilentBeat() {
        String prompt = PROMPT.replace("【参考与边界】", "【参考锚定】")
            .replace("【时间节拍】0-2秒：右指停在回执栏，视线转向父皇；2-6秒：说原话，父皇静听，右手留在纸上，句末闭口等回应。",
                "【时间轴】0—4秒：青年看向回执栏，指尖停在缺签处，呼吸后保持视线与站姿。")
            .replace("【对白与口型】朱承晏在2秒说「请父皇准我查账。」低声清楚，口型同步。父皇不开口。", "【对白与口型】本镜静默，嘴唇闭合。")
            .replace("【光色与材质】", "【光影与材质／声音】").replace("【声音】", "");
        assertTrue(ShortDramaVideoPromptReview.issues(prompt, 4, "青年静看回执。").isEmpty());
    }

    @Test void modelBatchReviewRejectsMissingDuplicateButKeepsCreativeDrafts() throws Exception {
        var panel = panel(); panel.setVideoPrompt("已有人工草稿");
        String good = new ObjectMapper().writeValueAsString(List.of(java.util.Map.of(
            "panel_number", 1, "video_prompt", PROMPT, "image_prompt", "青年站立在桌前")));
        assertEquals(1, ShortDramaServiceImpl.reviewStoryboardDetailResponse(good, List.of(panel)).size());
        assertEquals("已有人工草稿", panel.getVideoPrompt());
        assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.reviewStoryboardDetailResponse("[]", List.of(panel)));
        assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.reviewStoryboardDetailResponse(good.replace("\"panel_number\":1", "\"panel_number\":2"), List.of(panel)));
        assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.reviewStoryboardDetailResponse(
            good.substring(0, good.length() - 1) + "," + good.substring(1), List.of(panel)));
        assertDoesNotThrow(() -> ShortDramaServiceImpl.reviewStoryboardDetailResponse("[{\"panel_number\":1,\"video_prompt\":\"青年查账\",\"image_prompt\":\"站立\"}]", List.of(panel)));
        assertEquals("已有人工草稿", panel.getVideoPrompt());
    }

    @Test void invalidGeneratedPromptNeverTouchesOldStoryboardRows() throws Exception {
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        var mapper = mock(ShortDramaStoryboardMapper.class);
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        ReflectionTestUtils.setField(service, "planningSkillSnapshots", new java.util.concurrent.ConcurrentHashMap<Long, ShortDramaSkillCatalog.Snapshot>());
        var persist = ShortDramaServiceImpl.class.getDeclaredMethod("persistStoryboards", Long.class, Long.class, List.class, ShortDramaScript.class);
        persist.setAccessible(true);
        var panel = panel(); panel.setVideoPrompt("");
        var error = assertThrows(InvocationTargetException.class, () -> persist.invoke(service, 1L, 2L, List.of(panel), new ShortDramaScript()));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains("尚无视频提示词"));
        verifyNoInteractions(mapper);
    }

    @Test void generationAndKeyframeFastPathLoadSkillKeepSourceAndAvoidRejectedImageDraft() {
        assertTrue(ShortDramaServiceImpl.buildStoryboardDetailPrompt("[]", "青年", "御书房", "realistic", "16:9")
            .contains("[skill:video-prompt@"));
        String enriched = ShortDramaVideoPromptReview.anchoredPrompt(PROMPT, 6, SOURCE, "站着", "右指留纸上");
        assertTrue(enriched.contains("[skill:video-prompt@"));
        assertTrue(enriched.contains(PROMPT)); assertTrue(enriched.contains(SOURCE));
        assertTrue(enriched.contains("6秒")); assertTrue(enriched.contains("心声不驱动"));
        assertFalse(enriched.contains("@image1"));
    }

    @Test void actualKeyframeSubmissionBranchPreservesDetailedPromptAndSource() throws Exception {
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        var assets = mock(ShortDramaVisualAssetService.class);
        ReflectionTestUtils.setField(service, "visualAssets", assets);
        var projectMapper = mock(org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper.class);
        var catalog = mock(ShortDramaSkillCatalog.class);
        var project = new org.ruoyi.domain.entity.shortdrama.ShortDramaProject(); project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(catalog.selectedVisual(project, "aesthetic")).thenReturn("[selected-skill:art@v1]清楚轮廓\n");
        when(catalog.selectedVisual(project, "director")).thenReturn("[selected-skill:director@v2]连续动作\n");
        ReflectionTestUtils.setField(service, "projectMapper", projectMapper); ReflectionTestUtils.setField(service, "skillCatalog", catalog);
        when(assets.readyFrame(99L)).thenReturn("https://example.com/confirmed-frame.png");
        var shot = new org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard();
        shot.setId(99L); shot.setProjectId(7L); shot.setDurationSeconds(6); shot.setVideoPrompt(PROMPT); shot.setSourceText(SOURCE);
        shot.setContinuityJson("{\"start_state\":\"站立\",\"end_state\":\"右手留在纸上\"}");
        shot.setImagePrompt("rejected-image-draft-do-not-append");
        var method = ShortDramaServiceImpl.class.getDeclaredMethod("buildEnrichedVideoPrompt",
            org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard.class, List.class, String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, shot, List.of("https://example.com/confirmed-frame.png"), null);
        assertTrue(result.contains(PROMPT)); assertTrue(result.contains(SOURCE));
        assertTrue(result.contains("[skill:video-prompt@")); assertTrue(result.contains("6秒"));
        assertTrue(result.contains("[selected-skill:art@v1]")); assertTrue(result.contains("[selected-skill:director@v2]"));
        assertFalse(result.contains("rejected-image-draft-do-not-append"));
    }

    private static ShortDramaServiceImpl.StoryboardPanelData panel() {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData();
        panel.setPanelNumber(1); panel.setDuration(6); panel.setSourceText(SOURCE);
        return panel;
    }
    @Test void reviewedActualFirstFrameOverridesTheRejectedPlannedPoseWithoutRegeneration() {
        String prompt="【首帧实况】青年已经坐在床沿，双眼睁开。"+PROMPT;
        String actual=ShortDramaVideoPromptReview.anchoredPrompt(prompt,6,SOURCE,"原计划躺着睡觉","右指留纸上");
        assertTrue(actual.contains("【绑定起止状态】起点：青年已经坐在床沿，双眼睁开。"));
        assertFalse(actual.contains("原计划躺着睡觉"));
        assertTrue(actual.contains(PROMPT)); assertTrue(actual.contains(SOURCE));
        assertTrue(ShortDramaVideoPromptReview.anchoredPrompt(PROMPT,6,SOURCE,"默认站着","右指留纸上").contains("起点：默认站着"));
    }
}
