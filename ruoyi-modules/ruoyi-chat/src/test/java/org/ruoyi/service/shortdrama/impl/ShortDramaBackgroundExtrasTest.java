package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaBackgroundExtrasTest {
    static final String EXTRAS = "0秒：可见人数=6-8；身份服饰=匿名乱兵，粗布军装、不同脸型；位置=城墙下梯架两侧；姿态动作=躬身贴梯、双手扶梯，尚未登上城头";
    private static final ObjectMapper JSON = new ObjectMapper();
    @BeforeAll static void jsonBean() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
    }
    @Test void sourceArmyCanRemainAnonymousWithoutCreatingOrBorrowingARegisteredLeader() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        assets.characters().get(0).setName("乱兵头目");
        var p = anonymousPanel();
        assertDoesNotThrow(() -> assets.referenced(List.of(p)));
        assertDoesNotThrow(() -> assets.validateSceneCast("外景 修渠处\n乱兵群举梯停在墙根。", "乱兵头目在上一场", List.of(p)));
        assertEquals(6, ShortDramaBackgroundExtras.parse(EXTRAS).minimum());
        assertEquals(8, ShortDramaBackgroundExtras.parse(EXTRAS).maximum());
        p.setPresentCharacters(List.of("爬梯乱兵群像"));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(p)));
        p.setPresentCharacters(List.of("乱兵头目"));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("乱兵群举梯停在墙根", "乱兵头目在上一场", List.of(p)));
    }
    @Test void occupationAliasesPermitAnonymousPeersOfRegisteredForegroundRepresentatives() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        var foreground = assets.characters().get(0); foreground.setName("前景差役"); foreground.setAliases("差役");
        var young = character(11L, "年轻乡勇", "乡勇"); var student = character(12L, "徒弟", "徒弟");
        assets.characters().addAll(List.of(young, student));
        var p = ShortDramaSceneCheckpointTest.panel(); p.getCharacters().get(0).setName("前景差役"); p.setPresentCharacters(List.of("前景差役"));
        p.setBackgroundExtras("0秒：可见人数=5；身份服饰=匿名差役、乡勇和徒弟，粗布衣、彼此不同脸型；位置=前景代表身后；姿态动作=站立等候，双手空置");
        assertDoesNotThrow(() -> assets.validateSceneCast("前景差役带差役们守门；年轻乡勇身后另有乡勇们，徒弟们在后侧等候。", "", List.of(p)));
        assertTrue(ShortDramaBackgroundExtras.genericRole("前景差役"));
        assertTrue(ShortDramaBackgroundExtras.genericRole("乡勇"));
        assertTrue(ShortDramaBackgroundExtras.genericRole("徒弟"));
        assertFalse(ShortDramaBackgroundExtras.genericRole("朱承晏"));
        assertFalse(ShortDramaBackgroundExtras.genericRole("Codex"));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("前景差役独自在门口站立。", "", List.of(p)));
    }
    @Test void anonymousFieldCannotConcealProperNamesUniqueAliasesOrCG() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        assets.characters().addAll(List.of(character(11L, "朱承晏", "承晏"), character(12L, "陈铁牛", "铁牛"),
            character(13L, "Codex", "青玉色半透明立方体")));
        for (String hidden : List.of("朱承晏", "承晏", "陈铁牛", "铁牛", "Codex", "青玉色半透明立方体")) {
            var p = anonymousPanel(); p.setBackgroundExtras(EXTRAS.replace("匿名乱兵", "匿名乱兵和" + hidden));
            assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
                () -> assets.validateSceneCast("乱兵群在墙根停步", "", List.of(p)), hidden);
        }
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("工匠独自核账：「以前这里有乱兵群。」", "", List.of(anonymousPanel())));
    }
    @Test void countRangeAndDescriptionAreExplicitAndIdsNeverCreateAnonymousAssets() {
        for (String malformed : List.of("十名乱兵", EXTRAS.replace("6-8", "8-6"), EXTRAS.replace("位置=城墙下梯架两侧", "位置="),
            EXTRAS.replace("匿名", "登记"), EXTRAS + ";character_id=1", EXTRAS + ";参考@image1", EXTRAS + "甲".repeat(4001)))
            assertThrows(IllegalArgumentException.class, () -> ShortDramaBackgroundExtras.parse(malformed), malformed);
        assertFalse(ShortDramaBackgroundExtras.parse(null).visible());
        assertFalse(ShortDramaBackgroundExtras.parse("").visible());
        var malformedObject = JSON.createObjectNode(); malformedObject.putArray("background_extras");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaBackgroundExtras.readText(malformedObject));
    }
    @Test void explicitZeroExtrasNeedNeitherCostumesNorSourceCrowds() {
        var p = ShortDramaSceneCheckpointTest.panel();
        p.setBackgroundExtras("0秒：可见人数=0；无匿名群演");
        var parsed = ShortDramaBackgroundExtras.parse(p.getBackgroundExtras());
        assertFalse(parsed.visible()); assertEquals(0, parsed.minimum());
        assertDoesNotThrow(() -> ShortDramaBackgroundExtras.validateScene("工匠独自在桌边核账。", List.of(), List.of(p)));
        assertTrue(ShortDramaBackgroundExtras.frameGuidance(p.getBackgroundExtras()).contains("可见人数=0"));
        p.setBackgroundExtras("0秒：可见人数=0-1；无匿名群演");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaBackgroundExtras.parse(p.getBackgroundExtras()));
    }
    @Test void zeroExtrasCannotSmuggleAssetReferences() {
        for (String reference : List.of("character_id=1", "参考@image1"))
            assertThrows(IllegalArgumentException.class,
                () -> ShortDramaBackgroundExtras.parse("0秒：可见人数=0；无匿名群演；" + reference));
    }
    @Test void serializedContinuityAndValidatedCheckpointKeepExtrasWithoutChangingExistingProof() throws Exception {
        var p = anonymousPanel(); p.setStartState("匿名乱兵仍在梯下，尚未登顶"); p.setEndState("梯身立稳");
        String continuity = ReflectionTestUtils.invokeMethod(ShortDramaServiceImpl.class, "buildContinuityJson", p);
        assertNotNull(continuity); assertEquals(EXTRAS, JSON.readTree(continuity).path("background_extras").asText());
        var restored = new StoryboardPanelData();
        ReflectionTestUtils.invokeMethod(ShortDramaServiceImpl.class, "applyContinuityJson", restored, continuity);
        assertEquals(EXTRAS, restored.getBackgroundExtras()); assertEquals(p.getStartState(), restored.getStartState());
        var roundtrip = JSON.readValue(JSON.writeValueAsString(p), StoryboardPanelData.class);
        assertEquals(EXTRAS, roundtrip.getBackgroundExtras());
        var store = new ShortDramaSceneCheckpoint(new ShortDramaSceneCheckpointTest.Memory());
        var assets = ShortDramaSceneCheckpointTest.assets(); var id = ShortDramaSceneCheckpointTest.identity("乱兵群停在墙根");
        store.save(id, assets, List.of(p), true, "native_validated");
        assertEquals(EXTRAS, store.compatible(id, assets, true).panels().get(0).getBackgroundExtras());
        var oldId = ShortDramaSceneCheckpointTest.identity("工匠核账");
        store.save(oldId, assets, List.of(ShortDramaSceneCheckpointTest.panel()), true, "native_validated");
        assertNotNull(store.compatible(oldId, assets, true)); // Existing receipts have no extras and need no skill/version bump.
    }
    @Test void zeroNamedCrowdFrameAndOneNamedCrowdFrameDoNotBecomeEmptyOrSoloFrames() throws Exception {
        var shot = shot(List.of(), EXTRAS); String prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains(EXTRAS)); assertFalse(prompt.contains("0秒画面无人")); assertFalse(prompt.contains("严格只有1人"));
        shot = shot(List.of("工匠"), EXTRAS); shot.setShotType("人物近景");
        prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("唯一具名/登记主体：工匠")); assertTrue(prompt.contains("已明确的0秒匿名群演人数"));
        assertFalse(prompt.contains("严格只有1人"));
        assertTrue(prompt.contains("尚未登上城头")); assertTrue(prompt.contains("不提前执行"));
    }
    @Test void noExtrasEmptyAndSoloFramesAndFutureEntryRemainProtected() throws Exception {
        var shot = shot(List.of(), null);
        assertTrue(ShortDramaVisualAssetService.framePrompt(shot).contains("0秒画面无人"));
        shot = shot(List.of("工匠"), null);
        assertTrue(ShortDramaVisualAssetService.framePrompt(shot).contains("严格只有1人：工匠"));
        shot = shot(List.of("工匠"), EXTRAS.replace("6-8", "0").replace("尚未登上城头", "将在第3秒进入门口"));
        String prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("严格只有1人：工匠")); assertTrue(prompt.contains("0秒匿名群演可见人数=0"));
        assertFalse(prompt.contains("将在第3秒进入门口"));
    }
    @Test void namedCloseUpVideoIdentityRuleRespectsExplicitAnonymousExtras() throws Exception {
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS); var ref = new CharacterRef(); ref.setName("工匠");
        var shot = shot(List.of("工匠"), EXTRAS); shot.setShotType("人物近景"); var sb = new StringBuilder();
        ReflectionTestUtils.invokeMethod(service, "appendCharacterIdentityIsolationPrompt", sb, shot, List.of(ref), List.of("https://example.test/face.png"));
        assertTrue(sb.toString().contains("0秒匿名群演必须按background_extras保留")); assertFalse(sb.toString().contains("只允许一张"));
        shot = shot(List.of("工匠"), null); shot.setShotType("人物近景"); sb = new StringBuilder();
        ReflectionTestUtils.invokeMethod(service, "appendCharacterIdentityIsolationPrompt", sb, shot, List.of(ref), List.of("https://example.test/face.png"));
        assertTrue(sb.toString().contains("只允许一张可辨认人脸"));
    }
    @Test void eachPanelBindsOneRealLocationInsideOneMultiLocationSourceScene() {
        var assets = ShortDramaSceneCheckpointTest.assets(); assets.locations().get(0).setName("县城城头_秋日");
        var outside = new ShortDramaLocation(); outside.setId(4L); outside.setName("县城外郊野_秋日"); assets.locations().add(outside);
        var a = anonymousPanel(); a.setLocation("县城城头_秋日"); var b = anonymousPanel(); b.setLocation("县城外郊野_秋日");
        assertDoesNotThrow(() -> assets.referenced(List.of(a, b)));
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, b), true);
        assertEquals(List.of(1, 1), List.of(a.getSceneNumber(), b.getSceneNumber()));
        b.setLocation("县城城头及城外郊野_秋日");
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(a, b)));
        String prompt = ShortDramaServiceImpl.buildStoryboardPlanPrompt("城头及城外郊野", "角色", "地点", "简介", "形象", "视觉", "chinese-3d", "16:9");
        assertTrue(prompt.contains("每镜location只能逐字选择一条真实登记场景名"));
        assertTrue(prompt.contains("不要求每个可见人头都成为角色资产"));
    }
    @Test void scopedSourceLibrariesKeepOffscreenVoiceAndNecessaryCGButNotPreviousOrQuotedNames() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        assets.characters().addAll(List.of(character(11L, "朱承晏", "承晏"), character(12L, "陈铁牛", "铁牛"),
            character(13L, "Codex", "青玉色半透明立方体"), character(14L, "周录音", "")));
        var library = ShortDramaServiceImpl.sceneCharacterLibraries(assets,
            "朱承晏身旁悬着青玉色半透明立方体。周录音（画外声）：「陈铁牛昨天修好了。」", "陈铁牛在桌旁站立");
        assertTrue(library.get("names").contains("朱承晏")); assertTrue(library.get("names").contains("Codex"));
        assertTrue(library.get("names").contains("周录音")); assertFalse(library.get("names").contains("陈铁牛"));
        assertFalse(library.get("descriptions").contains("工匠"));
    }
    @Test void phaseSixCannotAddDeleteOrReplacePlannedExtrasAndCannotAddNamedCast() throws Exception {
        var p = new StoryboardPanelData(); p.setPanelNumber(1); p.setDuration(6); p.setSourceText(ShortDramaVideoPromptReviewTest.SOURCE);
        p.setPresentCharacters(List.of("朱承晏")); p.setBackgroundExtras(EXTRAS);
        var result = new LinkedHashMap<String, Object>(); result.put("panel_number", 1); result.put("video_prompt", ShortDramaVideoPromptReviewTest.PROMPT);
        result.put("image_prompt", "朱承晏0秒站立，匿名军士在后方梯侧停步");
        result.put("background_extras", EXTRAS); result.put("present_characters", List.of("朱承晏"));
        assertEquals(1, ShortDramaServiceImpl.reviewStoryboardDetailResponse(JSON.writeValueAsString(List.of(result)), List.of(p)).size());
        for (String altered : List.of("", EXTRAS.replace("6-8", "10"))) {
            result.put("background_extras", altered); String raw = JSON.writeValueAsString(List.of(result));
            assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.reviewStoryboardDetailResponse(raw, List.of(p)));
        }
        result.put("background_extras", EXTRAS); result.put("present_characters", List.of("朱承晏", "Codex"));
        String injected = JSON.writeValueAsString(List.of(result));
        assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.reviewStoryboardDetailResponse(injected, List.of(p)));
        assertEquals(EXTRAS, p.getBackgroundExtras()); assertEquals(List.of("朱承晏"), p.getPresentCharacters());
    }
    @Test void detailLibraryIncludesLaterEntrantOffscreenSpeakerAndCGWithoutGlobalActors() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        assets.characters().addAll(List.of(character(11L, "朱承晏", "承晏"), character(12L, "陈铁牛", "铁牛"),
            character(13L, "Codex", "青玉色半透明立方体"), character(14L, "周录音", "")));
        var p = anonymousPanel(); p.setBackgroundExtras(null); p.setSourceText("周录音（画外声）：「看清箱子。」");
        var later = new CharacterRef(); later.setName("朱承晏"); var cg = new CharacterRef(); cg.setName("Codex");
        p.setCharacters(List.of(later, cg)); p.setPresentCharacters(List.of());
        var library = ShortDramaServiceImpl.panelCharacterLibraries(assets, List.of(p));
        assertTrue(library.get("names").contains("朱承晏")); assertTrue(library.get("names").contains("Codex"));
        assertTrue(library.get("names").contains("周录音")); assertFalse(library.get("names").contains("陈铁牛"));
        assertFalse(library.get("names").contains("工匠"));
    }
    @Test void timingRepairKeepsExtrasAndCanRestateTheNextShotsZeroSecondPosition() throws Exception {
        var p = ShortDramaSceneCheckpointTest.panel(); p.setBackgroundExtras(EXTRAS);
        var edit = JSON.createObjectNode().put("from_panel", 1).put("source_text", p.getSourceText()).put("duration", 5);
        edit.putObject("timing").put("speech_rate", 4).put("action_seconds", 0).put("pause_seconds", 1);
        String raw = JSON.writeValueAsString(List.of(edit));
        assertEquals(EXTRAS, ShortDramaSceneBudget.applyRepair(List.of(p), raw).get(0).getBackgroundExtras());
        String next = EXTRAS.replace("双手扶梯", "双手停在梯沿"); edit.put("background_extras", next);
        assertEquals(next, ShortDramaSceneBudget.applyRepair(List.of(p), JSON.writeValueAsString(List.of(edit))).get(0).getBackgroundExtras());
        assertTrue(ShortDramaSceneBudget.repairPrompt("乱兵群（预计5秒）", List.of(p), "预算调整").contains(EXTRAS));
        assertEquals(EXTRAS, p.getBackgroundExtras());
    }
    @Test void quantifiedFarmersAndServantsRemainSourceGroupsAndVisibleCountsAreOnlySubsets() {
        var assets = ShortDramaSceneCheckpointTest.assets(); assets.characters().get(0).setName("老农户"); assets.characters().get(0).setAliases("老汉");
        var p = anonymousPanel(); p.setBackgroundExtras(EXTRAS.replace("匿名乱兵", "匿名老农户"));
        assertDoesNotThrow(() -> assets.validateSceneCast("两个身穿旧麻布衣的老农户在水车旁等候", "", List.of(p)));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("1644年，老农户独自站在水车旁。", "", List.of(p)));
        p.setBackgroundExtras(EXTRAS.replace("6-8", "2").replace("匿名乱兵", "匿名富户"));
        assertDoesNotThrow(() -> assets.validateSceneCast("两个富户站在张家门外，张家仆役群守住门槛", "", List.of(p)));
        p.setBackgroundExtras(EXTRAS.replace("匿名乱兵", "匿名仆役"));
        assertDoesNotThrow(() -> assets.validateSceneCast("张家仆役群守住门槛", "", List.of(p)));
        p.setBackgroundExtras(EXTRAS.replace("6-8", "8-12").replace("匿名乱兵", "匿名溃兵"));
        assertDoesNotThrow(() -> assets.validateSceneCast("42名溃兵与2名村民在队尾停步", "", List.of(p)));
        assertEquals(12, ShortDramaBackgroundExtras.parse(p.getBackgroundExtras()).maximum());
    }
    @Test void positionCanReferToAlreadyVisibleNamedActorButNeverHideAnotherIdentityOrCG() {
        var assets = ShortDramaSceneCheckpointTest.assets(); assets.characters().get(0).setName("朱承晏");
        assets.characters().add(character(11L, "Codex", "青玉色半透明立方体"));
        var p = anonymousPanel(); p.setPresentCharacters(List.of("朱承晏"));
        p.setBackgroundExtras(EXTRAS.replace("位置=城墙下梯架两侧", "位置=朱承晏后侧远景").replace("尚未登上城头", "不复制前景朱承晏，尚未登上城头"));
        assertDoesNotThrow(() -> assets.validateSceneCast("朱承晏站在城头，乱兵群停在梯架两侧", "", List.of(p)));
        p.setPresentCharacters(List.of());
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("朱承晏在镜头后半段入画，乱兵群停在梯架两侧", "", List.of(p)));
        p.setPresentCharacters(List.of("朱承晏"));
        p.setBackgroundExtras(EXTRAS.replace("身份服饰=匿名乱兵", "身份服饰=匿名乱兵与朱承晏"));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("朱承晏与乱兵群在城头", "", List.of(p)));
        p.setBackgroundExtras(EXTRAS.replace("位置=城墙下梯架两侧", "位置=青玉色半透明立方体后侧"));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("朱承晏与乱兵群在城头", "", List.of(p)));
    }
    @Test void firstFrameReferenceListExcludesLaterEntrantsButLegacyRowsStillUseTheirBindings() throws Exception {
        var service = mock(ShortDramaVisualAssetService.class, CALLS_REAL_METHODS);
        var characters = mock(org.ruoyi.mapper.shortdrama.ShortDramaCharacterMapper.class);
        var appearances = mock(org.ruoyi.mapper.shortdrama.ShortDramaCharacterAppearanceMapper.class);
        var locations = mock(org.ruoyi.mapper.shortdrama.ShortDramaLocationMapper.class);
        ReflectionTestUtils.setField(service, "characters", characters); ReflectionTestUtils.setField(service, "appearances", appearances); ReflectionTestUtils.setField(service, "locations", locations);
        var actor = character(1L, "工匠", ""); actor.setReferenceImageUrl("https://example.test/actor.png");
        var location = new ShortDramaLocation(); location.setName("修渠处"); location.setReferenceImageUrl("https://example.test/location.png");
        when(characters.selectOne(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(actor);
        when(appearances.selectList(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        when(locations.selectOne(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(location);
        var shot = shot(List.of(), null); shot.setCharactersJson("[{\"name\":\"工匠\"}]"); shot.setProjectId(10L); shot.setLocationName("修渠处");
        List<String> refs = ReflectionTestUtils.invokeMethod(service, "baseReferences", shot);
        assertEquals(List.of("https://example.test/location.png"), refs); verifyNoInteractions(characters, appearances);
        ShortDramaVisualAssetService.FrameReferences bundle = ReflectionTestUtils.invokeMethod(service, "frameReferences", shot);
        assertNotNull(bundle); assertTrue(bundle.labels().contains("参考图1绑定场景建筑")); assertFalse(bundle.labels().contains("绑定人物"));
        assertTrue(ShortDramaVisualAssetService.firstFrameBindings(shot).isEmpty());
        shot.setContinuityJson("{\"present_characters\":[\"工匠\"]}");
        refs = ReflectionTestUtils.invokeMethod(service, "baseReferences", shot);
        assertEquals(List.of("https://example.test/actor.png", "https://example.test/location.png"), refs);
        bundle = ReflectionTestUtils.invokeMethod(service, "frameReferences", shot);
        assertNotNull(bundle); assertTrue(bundle.labels().contains("参考图1绑定人物：工匠")); assertTrue(bundle.labels().contains("参考图2绑定场景建筑"));
        shot.setContinuityJson("{}"); refs = ReflectionTestUtils.invokeMethod(service, "baseReferences", shot);
        assertEquals(List.of("https://example.test/actor.png", "https://example.test/location.png"), refs);
        shot.setContinuityJson("旧的非结构化连续性状态");
        refs = ReflectionTestUtils.invokeMethod(service, "baseReferences", shot);
        assertEquals(List.of("https://example.test/actor.png", "https://example.test/location.png"), refs);
        assertEquals("[{\"name\":\"工匠\"}]", shot.getCharactersJson()); // The video identity binding is never removed.
    }
    private static StoryboardPanelData anonymousPanel() {
        var p = ShortDramaSceneCheckpointTest.panel(); p.setCharacters(List.of()); p.setPresentCharacters(List.of());
        p.setSourceText("匿名乱兵在梯架两侧停步"); p.setBackgroundExtras(EXTRAS); return p;
    }
    private static ShortDramaCharacter character(long id, String name, String aliases) {
        var c = new ShortDramaCharacter(); c.setId(id); c.setName(name); c.setAliases(aliases); return c;
    }
    private static ShortDramaStoryboard shot(List<String> named, String extras) throws Exception {
        var shot = new ShortDramaStoryboard(); var continuity = JSON.createObjectNode().put("start_state", "0秒主体仍在原位，双手空置");
        continuity.set("present_characters", JSON.valueToTree(named)); if (extras != null) continuity.put("background_extras", extras);
        shot.setContinuityJson(continuity.toString()); shot.setCharactersJson(JSON.writeValueAsString(named.stream().map(n -> Map.of("name", n, "slot", "桌侧")).toList()));
        return shot;
    }
}
