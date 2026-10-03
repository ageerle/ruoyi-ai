package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.constant.ShortDramaImageConstants;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaCharacterArtPromptTest {
    @Test void explicitPortraitDoesNotBecomeFullBodyOrMultiViewSheet() {
        String description = "单幅头肩面部母版，24岁男子，完整乌纱帽，脸高占画面55%，双眼均在焦内";
        String prompt = ShortDramaCharacterArtPrompt.reference(description, "custom-skill");
        assertTrue(prompt.contains("【本张构图优先】"));
        assertTrue(prompt.contains(description));
        assertFalse(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_PREFIX));
        assertFalse(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX));
        assertFalse(prompt.contains("脸部特写与三个全身视角必须"));
        assertFalse(prompt.contains("单幅身份定妆图：一个完整全身人物"));
    }

    @Test void FaceDetailInOrdinaryDescriptionDoesNotSelectPortraitMode() {
        String prompt = ShortDramaCharacterArtPrompt.reference("单幅角色定妆，成年男子，面部五官清楚，完整全身", "custom-skill");
        assertTrue(prompt.contains("单幅身份定妆图：一个完整全身人物"));
        assertFalse(prompt.contains("【本张构图优先】"));
    }

    @Test void portraitRevisionPreservesRequestedIdentityAndWardrobeConstraints() {
        String prompt = ShortDramaCharacterArtPrompt.reference("单张面部肖像，左颊旧疤", "custom-skill",
            "identity_revision", "保留左颊旧疤，改为闭合圆领");
        assertTrue(prompt.contains("参考图只绑定脸部身份"));
        assertTrue(prompt.contains("保留左颊旧疤"));
        assertTrue(prompt.contains("左右领缘不交叉"));
        assertFalse(prompt.contains("脸部特写与三个全身视角必须"));
    }
    @Test void explicitRevisionBindsIdentityAndDefinesRequestedRoundNeckGeometry() {
        String prompt = ShortDramaCharacterArtPrompt.reference("成年男子，真人写真肤质，交领旧服", "chinese-3d",
            "identity_revision", "保持脸型与年龄，重建动画材质，衣领改为闭合圆领，保留左侧纽扣");
        assertTrue(prompt.contains("参考图只绑定脸部身份"));
        assertTrue(prompt.contains("不绑定参考图的摄影皮肤材质"));
        assertTrue(prompt.contains("本轮指定修订项以以下要求为准"));
        assertTrue(prompt.contains("左右领缘不交叉"));
        assertTrue(prompt.contains("前胸没有斜向重叠的V形交领线"));
        assertTrue(prompt.contains("保留左侧纽扣"));
        assertTrue(prompt.contains("各视角采用本轮修订后的同一服装裁片"));
        assertTrue(prompt.contains("尚未批准连续性"));
    }

    @Test void projectMaterialsOverrideStalePhotoSkinWithoutInventingWardrobeRevision() {
        String prompt = ShortDramaCharacterArtPrompt.reference("成年女子，细微毛孔，真人写真，蓝色夹克", "chinese-3d");
        assertTrue(prompt.contains("【材质优先级】"));
        assertTrue(prompt.contains("这些仅可提供脸部比例和年龄线索"));
        assertTrue(prompt.contains("同一服装裁片、同一配饰"));
        assertFalse(prompt.contains("【用途：身份绑定的修订候选"));
        assertFalse(prompt.contains("【圆领可见几何】"));
        assertEquals(prompt, ShortDramaCharacterArtPrompt.reference("成年女子，细微毛孔，真人写真，蓝色夹克", "chinese-3d", "identity", null));
    }

    @Test void revisionIntentMustBeExplicitAndNonempty() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaCharacterArtPrompt.reference("男子", "chinese-3d", "identity", "换衣领"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaCharacterArtPrompt.reference("男子", "chinese-3d", "identity_revision", " "));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaCharacterArtPrompt.reference("男子", "chinese-3d", "anything", "换衣领"));
    }

    @Test void nonHumanRevisionUsesCoreShapeIdentityWithoutHumanFaceBinding() {
        String prompt = ShortDramaCharacterArtPrompt.reference("主体没有人类五官。椭圆晶体", "chinese-3d",
            "identity_revision", "保持椭圆轮廓，表面改为半透明玉质");
        assertTrue(prompt.contains("参考图只绑定主体的核心身份"));
        assertTrue(prompt.contains("半透明玉质"));
        assertFalse(prompt.contains("参考图只绑定脸部身份"));
    }
    @Test void humanThreeDimensionalReferenceUsesAnimationMaterialsAndKeepsIdentity() {
        String description = "二十四岁男子，石青圆领官服，黑色官帽，左腕旧布条，右手无伤";
        String prompt = ShortDramaCharacterArtPrompt.reference(description, "chinese-3d");
        assertTrue(prompt.contains("【三维动画身份参照材质】"));
        assertTrue(prompt.contains("CG发束和毛发、PBR布料"));
        assertTrue(prompt.contains("受控次表面散射"));
        assertTrue(prompt.contains("不出现摄影毛孔或真人实拍肤质"));
        assertFalse(prompt.contains("脸部、毛发和衣料细节用该画风的线条与上色表达"));
        assertTrue(prompt.contains(description));
        assertTrue(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_PREFIX));
        assertTrue(prompt.contains("脸部特写与三个全身视角必须同一张脸"));
        assertTrue(prompt.contains("[skill:character-art-direction@"));
        assertTrue(prompt.length() < 4000, "media prompt should not embed complete skill handbooks");
    }

    @Test void explicitVirtualNonHumanHasNeutralShapeViewsWithoutHumanCasting() {
        String description = "无常规人类五官、躯体与服饰。悬浮的规整立方体，半透明浅青材质，内部有细弱流光，相对手掌略小，侧面有一条银色细纹";
        String prompt = ShortDramaCharacterArtPrompt.reference(description, "chinese-3d");
        assertTrue(prompt.contains("【用途：非人形主体形态参照图，不是剧情首帧】"));
        assertTrue(prompt.contains("单一主体的中性背景多角度形态参照"));
        assertTrue(prompt.contains("CG造型和PBR材质"));
        assertTrue(prompt.contains(description));
        assertTrue(prompt.contains("相对尺度"));
        assertTrue(prompt.contains("已有视觉锚"));
        assertTrue(prompt.contains("不添加其他角色、道具、环境或剧情表演"));
        assertTrue(prompt.contains("[skill:visual-world@"));
        assertFalse(prompt.contains("[skill:character-art-direction@"));
        assertFalse(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_PREFIX));
        assertFalse(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX));
        assertFalse(prompt.contains("脸部特写与三个全身视角"));
        assertFalse(prompt.contains("自然东方面部结构与身体比例"));
        assertFalse(prompt.contains("同一发际线"));
    }

    @Test void explicitAnatomyStatementAfterSubjectDescriptionAlsoSelectsShapeReference() {
        String description = "悬浮的椭圆晶体，主体不具备人类躯体，边缘柔和发光";
        assertTrue(ShortDramaCharacterArtPrompt.reference(description, "chinese-3d")
            .contains("非人形主体形态参照图"));
    }

    @Test void AnimalMotifAndItsAnatomyDoNotTurnTheWearerIntoNonHumanSubject() {
        String description = "中年男子，衣服上有动物图案，图案没有人类五官，黑色短发";
        String prompt = ShortDramaCharacterArtPrompt.reference(description, "chinese-3d");
        assertTrue(prompt.contains("【三维动画身份参照材质】"));
        assertTrue(prompt.contains(ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX));
        assertFalse(prompt.contains("非人形主体形态参照图"));
    }

    @Test void realisticAndTwoDimensionalHumanRulesRemainAvailable() {
        String description = "五十二岁男子，左腕浅灰旧布，右手无伤，头发正常维护";
        String realistic = ShortDramaCharacterArtPrompt.reference(description, "realistic");
        assertTrue(realistic.contains("【写实身份参照材质】自然未磨皮的成人肤质"));
        assertTrue(realistic.contains("细微毛孔、正常细小汗毛和年龄痕迹"));
        assertTrue(realistic.contains(description));
        assertTrue(realistic.contains(ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX));
        assertTrue(realistic.contains("[skill:character-art-direction@"));
        assertEquals(realistic, ShortDramaCharacterArtPrompt.reference(description, null));
        String comic = ShortDramaCharacterArtPrompt.reference(description, "chinese-comic");
        assertTrue(comic.contains("Chinese donghua 2D comic style"));
        assertTrue(comic.contains("脸部、毛发和衣料细节用该画风的线条与上色表达"));
        assertTrue(comic.contains("不混入真人肤质或摄影景深"));
    }
}
