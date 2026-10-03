package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class ShortDramaDirectorSkillsTest {
    @Test void imageCompletionKeepsPlanningCheckpointButDescriptionChangesInvalidateIt() {
        var location = new org.ruoyi.domain.entity.shortdrama.ShortDramaLocation();
        location.setId(1L); location.setName("维修铺"); location.setDescriptions("正常维护");
        String before = ShortDramaServiceImpl.planningAssetSignature(java.util.List.of(location));
        location.setImageUrls("[\"https://example.com/new-image.png\"]");
        location.setUpdateTime(new java.util.Date());
        assertEquals(before, ShortDramaServiceImpl.planningAssetSignature(java.util.List.of(location)));
        location.setDescriptions("家具位置改变");
        assertNotEquals(before, ShortDramaServiceImpl.planningAssetSignature(java.util.List.of(location)));
    }
    @Test void sceneBudgetIsAnAdvisoryEstimate() {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData(); panel.setDuration(5);
        var panels = new java.util.ArrayList<ShortDramaServiceImpl.StoryboardPanelData>();
        for(int i=0;i<17;i++) panels.add(panel);
        assertDoesNotThrow(()->ShortDramaServiceImpl.validateSceneDuration("内景 维修铺（预计95秒）",panels));
        assertFalse(ShortDramaServiceImpl.scenePlanIssues("内景 维修铺（预计95秒）",panels).isEmpty());
    }
    @Test void bundledSkillLoadsEvenWhenWorkerContextCannotSeeApplicationResources() {
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(new ClassLoader(null) {});
            assertTrue(ShortDramaDirectorSkills.read("director-blocking").contains("默认")
                || ShortDramaDirectorSkills.read("director-blocking").contains("调度"));
        } finally { thread.setContextClassLoader(previous); }
    }
    @Test void allDefaultSkillsAreBundledAndVersioned() {
        for (String name : new String[]{"emotional-dialogue","story-causality","director-blocking","visual-world","character-art-direction","real-material","director-review","video-continuity","video-prompt","script-refinement"}) {
            String text=ShortDramaDirectorSkills.load(name);
            assertTrue(text.contains("[skill:"+name+"@"));
            assertFalse(text.contains("description:"));
        }
        assertThrows(IllegalStateException.class,()->ShortDramaDirectorSkills.load("missing-skill"));
    }
    @Test void referenceMaterialIsStyleAwareAndKeepsUsersIdentityMarks() {
        String realistic=ShortDramaCharacterArtPrompt.reference("左腕浅灰旧布，右手无伤", "realistic");
        assertTrue(realistic.contains("【写实身份参照材质】"));
        assertTrue(realistic.contains("左腕浅灰旧布，右手无伤"));
        assertTrue(realistic.contains("【用途：身份参照图，不是剧情首帧】"));
        String comic=ShortDramaCharacterArtPrompt.reference("黑色短发", "chinese-comic");
        assertFalse(comic.contains("【写实身份参照材质】"));
        assertTrue(comic.contains("Chinese donghua 2D comic style"));
        assertTrue(comic.contains("不混入真人肤质或摄影景深"));
    }
    @Test void performanceIsMatchedByPersonRatherThanSharedEmotionTemplate() throws Exception {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData();
        panel.setPanelNumber(1);
        panel.setSourceText("陈念：我没说要扔。林叔收回伸向纸箱的手。");
        var daughter = new ShortDramaServiceImpl.CharacterRef(); daughter.setName("陈念");
        var uncle = new ShortDramaServiceImpl.CharacterRef(); uncle.setName("林叔");
        panel.setCharacters(java.util.List.of(daughter, uncle));
        panel.setPerformanceBeats(new com.fasterxml.jackson.databind.ObjectMapper().readTree(
            "[{\"name\":\"林叔\",\"acting\":\"听见她拒绝后收回手，不再催促\"},{\"name\":\"陈念\",\"acting\":\"护住眼镜盒，拒绝叔叔替自己决定\"},{\"name\":\"父亲\",\"acting\":\"不应出镜\"}]"));
        var result = ShortDramaServiceImpl.buildLocalActingDirections(java.util.List.of(panel)).get(0).getCharacters();
        assertEquals(2, result.size());
        assertEquals("护住眼镜盒，拒绝叔叔替自己决定", result.get(0).path("acting").asText());
        assertEquals("听见她拒绝后收回手，不再催促", result.get(1).path("acting").asText());
    }
    @Test void missingPerformanceUsesSourceInsteadOfInventingTears() {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData(); panel.setPanelNumber(1);
        panel.setSceneType("emotion"); panel.setSourceText("陈念把零钱推回周姨面前。");
        var ref = new ShortDramaServiceImpl.CharacterRef(); ref.setName("陈念");
        panel.setCharacters(java.util.List.of(ref));
        String acting = ShortDramaServiceImpl.buildLocalActingDirections(java.util.List.of(panel)).get(0).getCharacters().toString();
        assertTrue(acting.contains("把零钱推回"));
        assertFalse(acting.contains("释放情绪"));
    }
    @Test void actualFramePromptCarriesGazeAndWorldRulesBeforeShotDescription() {
        var shot=new ShortDramaStoryboard();shot.setImagePrompt("女主正在清理遗物");
        shot.setContinuityJson("{\"start_state\":\"低头整理工具\"}");
        String result=ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(result.contains("只有剧本明确要求打破第四面墙才直视镜头"));
        assertTrue(result.contains("维护程度"));assertTrue(result.contains("低头整理工具"));
        assertTrue(result.indexOf("[skill:")<result.indexOf("低头整理工具"));
    }
}
