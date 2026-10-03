package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaSceneCheckpointTest {
    static class Memory implements ShortDramaSceneCheckpoint.Cache {
        final Map<String, String> values = new ConcurrentHashMap<>();
        public String get(String key) { return values.get(key); }
        public void put(String key, String value) { values.put(key, value); }
    }
    static ShortDramaScript script() {
        var s = new ShortDramaScript(); s.setId(20L); s.setProjectId(10L); s.setWorldbuilding("旧县初建，木材与石材"); s.setOutlineText("先修渠再试水"); return s;
    }
    static ShortDramaSceneCheckpoint.Identity identity(String scene) {
        return ShortDramaSceneCheckpoint.identity(10L, script(), 1, scene, "", "chinese-3d", "16:9", "writer");
    }
    static ShortDramaSceneCheckpoint.Assets assets() {
        var c = new ShortDramaCharacter(); c.setId(1L); c.setName("工匠"); c.setVisualDescription("青衣圆领");
        var a = new ShortDramaCharacterAppearance(); a.setId(2L); a.setCharacterId(1L); a.setAppearanceIndex(0); a.setChangeReason("初始造型"); a.setDescription("青布圆领");
        var l = new ShortDramaLocation(); l.setId(3L); l.setName("修渠处"); l.setDescriptions("未完工的木制水车");
        return new ShortDramaSceneCheckpoint.Assets(new ArrayList<>(List.of(c)), new ArrayList<>(List.of(a)), new ArrayList<>(List.of(l)));
    }
    static StoryboardPanelData panel() {
        var p = new StoryboardPanelData(); p.setPanelNumber(1); p.setSceneNumber(1); p.setLocation("修渠处"); p.setDuration(5);
        p.setSourceText("工匠：「可以试水了。」");
        var ref = new ShortDramaServiceImpl.CharacterRef(); ref.setName("工匠"); ref.setAppearance("初始造型");
        p.setCharacters(List.of(ref)); p.setPresentCharacters(List.of("工匠")); return p;
    }
    @Test void acceptsOnlyASingleWrappedAppearanceIdentifier() throws Exception {
        var mapper = new ObjectMapper();
        var wrapped = mapper.readValue("{\"name\":\"朱承晏\",\"appearance\":[\"2026程序员（穿越前场景专用）\"],\"slot\":\"桌前\"}",
            ShortDramaServiceImpl.CharacterRef.class);
        assertEquals("2026程序员（穿越前场景专用）", wrapped.getAppearance());
        assertThrows(JsonMappingException.class, () -> mapper.readValue(
            "{\"name\":\"朱承晏\",\"appearance\":[\"现代\",\"古代\"]}", ShortDramaServiceImpl.CharacterRef.class));
    }
    @Test void imageSelectionAndUnrelatedNewAssetKeepSceneButReferencedDescriptionInvalidates() {
        var memory = new Memory(); var store = new ShortDramaSceneCheckpoint(memory); var assets = assets(); var id = identity("外景 修渠处（预计5秒）");
        store.save(id, assets, List.of(panel()), true, "native_validated");
        assets.locations().get(0).setImageUrls("[\"https://example.test/new.png\"]"); assets.locations().get(0).setSelectedImageIndex(1);
        assets.characters().get(0).setUpdateTime(new Date()); assets.appearances().get(0).setReferenceImageUrl("https://example.test/approved.png");
        assertNotNull(store.compatible(id, assets, true));
        var unrelated = new ShortDramaLocation(); unrelated.setId(4L); unrelated.setName("新登记空间"); unrelated.setDescriptions("尚未使用"); assets.locations().add(unrelated);
        assertNotNull(store.compatible(id, assets, true));
        assets.locations().get(0).setDescriptions("水车已经拆除"); assertNull(store.compatible(id, assets, true));
    }
    @Test void onlyActuallyReferencedAppearanceInvalidatesScene() {
        var store = new ShortDramaSceneCheckpoint(new Memory()); var assets = assets(); var id = identity("原场次");
        var unused = new ShortDramaCharacterAppearance(); unused.setId(9L); unused.setCharacterId(1L); unused.setAppearanceIndex(1); unused.setChangeReason("冬衣");
        assets.appearances().add(unused); store.save(id, assets, List.of(panel()), true, "native_validated");
        unused.setDescription("厚冬衣"); assertNotNull(store.compatible(id, assets, true));
        assets.appearances().get(0).setDescription("红色交领"); assertNull(store.compatible(id, assets, true));
    }
    @Test void rawSceneBudgetBridgeWorldModelAndSceneNumberAreProofComponents() {
        var store = new ShortDramaSceneCheckpoint(new Memory()); var assets = assets(); var s = script(); String scene = "外景 修渠处（预计5秒）";
        var id = ShortDramaSceneCheckpoint.identity(10L, s, 1, scene, "桥梁", "cg", "16:9", "writer");
        store.save(id, assets, List.of(panel()), true, "native_validated");
        for (var changed : List.of(
            ShortDramaSceneCheckpoint.identity(10L, s, 1, scene.replace("5秒", "8秒"), "桥梁", "cg", "16:9", "writer"),
            ShortDramaSceneCheckpoint.identity(10L, s, 1, scene, "改过的上一场", "cg", "16:9", "writer"),
            ShortDramaSceneCheckpoint.identity(10L, s, 1, scene, "桥梁", "cg", "9:16", "writer"),
            ShortDramaSceneCheckpoint.identity(10L, s, 1, scene, "桥梁", "cg", "16:9", "other-model"),
            ShortDramaSceneCheckpoint.identity(10L, s, 2, scene, "桥梁", "cg", "16:9", "writer"),
            ShortDramaSceneCheckpoint.identity(11L, s, 1, scene, "桥梁", "cg", "16:9", "writer"))) assertNull(store.compatible(changed, assets, true));
        s.setWorldbuilding("改为现代城市"); assertNull(store.compatible(ShortDramaSceneCheckpoint.identity(10L, s, 1, scene, "桥梁", "cg", "16:9", "writer"), assets, true));
    }
    @Test void voiceOnlyCharacterIsStillAnAssetDependency() {
        var store = new ShortDramaSceneCheckpoint(new Memory()); var assets = assets(); var p = panel(); p.setCharacters(List.of()); p.setPresentCharacters(List.of());
        var id = identity("画外声场次"); store.save(id, assets, List.of(p), true, "native_validated");
        assets.characters().get(0).setIntroduction("身份已更换"); assertNull(store.compatible(id, assets, true));
    }
    @Test void unknownLocationOrAmbiguousAppearanceCannotBecomeReusable() {
        var store = new ShortDramaSceneCheckpoint(new Memory()); var assets = assets(); var p = panel();
        p.setLocation("未登记场景"); assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> store.save(identity("场次"), assets, List.of(p), true, "reviewed_import"));
        p.setLocation("修渠处"); p.getCharacters().get(0).setAppearance("不存在的服装");
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> store.save(identity("场次"), assets, List.of(p), true, "reviewed_import"));
    }
    @Test void yearPrefixedStageNamesWinBeforeExplicitNumericIndices() {
        var assets = assets(); var p = panel();
        assets.appearances().get(0).setChangeReason("2026程序员·24岁·触电前现代造型");
        var later = new ShortDramaCharacterAppearance(); later.setId(8L); later.setCharacterId(1L);
        later.setAppearanceIndex(1); later.setChangeReason("1644伤后县令造型"); assets.appearances().add(later);
        for (String name : List.of("2026程序员·24岁·触电前现代造型", "1644伤后县令造型", "[1]伤后标签", "1")) {
            p.getCharacters().get(0).setAppearance(name);
            long expected = name.startsWith("2026") ? 2L : 8L;
            assertEquals(expected, assets.referenced(List.of(p)).stream().filter(b -> b.kind().equals("appearance")).findFirst().orElseThrow().id());
        }
        p.getCharacters().get(0).setAppearance("1644不存在的阶段");
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(p)));
    }
    @Test void duplicateLiteralStageOrNumericIndexRemainsAnExplicitBindingFailure() {
        var assets = assets(); var p = panel();
        var duplicate = new ShortDramaCharacterAppearance(); duplicate.setId(8L); duplicate.setCharacterId(1L);
        duplicate.setAppearanceIndex(1); duplicate.setChangeReason("初始造型"); assets.appearances().add(duplicate);
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(p)));
        duplicate.setChangeReason("另一形象"); duplicate.setAppearanceIndex(0); p.getCharacters().get(0).setAppearance("0");
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(p)));
    }
    @Test void verboseModelDescriptionCanResolveOnlyOneStronglyDistinctAppearance() {
        var assets = assets(); var p = panel();
        var modern = assets.appearances().get(0);
        modern.setChangeReason("2026程序员（穿越前专用）");
        modern.setDescription("偏长五角形脸，内收深棕杏眼，直鼻窄翼与利落非尖下颌；乌黑短发整洁微蓬；穿深灰蓝轻薄连帽外套、雾蓝圆领衫、炭灰长裤与黑色运动鞋，左腕智能手表。");
        var ancient = new ShortDramaCharacterAppearance(); ancient.setId(8L); ancient.setCharacterId(1L);
        ancient.setAppearanceIndex(1); ancient.setChangeReason("明代白衣常服");
        ancient.setDescription("偏长五角形脸，内收深棕杏眼，直鼻窄翼与利落非尖下颌；乌黑长发高束；穿月白直身和雾白宽袖罩衫，青玉发冠与白玉佩。");
        assets.appearances().add(ancient);
        p.getCharacters().get(0).setAppearance("2026程序员形象：偏长五角形脸、内收深棕杏眼、直鼻窄翼与利落非尖下颌，乌黑短发整洁微蓬；穿深灰蓝轻薄连帽外套、雾蓝圆领衫、炭灰长裤与黑色运动鞋，左腕智能手表");
        assertEquals(2L, assets.referenced(List.of(p)).stream().filter(b -> b.kind().equals("appearance")).findFirst().orElseThrow().id());
        p.getCharacters().get(0).setAppearance("偏长五角形脸、内收深棕杏眼、直鼻窄翼与利落非尖下颌");
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.referenced(List.of(p)));
    }
    @Test void draftsAreSeparateFromValidatedProofAndCorruptReceiptsAreIgnored() {
        var memory = new Memory(); var store = new ShortDramaSceneCheckpoint(memory); var assets = assets(); var id = identity("场次");
        store.save(id, assets, List.of(panel()), false, "native_draft"); assertNull(store.compatible(id, assets, true)); assertNotNull(store.compatible(id, assets, false));
        store.save(id, assets, List.of(panel()), true, "native_validated"); assertNotNull(store.compatible(id, assets, true));
        memory.values.put(id.prefix() + "receipts", "not json"); assertNull(store.compatible(id, assets, true));
    }
    @Test void shotMinimumIsPartOfProofAndWorldbuildingCannotAddAnAbsentCastMember() {
        var store = new ShortDramaSceneCheckpoint(new Memory()); var assets = assets(); var id = identity("工匠修渠");
        store.save(id, assets, List.of(panel()), true, "native_validated");
        assertNull(store.compatible(ShortDramaSceneCheckpoint.identity(10L, script(), 1, "工匠修渠", "", "chinese-3d", "16:9", "writer", 4), assets, true));
        var ghost = new ShortDramaCharacter(); ghost.setId(10L); ghost.setName("精灵"); assets.characters().add(ghost);
        var p = panel(); p.setPresentCharacters(List.of("工匠", "精灵"));
        var error = assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class, () -> assets.validateSceneCast("工匠在修渠处验水", "", List.of(p)));
        assertTrue(error.getMessage().contains("精灵")); assertTrue(error.getMessage().contains("镜头1"));
    }
    @Test void explicitSourceAliasPermitsAnActorAlongsideNamedActorsButNotAnUnrelatedOne() {
        var assets = assets();
        var farmer = new ShortDramaCharacter(); farmer.setId(11L); farmer.setName("老农户"); farmer.setAliases("老把式,老汉");
        assets.characters().add(farmer);
        var p = panel(); p.setPresentCharacters(List.of("工匠", "老农户"));
        assertDoesNotThrow(() -> assets.validateSceneCast("工匠让排首的老汉先试水", "", List.of(p)));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("工匠独自试水，他念叨：「老汉昨天来过。」", "", List.of(p)));
    }
    @Test void ScriptDescriptiveAliasIsExplicitAndSinglePronounCannotAddAnActor() {
        var assets = assets();
        var cg = new ShortDramaCharacter(); cg.setId(12L); cg.setName("数字助手"); cg.setAliases("青玉色半透明立方体,它");
        assets.characters().add(cg);
        var p = panel(); p.setPresentCharacters(List.of("工匠", "数字助手"));
        assertDoesNotThrow(() -> assets.validateSceneCast("工匠身旁悬着青玉色半透明立方体", "", List.of(p)));
        assertThrows(ShortDramaSceneCheckpoint.BindingMismatch.class,
            () -> assets.validateSceneCast("工匠试水，它流进支渠", "", List.of(p)));
    }
}
