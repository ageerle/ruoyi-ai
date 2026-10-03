package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.constant.ShortDramaImageConstants;

/** Shared casting-reference rules for synchronous, asynchronous and edit image requests. */
final class ShortDramaCharacterArtPrompt {
    // Only an explicit statement about the subject's anatomy selects this branch; decorative animal motifs do not.
    private static final java.util.regex.Pattern NON_HUMAN_SUBJECT = java.util.regex.Pattern.compile(
        "(?:^|[。；;，,：:\\n])\\s*(?:(?:主体|本体|该角色|其)(?:明确)?(?:为|是)?\\s*)?"
            + "(?:(?:无|没有|不具备|不具有|不含)(?:常规)?(?:人类|人形)(?:的)?(?:五官|躯体|身体)"
            + "|(?:外观|形态|主体)(?:明确)?(?:为|是|呈)?非人形|非人形(?:虚拟)?(?:主体|角色|实体))");

    private ShortDramaCharacterArtPrompt() {}

    static String reference(String description, String artStyle) {
        return reference(description, artStyle, null, null);
    }

    /** An explicit revision uses the supplied image for identity, not for the rejected wardrobe/material. */
    static String reference(String description, String artStyle, String referencePurpose, String revisionRequirements) {
        return reference(description, artStyle, referencePurpose, revisionRequirements, null);
    }

    static String reference(String description, String artStyle, String referencePurpose, String revisionRequirements, String worldbuilding) {
        boolean revision = isRevision(referencePurpose, revisionRequirements);
        String style = artStyle == null ? ShortDramaImageConstants.DEFAULT_ART_STYLE : artStyle;
        String subjectDescription = description == null ? "" : description;
        // Portraits are independent casting masters, not a small inset in a full-body sheet.
        boolean portraitReference = subjectDescription.matches("(?s).*(?:单幅|单张)(?:头肩|面部|脸部|胸像)(?:面部)?(?:母版|定妆|肖像|特写).*" );
        boolean singleReference = subjectDescription.matches("(?s).*(?:单幅(?:角色|人物|定妆|全身)|单张(?:角色|人物|定妆|全身)|单人\\s*3/4\\s*全身|(?:不要|不做|不用)三视图).*" );
        if (NON_HUMAN_SUBJECT.matcher(subjectDescription).find()) {
            return nonHumanReference(subjectDescription, style, worldbuilding) + revisionRules(revision, revisionRequirements, true);
        }
        String material = switch (style) {
            case "custom-skill" -> "【风格约束】人物造型、肤质、毛发、衣物材质和光色全部服从选中审美技能正文，不从旧基础画风强加真人写真、二维平涂或国风三维预设；身份、年龄和服装裁片仍服从本张实际描述。\n";
            case "realistic" -> "【写实身份参照材质】自然未磨皮的成人肤质，细微毛孔、正常细小汗毛和年龄痕迹，眼球湿润但不过度放大，发际线与发束自然；衣料织理、接缝、厚薄和受力褶皱可辨。维护程度及磨损位置以角色描述为准，不统一加旧、加脏或新增皮肤记号。柔和中性光保留体积和肌肤中间调，避免美颜滤镜、塑料皮肤和电商假发感。\n";
            case "chinese-3d" -> "【三维动画身份参照材质】采用国风动画电影的风格化CG雕塑造型，面部有清晰的体面、轮廓和受控色阶，一看就是三维动画角色；自然东方面部结构与身体比例，CG发束和毛发、PBR布料织理与接缝，发束成组且有设计，衣纹服从建模体块，厚薄和受力褶皱可辨；受控次表面散射与柔和中性体积光塑造动画皮肤，保留立体明暗和材质层次，不出现摄影毛孔或真人实拍肤质，不使用仿真人数字人写真、逐根摄影发丝或真人棚拍，不用二维线稿或平涂表达材质，不做塑料玩偶或油亮蜡像。年龄、服装裁片、配饰及已有标志以角色描述为准，不自动美化或增加污损。\n";
            default -> "【风格约束】严格采用项目指定画风，脸部、毛发和衣料细节用该画风的线条与上色表达；不混入真人肤质或摄影景深。\n";
        };
        return ShortDramaDirectorSkills.media("visual-world", "character-art-direction")
            + ShortDramaAssetAesthetics.characterReference()
            + "【用途：身份参照图，不是剧情首帧】\n"
            + ShortDramaAssetWorldPrompt.style(style) + "\n" + material
            + ShortDramaAssetWorldPrompt.context(worldbuilding)
            + "【本张角色当前形象描述】\n"
            + (portraitReference ? "【本张构图优先】单幅头肩面部母版：只画一位人物，从完整头发帽饰至肩胸；脸部为视觉中心，双眼、眉毛、鼻唇边缘与发际线清楚，头脸占比服从本张描述。通用完整服装规则仅用于全身定妆，本张不附全身小人、不拼接三视图、不加文字标签。"
                : singleReference ? "单幅身份定妆图：一个完整全身人物，自然静态3/4姿势，完整头发帽饰与双脚在画内，中性简洁背景，清晰脸形、服装裁片及原描述中的既有配饰；不做多视角拼图，不复制成多个演员，不加文字标签。" : ShortDramaImageConstants.CHARACTER_PROMPT_PREFIX)
            + subjectDescription
            + (portraitReference || singleReference ? "。本张只画一个当前身份，不增加第二视角；脸部、年龄、发际线、衣物和左右标志按本轮描述与批准约束落实。" : ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX)
            + (portraitReference || singleReference ? "" : revision
                ? "本候选的脸部特写与三个全身视角必须同一张脸、同一发际线；各视角采用本轮修订后的同一服装裁片、配饰和左右侧标志。主角身份不自动增加颜值或服装华丽度。\n"
                : "脸部特写与三个全身视角必须同一张脸、同一发际线、同一服装裁片、同一配饰与左右侧标志。主角身份不自动增加颜值或服装华丽度。")
            + ("chinese-3d".equals(style)
                ? "\n【材质优先级】项目国风三维动画画风优先于描述或参考图里的‘写实肤质、细微毛孔、真人写真’等材质表述；这些仅可提供脸部比例和年龄线索，须重建为有体积的三维动画皮肤、CG发束与PBR布料，不复制摄影皮肤纹理。\n"
                : "")
            + ShortDramaAssetWorldPrompt.currentStateRules()
            + revisionRules(revision, revisionRequirements, false);
    }

    static boolean isRevision(String referencePurpose, String revisionRequirements) {
        String purpose = referencePurpose == null || referencePurpose.isBlank() ? "identity" : referencePurpose.trim();
        if (!"identity".equals(purpose) && !"identity_revision".equals(purpose)) {
            throw new IllegalArgumentException("不支持的参考用途: " + purpose);
        }
        boolean revision = "identity_revision".equals(purpose);
        if (revision && (revisionRequirements == null || revisionRequirements.isBlank())) {
            throw new IllegalArgumentException("修订候选必须填写明确的修订要求");
        }
        if (!revision && revisionRequirements != null && !revisionRequirements.isBlank()) {
            throw new IllegalArgumentException("修订要求须配合 referencePurpose=identity_revision 使用");
        }
        if (revisionRequirements != null && revisionRequirements.length() > 4000) {
            throw new IllegalArgumentException("修订要求不能超过4000字符");
        }
        return revision;
    }

    private static String revisionRules(boolean revision, String requirements, boolean nonHuman) {
        if (!revision) return "";
        String identity = nonHuman
            ? "参考图只绑定主体的核心身份与未指定修改的形态辨识点；材质与明确指定的结构修正按本轮要求执行，不添加人体结构。"
            : "参考图只绑定脸部身份：脸型、眉眼鼻口比例、年龄、发际线及未指定修改的辨识点；不绑定参考图的摄影皮肤材质或被本轮明确纠正的服装。";
        String roundNeck = !nonHuman && requirements.contains("圆领")
            ? "\n【圆领可见几何】本轮指定圆领时，正面领口为绕颈闭合的圆弧，左右领缘不交叉，前胸没有斜向重叠的V形交领线；用正面及侧面视角清楚展示闭合圆领的领缘和接缝。"
            : "";
        return "\n【用途：身份绑定的修订候选，尚未批准连续性】\n" + identity
            + "只修改本轮明确列出的材质、服装或结构，未指定的身份特征、配饰及左右标志仍保持。世界规范中的默认服装锁适用于已批准参考；本轮指定修订项以以下要求为准，不沿用被拒参考的旧裁片。新候选各视角保持同一修订结果。\n"
            + "【本轮明确修订要求】\n" + requirements.trim() + roundNeck
            + "\n此图仅作为待审候选，须人工审阅选择后才能成为后续镜头的批准连续性参考。";
    }

    private static String nonHumanReference(String description, String style, String worldbuilding) {
        // Human casting/makeup rules do not apply to a subject explicitly declared to have no human anatomy.
        String stylePrompt = switch (style) {
            case "chinese-3d" -> "国风电影级三维动画，Chinese donghua cinematic 3D animation；CG造型和PBR材质，克制的次表面散射、透明度和反射以描述为准，柔和中性体积光保留立体边缘与内部层次，不使用真人摄影实拍、二维赛璐璐平涂或黑色漫画线稿。";
            case "realistic" -> "影视写实的非人形主体材质参照，真实形体与表面质感，可解释的柔和中性光源和丰富中间调；材质、反射、透明度及维护状态服从描述，不增加人体结构。";
            case "chinese-comic" -> "现代国漫动画风格，Chinese donghua 2D comic style，赛璐璐平涂上色，干净锐利的黑色线稿、平面化光影与清晰形态边缘；不使用真人摄影、3D渲染或景深虚化。";
            default -> ShortDramaImageConstants.artStylePrompt(style);
        };
        return ShortDramaDirectorSkills.media("visual-world")
            + ShortDramaAssetAesthetics.characterReference()
            + "【用途：非人形主体形态参照图，不是剧情首帧】\n"
            + "世界规范中涉及人体身份与妆造的通用条款仅适用于具备相应结构的主体；本图采用以下专用形态参照规范。\n"
            + stylePrompt + "\n" + ShortDramaAssetWorldPrompt.context(worldbuilding)
            + "单一主体的中性背景多角度形态参照：主视图展示完整形态，其余视图展示同一主体的正向、侧向、背向或其他有辨识价值的角度，附一处关键材质或几何特征近景。所有角度保持同一结构、相对尺度、颜色、材质和已有视觉锚；不自行增添人体结构或拟人化表情。\n"
            + "【主体原始描述】" + description + "\n"
            + "完整保留描述中的轮廓、棱角、悬浮状态、透明度、内部纹理、发光强弱与相对尺度。纯白或浅灰中性背景，无文字标签，不添加其他角色、道具、环境或剧情表演，也不用另一人物作尺寸对照。";
    }
}
