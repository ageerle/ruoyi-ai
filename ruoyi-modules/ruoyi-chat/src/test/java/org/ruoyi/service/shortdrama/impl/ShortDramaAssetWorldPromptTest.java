package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaAssetWorldPromptTest {
    @Test void locationNameCarriesTimeEvenWhenDescriptionOmitsIt() {
        String prompt = ShortDramaAssetWorldPrompt.location("城市租屋_2028年深夜", "室内木桌与窗、电脑", "chinese-3d", "故事包含现代与前现代时点");
        assertTrue(prompt.contains("城市租屋_2028年深夜"));
        assertTrue(prompt.contains("名称中的年代、季候与昼夜是当前图的时间约束"));
        assertTrue(prompt.contains("不把深夜画成白昼窗景"));
        assertTrue(prompt.contains("室内木桌与窗、电脑"));
    }

    @Test void oldOverloadDoesNotInventANameOrDate() {
        String prompt = ShortDramaAssetWorldPrompt.location("普通屋舍", "chinese-3d", "当前世界");
        assertFalse(prompt.contains("【本张场景名称与时点】"));
        assertTrue(prompt.contains("普通屋舍"));
    }
    @Test void currentAssetStateIsSeparateFromFutureWorldPlans() {
        String world = "前现代小镇，当前尚在试种；未来拟修通商港口，尚未建造。";
        String description = "窄河岸临时渡口，仅有木栈桥与两条小船，远处低矮屋舍。";
        String prompt = ShortDramaAssetWorldPrompt.location(description, "chinese-3d", world);
        assertTrue(prompt.contains(world));
        assertTrue(prompt.contains(description));
        assertTrue(prompt.indexOf(world) < prompt.indexOf(description));
        assertTrue(prompt.contains("不是本图物件清单"));
        assertTrue(prompt.contains("未写已建成、已成熟或已引进的内容保持未发生"));
        assertFalse(prompt.contains("宽广空间全景"));
        assertTrue(prompt.contains("巨大水面"));
    }

    @Test void missingWorldDoesNotInventAnEraOrADevelopmentStage() {
        String prompt = ShortDramaAssetWorldPrompt.location("现代植物实验室，透明塑料容器与标准育苗盘", "realistic", null);
        assertTrue(prompt.contains("现代植物实验室，透明塑料容器与标准育苗盘"));
        assertFalse(prompt.contains("【当前项目世界观："));
        assertTrue(prompt.contains("前现代时点且没有明确的已引进依据时"));
        assertFalse(prompt.contains("风格化CG雕塑造型"));
        assertTrue(prompt.contains("默认已批准参考的身份与衣物仍保持"));
    }

    @Test void characterWorldScopeKeepsApprovedIdentityAndExplicitRevisionPriority() {
        String world = "故事处于前现代，蓝色衣物使用天然染料，手工缝制。";
        String identity = ShortDramaCharacterArtPrompt.reference("四十岁女子，圆脸，蓝色袍服", "chinese-3d", null, null, world);
        assertTrue(identity.contains(world));
        assertTrue(identity.contains("四十岁女子，圆脸，蓝色袍服"));
        assertTrue(identity.contains("默认已批准参考的身份与衣物仍保持"));
        assertFalse(identity.contains("【用途：身份绑定的修订候选"));
        String revision = ShortDramaCharacterArtPrompt.reference("四十岁女子，圆脸，蓝色袍服", "chinese-3d", "identity_revision",
            "只修正布料织理，衣物裁片与脸部身份保持", world);
        assertTrue(revision.endsWith("须人工审阅选择后才能成为后续镜头的批准连续性参考。"));
        assertTrue(revision.contains("只修正布料织理，衣物裁片与脸部身份保持"));
    }
}
