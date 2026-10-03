package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaAssetAestheticsTest {
    @Test void explicitlyRequestedSingleIdentityViewDoesNotRemoveTheDefaultMultiViewOption() {
        String single = ShortDramaCharacterArtPrompt.reference("单幅角色定妆图，单人3/4全身，24岁青年县令", "chinese-3d");
        assertTrue(single.contains("本张只画一个当前身份"));
        assertFalse(single.contains("character design sheet, multiple views"));
        assertFalse(single.contains("【左侧区域】"));
        String sheet = ShortDramaCharacterArtPrompt.reference("24岁青年县令", "chinese-3d");
        assertTrue(sheet.contains("【左侧区域】"));
        assertTrue(sheet.contains("三个全身视角"));
    }
    @Test void charactersAndSpacesLoadTheSameVersionWithDifferentResponsibilities() {
        String character = ShortDramaCharacterArtPrompt.reference("2026年，24岁程序员，黑色外套", "chinese-3d", null, null, "当代县城");
        String location = ShortDramaAssetWorldPrompt.location("现代出租屋_深夜", "十平方米，单扇窗，木桌", "chinese-3d", "当代县城");
        assertTrue(character.contains("[asset-aesthetics:" + ShortDramaAssetAesthetics.version() + "]"));
        assertTrue(location.contains("[asset-aesthetics:" + ShortDramaAssetAesthetics.version() + "]"));
        assertTrue(character.contains("人物身份参照："));
        assertTrue(location.contains("场景空间参照："));
        assertFalse(location.contains("人物身份参照："));
        assertTrue(character.contains("2026年，24岁程序员，黑色外套"));
        assertTrue(location.contains("十平方米，单扇窗，木桌"));
        assertTrue(location.contains("不扩大贫困或建设初期地点"));
    }

    @Test void aestheticsDoesNotTurnAnApprovedFirstFrameIntoAModelSheetOrChangeItsPopulation() {
        var shot = new ShortDramaStoryboard();
        shot.setShotType("中景");
        shot.setImagePrompt("青灰棉麻纹理，侧光");
        shot.setCharactersJson("[{\"name\":\"朱承晏\",\"slot\":\"左侧\"}]");
        shot.setContinuityJson("{\"start_state\":\"朱承晏坐在木桌左侧，右手持铜书签\",\"present_characters\":[\"朱承晏\"],\"background_extras\":\"0秒：可见人数=2；身份服饰=匿名百姓，粗布衣；位置=门外；姿态动作=静候\"}");
        String prompt = ShortDramaVisualAssetService.framePrompt(shot);
        assertTrue(prompt.contains("实际镜头首帧："));
        assertTrue(prompt.contains("不套用多视角定妆图"));
        assertTrue(prompt.contains("朱承晏坐在木桌左侧，右手持铜书签"));
        assertTrue(prompt.contains("可见人数=2"));
        assertTrue(prompt.contains("匿名百姓，粗布衣"));
        assertTrue(prompt.contains("青灰棉麻纹理"));
    }

    @Test void nonHumanArtAndPropsRemainTheirOwnAssetsWithoutHumanAnatomyOrLiveActionDefaults() {
        String nonHuman = ShortDramaCharacterArtPrompt.reference("无人类五官；八厘米青玉立方体", "chinese-3d");
        assertTrue(nonHuman.contains("非人形主体按自身几何与材质审美"));
        assertTrue(nonHuman.contains("非人形主体形态参照图"));
        assertTrue(nonHuman.contains("不自行增添人体结构"));
        String prop = ShortDramaAssetAesthetics.propReference("chinese-3d", "1639年，贫困小县，以木、铁与粗布为主");
        assertTrue(prop.contains("物品形制参照："));
        assertTrue(prop.contains("三维动画"));
        assertTrue(prop.contains("贫困小县"));
        assertFalse(prop.contains("写实电影道具定妆图"));
        assertTrue(prop.contains("不额外增加人物"));
    }
}
