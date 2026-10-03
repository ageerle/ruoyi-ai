package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev") class ShortDramaVisualAssetTest {
    @Test void originalMediaIsExcludedFromGenerativeFrames() {
        assertTrue(ShortDramaVisualAssetService.directInsert("{\"source_media\":{\"mode\":\"direct_insert\"}}"));
        assertFalse(ShortDramaVisualAssetService.directInsert("{\"source_media\":{\"mode\":\"reference\"}}"));
        assertFalse(ShortDramaVisualAssetService.directInsert(null));
    }
    @Test void sceneBudgetRemainsAnEditableEstimate() {
        var panel=new ShortDramaServiceImpl.StoryboardPanelData();panel.setDuration(15);
        assertDoesNotThrow(()->ShortDramaServiceImpl.validateSceneDuration("第一场 内景 修理铺 下午（预计8秒）",List.of(panel)));
        panel.setDuration(8);
        assertDoesNotThrow(()->ShortDramaServiceImpl.validateSceneDuration("第一场 内景 修理铺 下午（预计8秒）",List.of(panel)));
    }
    @Test void voiceOnlyCharacterDoesNotRequireVisualIdentity() {
        var character=new org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter();
        character.setIntroduction("全剧仅以手机语音出现，无实体出场");
        assertTrue(ShortDramaVisualAssetService.voiceOnly(character));
        character.setIntroduction("陈念在铺内播放手机语音");
        assertFalse(ShortDramaVisualAssetService.voiceOnly(character));
    }
    @Test void changedStateOrReferenceInvalidatesFrame() {
        var s=new ShortDramaStoryboard();s.setImagePrompt("同一画面");s.setCharactersJson("[]");s.setContinuityJson("坐着");
        var first=ShortDramaVisualAssetService.frameHash(s,List.of("face"),"props");
        s.setContinuityJson("站着");assertNotEquals(first,ShortDramaVisualAssetService.frameHash(s,List.of("face"),"props"));
        s.setContinuityJson("坐着");assertNotEquals(first,ShortDramaVisualAssetService.frameHash(s,List.of("new face"),"props"));
        assertTrue(ShortDramaVisualAssetService.framePrompt(s).contains("只画起始状态"));
    }
    @Test void singleFrameRevisionCanReuseThePreviousCompositionAsAnEditReference() {
        assertEquals("https://example.com/previous.jpg", ShortDramaVisualAssetService.previousFrameReference(
            "{\"previousImageUrl\":\"https://example.com/previous.jpg\"}"));
        assertEquals("", ShortDramaVisualAssetService.previousFrameReference(
            "{\"previousImageUrl\":\"http://example.com/unsafe.jpg\"}"));
        assertEquals("", ShortDramaVisualAssetService.previousFrameReference("not-json"));
    }
    @Test void propMetadataKeepsLegacyShotBindingsAndASeparateUserReference() {
        assertEquals(List.of(1,3), ShortDramaVisualAssetService.propShotNumbers("[3,1,3]"));
        assertEquals("", ShortDramaVisualAssetService.propReferenceImage("[3,1,3]"));
        String metadata = ShortDramaVisualAssetService.propMetadataJson(List.of(3,1), "https://example.com/prop-reference.jpg");
        assertEquals(List.of(1,3), ShortDramaVisualAssetService.propShotNumbers(metadata));
        assertEquals("https://example.com/prop-reference.jpg", ShortDramaVisualAssetService.propReferenceImage(metadata));
    }
    @Test void cameraChangeInvalidatesPreviouslyGeneratedComposition() {
        var shot=new ShortDramaStoryboard();shot.setImagePrompt("核对账本");shot.setShotType("平视中景");
        var original=ShortDramaVisualAssetService.frameHash(shot,List.of("face"),"props");
        shot.setShotType("俯拍近景");
        assertNotEquals(original,ShortDramaVisualAssetService.frameHash(shot,List.of("face"),"props"));
        assertTrue(ShortDramaVisualAssetService.framePrompt(shot).contains("俯拍近景"));
    }
    @Test void firstFrameDoesNotMixInAConflictingEndPose() {
        var shot=new ShortDramaStoryboard();shot.setImagePrompt("两人已经坐好");
        shot.setContinuityJson("{\"start_state\":\"两人站立准备拉椅\",\"present_characters\":[\"陈念\",\"周姨\"]}");
        String prompt=ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("两人站立准备拉椅"));assertFalse(prompt.contains("两人已经坐好"));
    }
    @Test void soloFrameDoesNotImportAnAbsentCharacterFromSceneAnchor() {
        var shot=new ShortDramaStoryboard();
        shot.setCharactersJson("[{\"name\":\"陈念\",\"slot\":\"工作台右侧\"}]");
        shot.setContinuityJson("{\"start_state\":\"陈念低头收工具\",\"present_characters\":[\"陈念\"],\"spatial_anchor\":\"陈念在工作台右侧，林叔在入口\"}");
        String prompt=ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("画面严格只有1人：陈念"));
        assertTrue(prompt.contains("工作台右侧"));
        assertFalse(prompt.contains("林叔在入口"));
    }
    @Test void closeFrameUsesNarrativeFocusWithoutForcingAllParticipantsIntoFrame() {
        var shot=new ShortDramaStoryboard();shot.setShotType("近景");shot.setSceneTitle("周姨说有人嗯一声与没人不同");
        shot.setCharactersJson("[{\"name\":\"陈念\"},{\"name\":\"周姨\"}]");
        String instruction=ShortDramaVisualAssetService.closeFrameInstruction(shot);
        assertTrue(instruction.contains("以周姨为视觉中心"));
        assertTrue(instruction.contains("不要求全部同时入画"));
        shot.setContinuityJson("{\"frame_focus\":\"陈念的倾听反应\"}");
        assertTrue(ShortDramaVisualAssetService.closeFrameInstruction(shot).contains("以陈念的倾听反应为视觉中心"));
        shot.setShotType("双人中景");assertEquals("",ShortDramaVisualAssetService.closeFrameInstruction(shot));
    }
    @Test void reviewedDirectiveSurvivesStructuredStartState() {
        var shot=new ShortDramaStoryboard();shot.setImagePrompt("手已放开手机");
        shot.setContinuityJson("{\"start_state\":\"右手持手机\",\"keyframe_directive\":\"两枚对角后摄，手机仍在手中\"}");
        String prompt=ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("右手持手机"));
        assertTrue(prompt.contains("两枚对角后摄，手机仍在手中"));
        assertFalse(prompt.contains("手已放开手机"));
    }
    @Test void morningLightingAndMaterialsSurviveWithoutImportingLaterSitUpAction() {
        var shot = new ShortDramaStoryboard(); shot.setShotType("侧面近景");
        shot.setImagePrompt("朱承晏坐起伸手接纸。清晨冷蓝天光从左窗透入，低曝光保留窗外细节。粗麻衣料与炕板旧木纹清晰，青灰冷色调。镜头背景还有两名老农人影。");
        shot.setContinuityJson("{\"start_state\":\"朱承晏仰卧炕上，双手空置\",\"present_characters\":[\"朱承晏\"]}");
        String prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("朱承晏仰卧炕上，双手空置"));
        assertTrue(prompt.contains("清晨冷蓝天光从左窗透入，低曝光保留窗外细节"));
        assertTrue(prompt.contains("粗麻衣料与炕板旧木纹清晰"));
        assertTrue(prompt.contains("不锁时间、天气、色温、光照或曝光"));
        assertFalse(prompt.contains("坐起伸手接纸")); assertFalse(prompt.contains("两名老农人影"));
    }
    @Test void mixedActionClauseIsOmittedAndStructuredCurrentLightingCanSupplyMorning() {
        var shot = new ShortDramaStoryboard();
        shot.setImagePrompt("朱承晏坐起，清晨光线映在脸上；傍晚人物站在门口。");
        shot.setPhotographyRules("{\"lighting\":{\"direction\":\"清晨主光从画面左侧窗户照入\",\"quality\":\"柔和冷蓝色天光\"},\"depth_of_field\":\"浅景深与旧木纹质感\",\"acting\":\"朱承晏坐起\"}");
        shot.setContinuityJson("{\"start_state\":\"朱承晏仰卧，双手空置\"}");
        String prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("清晨主光从画面左侧窗户照入")); assertTrue(prompt.contains("柔和冷蓝色天光"));
        assertFalse(prompt.contains("坐起")); assertFalse(prompt.contains("傍晚人物站"));
    }
}
