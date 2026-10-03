package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaSourceSceneContinuityTest {
    @Test void twoSourceBudgetsKeepReliefPointAndReturnHomeInOneFortyFiveSecondScene() {
        var grain = panel(1, "赈济点_傍晚", 15, "王二嫂领粮");
        var receipt = panel(1, "赈济点_傍晚", 15, "确认份额");
        var home = panel(1, "王二嫂家_夜", 15, "王二嫂回家分粮");
        var office = panel(2, "县衙_夜", 10, "县令核对余粮");
        grain.setEndState("王二嫂左手捧粮，站在赈济点队首"); grain.setSpatialAnchor("赈济点长桌左侧");
        receipt.setEndState("王二嫂仍在赈济点长桌左侧"); receipt.setSpatialAnchor("赈济点长桌左侧");
        receipt.setPresentCharacters(List.of("王二嫂", "粮吏"));
        var cast = new ShortDramaServiceImpl.CharacterRef(); cast.setName("王二嫂"); home.setCharacters(List.of(cast));
        var panels = List.of(grain, receipt, home, office);
        var sources = panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getSourceText).toList();
        ShortDramaServiceImpl.normalizeContinuityChain(panels, true);
        assertEquals(List.of(1, 1, 1, 2), panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getSceneNumber).toList());
        assertEquals(List.of(1, 2, 3, 1), panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getSegmentNumber).toList());
        assertNotEquals(receipt.getEndState(), home.getStartState()); assertNull(home.getSpatialAnchor());
        assertEquals(List.of("王二嫂"), home.getPresentCharacters()); assertFalse(home.getPresentCharacters().contains("粮吏"));
        assertEquals(sources, panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getSourceText).toList());
        assertBudgets("外景 赈济点 傍晚（预计45秒）\n王二嫂领粮后回家分粮。\n内景 县衙 夜（预计10秒）\n县令核对余粮。", panels);
    }
    @Test void seventyFiveSecondCrossYearMontageKeepsItsSourceSceneAndApprovedThreeLocations() {
        var opening = panel(1, "县衙_1639晨", 10, "县令核账");
        var panels = new ArrayList<ShortDramaServiceImpl.StoryboardPanelData>(); panels.add(opening);
        panels.add(panel(2, "水车_1639初建", 10, "安装轮轴")); panels.add(panel(2, "水车_1639初建", 15, "村民验水"));
        panels.add(panel(2, "县城_中期发展", 10, "集市扩展")); panels.add(panel(2, "县城_中期发展", 10, "修缮铺面"));
        panels.add(panel(2, "城防_1644", 15, "队伍集结")); panels.add(panel(2, "城防_1644", 15, "检查城门"));
        List<String> locations = panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getLocation).toList();
        ShortDramaServiceImpl.normalizeContinuityChain(panels, true);
        assertTrue(panels.subList(1, panels.size()).stream().allMatch(p -> p.getSceneNumber() == 2));
        assertEquals(75, panels.subList(1, panels.size()).stream().mapToInt(ShortDramaServiceImpl.StoryboardPanelData::getDuration).sum());
        assertEquals(locations, panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getLocation).toList());
        assertTrue(panels.get(3).getSegmentNumber() > panels.get(2).getSegmentNumber());
        assertTrue(panels.get(5).getSegmentNumber() > panels.get(4).getSegmentNumber());
        assertBudgets("内景 县衙 清晨（预计10秒）\n县令核账。\n外景 县域 跨年蒙太奇（预计75秒）\n1639初建，中期发展，1644城防集结。", panels);
    }
    @Test void legacyUnnumberedPlansStillInferScenesFromSpacesAndResetOnReturn() {
        var a = panel(null, "赈济点", 5, "领粮"); var b = panel(null, "家", 5, "分粮"); var c = panel(null, "赈济点", 5, "补账");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, b, c));
        assertEquals(List.of(1, 2, 3), List.of(a.getSceneNumber(), b.getSceneNumber(), c.getSceneNumber()));
        assertEquals(List.of(1, 1, 1), List.of(a.getSegmentNumber(), b.getSegmentNumber(), c.getSegmentNumber()));
    }
    @Test void sameSourceAndSpaceStillInheritActionButSameSpaceWithNewSourceResetsTheSegment() {
        var a = panel(5, "县衙", 5, "先核对账页"); a.setEndState("右手压住账本，左手指向地址"); a.setSpatialAnchor("桌左侧"); a.setPresentCharacters(List.of("县令"));
        var b = panel(5, "县衙", 5, "标记地址");
        var c = panel(6, "县衙", 5, "次日核账"); c.setStartState("次日清晨坐在桌右侧");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, b, c), true);
        assertEquals(a.getEndState(), b.getStartState()); assertEquals("桌左侧", b.getSpatialAnchor()); assertEquals(List.of("县令"), b.getPresentCharacters());
        assertEquals(List.of(5, 5, 6), List.of(a.getSceneNumber(), b.getSceneNumber(), c.getSceneNumber()));
        assertEquals(List.of(1, 1, 1), List.of(a.getSegmentNumber(), b.getSegmentNumber(), c.getSegmentNumber()));
        assertEquals("次日清晨坐在桌右侧", c.getStartState()); assertNull(c.getSpatialAnchor());
    }
    @Test void sourceModeRejectsMissingOrBackwardIdentityBeforeChangingAnyPanel() {
        var a = panel(2, "县衙", 5, "第一镜"); var missing = panel(null, "家", 5, "未编号");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, missing), true));
        assertNull(a.getStartState()); assertEquals(2, a.getSceneNumber());
        var backward = panel(1, "县衙", 5, "乱序");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, backward), true));
    }
    @Test void sameGeometryTimeJumpKeepsTheExplicitNewPhasePoseRatherThanForcingAnInstantTransformation() {
        var a = panel(2, "同一水车几何_早期", 10, "1639年初建"); a.setEndState("24岁县令穿青布衣，右手扶轴");
        var b = panel(2, "同一水车几何_早期", 10, "1641年匹配切"); b.setStartState("26岁县令穿秋衣，双手空置站在水车前");
        ShortDramaServiceImpl.normalizeContinuityChain(List.of(a, b), true);
        assertEquals("26岁县令穿秋衣，双手空置站在水车前", b.getStartState()); assertEquals(2, b.getSceneNumber());
        String phase3 = ShortDramaServiceImpl.buildStoryboardPlanPrompt("跨年", "角色", "地点", "简介", "形象", "外观", "chinese-3d", "16:9");
        assertTrue(phase3.contains("同一location且同一连续时刻")); assertTrue(phase3.contains("保留几何"));
    }
    private static void assertBudgets(String text, List<ShortDramaServiceImpl.StoryboardPanelData> panels) {
        ShortDramaServiceImpl.StoryboardPanelData previous = null;
        for (var panel : panels) {
            var design = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            for (String field : List.of("focus", "viewer_gain", "framing", "axis", "movement", "motivation")) design.put(field, "当前原文动作");
            boolean newScene = previous == null || !previous.getSceneNumber().equals(panel.getSceneNumber());
            boolean newSpace = previous != null && !previous.getLocation().equals(panel.getLocation());
            design.put("transition", newScene ? "opening" : newSpace ? "space_change" : "continuous");
            design.put("cut_in", "动作交接点"); design.put("cut_out", "动作交接点");
            design.putObject("state_in").put("地点", panel.getLocation());
            design.putObject("state_out").put("地点", panel.getLocation());
            panel.setShotDesign(design); previous = panel;
        }
        var script = new ShortDramaScript(); script.setScriptText(text);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(ShortDramaServiceImpl.class, "validateCompletedSceneBudgets", script, panels, 4));
    }
    private static ShortDramaServiceImpl.StoryboardPanelData panel(Integer scene, String location, int duration, String source) {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData(); panel.setSceneNumber(scene); panel.setSegmentNumber(1);
        panel.setLocation(location); panel.setDuration(duration); panel.setSourceText(source); return panel;
    }
}
