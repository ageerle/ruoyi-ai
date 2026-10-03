package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaContinuityTest {
    @Test void normalizationKeepsProductionEstimatesAboveLegacyVideoLimit() {
        var shot = panel(1, "山谷", "连续飞行后回稳"); shot.setDuration(23);
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(shot));
        assertEquals(23, shot.getDuration());
    }
    @Test
    void normalizationPreservesShortShotBudgets() {
        var a=panel(1,"修理铺","取账本");a.setDuration(3);
        var b=panel(1,"修理铺","放入包");b.setDuration(3);
        var c=panel(1,"修理铺","背好包");c.setDuration(2);
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(a,b,c));
        assertEquals(8,a.getDuration()+b.getDuration()+c.getDuration());
    }

    @Test
    void compatibleDeepSeekUsesAdapterThatSendsThinkingSetting() {
        assertEquals("deepseek", ShortDramaServiceImpl.shortDramaProviderCode("openai", "deepseek-ai/deepseek-v4.1-flash"));
        assertEquals("deepseek", ShortDramaServiceImpl.shortDramaProviderCode("atlas", "deepseek-ai/deepseek-v4.1-flash"));
        assertEquals("atlas", ShortDramaServiceImpl.shortDramaProviderCode("atlas", "gemini-3-flash"));
        assertEquals("openai", ShortDramaServiceImpl.shortDramaProviderCode("openai", "gemini-3-flash"));
        assertEquals("custom_api", ShortDramaServiceImpl.shortDramaProviderCode("custom_api", "deepseek-v4"));
    }

    @Test
    void checkpointSurvivesRetryButChangesWithScriptOrAssetsOrProject() {
        String key = ShortDramaServiceImpl.checkpointKey(1L, 2L, "正文", "大纲", "realistic", "16:9", "15:100");
        assertEquals(key, ShortDramaServiceImpl.checkpointKey(1L, 2L, "正文", "大纲", "realistic", "16:9", "15:100"));
        assertNotEquals(key, ShortDramaServiceImpl.checkpointKey(1L, 2L, "正文已改", "大纲", "realistic", "16:9", "15:100"));
        assertNotEquals(key, ShortDramaServiceImpl.checkpointKey(1L, 2L, "正文", "大纲", "realistic", "16:9", "15:101"));
        assertNotEquals(key, ShortDramaServiceImpl.checkpointKey(3L, 2L, "正文", "大纲", "realistic", "16:9", "15:100"));
    }

    @Test
    void scenePromptRendersDurationBudgetWithoutFormatErrors() {
        String prompt = ShortDramaServiceImpl.buildStoryboardPlanPrompt("本场预计75秒", "陈念", "修理铺", "女儿", "衬衫", "人物描述", "写实", "16:9");
        assertTrue(prompt.contains("误差不超过10%"));
        assertTrue(prompt.contains("本场预计75秒"));
        assertFalse(prompt.contains("最多40个镜头"));
    }

    @Test
    void sameLocationOnNextDayIsANewScene() {
        var evening = panel(1, "维修铺", "女儿合上账本");
        var morning = panel(2, "维修铺", "女儿带上账本出门");
        morning.setStartState("次日上午，女儿已换好衣服站在门口");
        morning.setNarrativeCause("约定周三上午有人在家");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(evening, morning));
        assertEquals(2, morning.getSceneNumber());
        assertEquals(1, morning.getSegmentNumber());
        assertEquals("次日上午，女儿已换好衣服站在门口", morning.getStartState());
        assertEquals("约定周三上午有人在家", morning.getNarrativeCause());
    }

    @Test
    void preservesSpecificCauseAndStartStateInContinuousScene() {
        var first = panel(1, "维修铺", "账本摊开在桌上");
        var next = panel(1, "维修铺", "女儿标记重复地址");
        next.setStartState("女儿低头看桌上摊开的账本，镜头切近手部");
        next.setNarrativeCause("她发现同一地址出现五次");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(first, next));
        assertEquals("她发现同一地址出现五次", next.getNarrativeCause());
        assertEquals("女儿低头看桌上摊开的账本，镜头切近手部", next.getStartState());
        assertEquals(2, next.getSegmentNumber()); // 8 + 8 exceeds the 15-second segment budget.
    }

    @Test
    void missingContinuousStartCanInheritActualPreviousEnd() {
        var first = panel(1, "维修铺", "右手压住账本，左手指向地址");
        var next = panel(1, "维修铺", "拿起手机拍照");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(first, next));
        assertEquals(first.getEndState(), next.getStartState());
    }

    @Test
    void sceneSplittingPreservesPreambleTimeJumpAndEnding() {
        var scenes = ShortDramaServiceImpl.splitScriptScenes("片名：爸爸没说完的话\n内景 维修铺 夜（预计40秒）\n发现账本\n外景 榕树巷 次日（预计40秒）\n敲门\n内景 维修铺 次日夜（预计40秒）\n留下下一次探访日期");
        assertEquals(3, scenes.size());
        assertTrue(scenes.get(0).startsWith("片名"));
        assertTrue(scenes.get(2).contains("下一次探访日期"));
    }

    @Test void numberedMarkdownHeadingsSplitWithoutSplittingProse() {
        var scenes = ShortDramaServiceImpl.splitScriptScenes("# 片名\n### 一　黑屏。冬。\n一只布鞋踩进积水。\n### 二　县衙。接上。\n他说了两句。\n### 三　库房。\n结尾。");
        assertEquals(3, scenes.size());
        assertTrue(scenes.get(0).contains("一只布鞋"));
        assertTrue(scenes.get(2).contains("结尾"));
    }

    @Test void explicitEmptyCastDoesNotInheritPreviousActor() {
        var first=panel(1,"维修铺","陈念在电脑前");first.setPresentCharacters(List.of("陈念"));
        var screen=panel(1,"维修铺","屏幕插入");screen.setPresentCharacters(List.of());
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(first,screen));
        assertTrue(screen.getPresentCharacters().isEmpty());
    }

    private static ShortDramaServiceImpl.StoryboardPanelData panel(int scene, String location, String end) {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData();
        panel.setSceneNumber(scene);
        panel.setSegmentNumber(1);
        panel.setLocation(location);
        panel.setDuration(8);
        panel.setEndState(end);
        panel.setStoryResult(end);
        return panel;
    }
}
