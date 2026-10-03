package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaShotDesignTest {
    private final ObjectMapper json = new ObjectMapper();
    private ShortDramaServiceImpl.StoryboardPanelData shot(String transition, String in, String out, String poseIn, String poseOut) {
        ObjectNode design = json.createObjectNode();
        design.put("focus", "右手与麻绳的受力接触点"); design.put("viewer_gain", "右手拉索令倾斜减小");
        design.put("framing", "同侧中近景，横杆与翼缘留在背景"); design.put("axis", "左侧伴飞，屏幕向右");
        design.put("movement", "固定伴飞"); design.put("motivation", "稳定构图让手的拉动和翼缘回正同时可见");
        design.put("transition", transition); design.put("cut_in", in); design.put("cut_out", out);
        design.putObject("state_in").put("匠人.悬挂姿态", poseIn).put("飞行.屏幕方向", "向右");
        design.putObject("state_out").put("匠人.悬挂姿态", poseOut).put("飞行.屏幕方向", "向右");
        var shot = new ShortDramaServiceImpl.StoryboardPanelData(); shot.setShotDesign(design); return shot;
    }
    @Test void acceptsContinuousActionWithSharedPhysicalStateAndCut() {
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(
            shot("opening", "开场", "右手拉索起势", "俯身悬挂", "俯身悬挂"),
            shot("continuous", "右手拉索起势", "翼面回正", "俯身悬挂", "俯身悬挂"))));
    }
    @Test void reportsPoseJumpForReview() {
        var issues = ShortDramaShotDesign.issues(List.of(
            shot("opening", "开场", "接上", "俯身悬挂", "俯身悬挂"),
            shot("continuous", "接上", "继续滑翔", "坐姿悬挂", "坐姿悬挂")));
        assertTrue(issues.get(0).contains("物理状态跳变"));
    }
    @Test void acceptsDraftWithActionAndFocusReviewNotes() {
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(
            shot("opening", "开场", "已开始拉索", "俯身悬挂", "俯身悬挂"),
            shot("continuous", "准备拉索", "完成拉索", "俯身悬挂", "俯身悬挂"))));
        var blank = shot("opening", "开场", "结束", "俯身悬挂", "俯身悬挂");
        ((ObjectNode)blank.getShotDesign()).put("focus", "");
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(blank)));
    }
    @Test void permitsTimeChangeAndReviewsReopening() {
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(
            shot("opening", "早年建立", "旧姿势落点", "青年站立", "青年站立"),
            shot("time_change", "原文十年后建立", "新姿势落点", "中年坐下", "中年坐下"))));
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(
            shot("opening", "开场", "姿势落点", "俯身悬挂", "俯身悬挂"),
            shot("opening", "重新建立", "结束", "坐姿悬挂", "坐姿悬挂"))));
    }
    @Test void reviewsMissingPhysicalAnchorWithoutBlocking() {
        var panel = shot("opening", "开场", "结束", "俯身悬挂", "俯身悬挂");
        ((ObjectNode)panel.getShotDesign().path("state_out")).remove("匠人.悬挂姿态");
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(panel)));
    }
    @Test void loadsProfessionalSkillInPlanningAndDetailingOnly() {
        String planning = ShortDramaServiceImpl.buildStoryboardPlanPrompt("测试原文", "", "", "", "", "", "国风CG", "16:9");
        String detailing = ShortDramaServiceImpl.buildStoryboardDetailPrompt("[]", "", "", "国风CG", "16:9");
        assertTrue(planning.contains("[skill:cinematic-storyboard@"));
        assertTrue(detailing.contains("[skill:cinematic-storyboard@"));
        assertTrue(planning.contains("shot_design"));
    }
    @Test void expandsOnlyExplicitDeltaAndPreservesUnchangedAnchorsAndActionCut() {
        var first = shot("opening", "开场", "手腕沉力", "俯身悬挂", "俯身悬挂");
        var next = shot("continuous", "unused", "调索完毕", "unused", "unused");
        var a = (ObjectNode) first.getShotDesign(); a.remove("state_out"); a.putObject("state_delta");
        var b = (ObjectNode) next.getShotDesign(); b.remove(List.of("state_in", "state_out", "cut_in"));
        b.putObject("state_delta").put("匠人.悬挂姿态", "俯身悬挂并轻倾回稳");
        ShortDramaShotDesign.expandDeltas(List.of(first, next));
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(first, next)));
        assertEquals(a.path("state_out"), b.path("state_in"));
        assertEquals("向右", b.path("state_out").path("飞行.屏幕方向").asText());
        assertEquals("手腕沉力", b.path("cut_in").asText());
        assertFalse(b.has("state_delta"));
    }
    @Test void deltaDoesNotOverwriteExplicitEndState() {
        var renamed = shot("opening", "开场", "结束", "俯身悬挂", "俯身悬挂");
        ((ObjectNode)renamed.getShotDesign()).putObject("state_delta").put("匠人.坐姿", "坐着");
        assertDoesNotThrow(() -> ShortDramaShotDesign.expandDeltas(List.of(renamed)));
        var contrary = shot("opening", "开场", "结束", "俯身悬挂", "坐着");
        ((ObjectNode)contrary.getShotDesign()).putObject("state_delta");
        assertDoesNotThrow(() -> ShortDramaShotDesign.expandDeltas(List.of(contrary)));
        assertEquals("坐着", contrary.getShotDesign().path("state_out").path("匠人.悬挂姿态").asText());
    }
    @Test void legacyContinuityRemainsEditable() {
        var first = shot("opening", "开场", "拉索中", "俯身悬挂", "俯身悬挂");
        var next = shot("continuous", "拉索中", "结束", "坐着", "坐着");
        ((ObjectNode)next.getShotDesign()).putObject("state_delta");
        ShortDramaShotDesign.expandDeltas(List.of(first, next));
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(first, next)));
        ((ObjectNode)first.getShotDesign().path("state_out")).remove("飞行.屏幕方向");
        ShortDramaShotDesign.expandDeltas(List.of(first));
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(List.of(first)));
    }
    @Test void missingTimeChangeInitialStateStaysUnknown() {
        var first = shot("opening", "开场", "结束", "青年站立", "青年站立");
        var next = shot("time_change", "十年后", "结束", "中年坐着", "中年坐着");
        var b = (ObjectNode)next.getShotDesign(); b.remove(List.of("state_in", "state_out")); b.putObject("state_delta");
        assertDoesNotThrow(() -> ShortDramaShotDesign.expandDeltas(List.of(first, next)));
        assertFalse(b.has("state_in"));
    }
}
