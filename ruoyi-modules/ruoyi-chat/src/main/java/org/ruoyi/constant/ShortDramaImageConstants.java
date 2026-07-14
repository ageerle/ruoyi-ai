package org.ruoyi.constant;

/**
 * 短剧图片资产常量 — 三视图 prompt 工程
 *
 * @author ruoyi
 */
public final class ShortDramaImageConstants {

    private ShortDramaImageConstants() {}

    /**
     * 角色三视图 prompt 前缀，生图时自动拼到用户 prompt 之前。
     * 把风格和构图指令放最前面，避免被角色描述稀释导致画风漂移。
     */
    public static final String CHARACTER_PROMPT_PREFIX =
        "character design sheet, multiple views reference sheet, " +
        "front view / side view / back view full body, " +
        "clean white background, no props, no text. ";

    /**
     * 角色三视图 prompt 后缀，生图时自动追加到用户 prompt 之后。
     * 左侧1/3正面特写 + 右侧2/3三视图横向排列（正面全身、侧面全身、背面全身）。
     */
    public static final String CHARACTER_PROMPT_SUFFIX =
        "。角色设定图，画面分为左右两个区域：" +
        "【左侧区域】占约1/3宽度，是角色的正面特写" +
        "（完整正脸，最具辨识度的正面形态）；" +
        "【右侧区域】占约2/3宽度，是角色三视图横向排列" +
        "（从左到右依次为：正面全身、侧面全身、背面全身），" +
        "三视图高度一致。纯白色背景，无其他元素。";

    /** 场景图 prompt 前缀 */
    public static final String LOCATION_PROMPT_PREFIX = "宽广空间全景，";

    /** 场景图 prompt 后缀 */
    public static final String LOCATION_PROMPT_SUFFIX = "，禁止出现任何角色，纯背景板";

    /** 每个资产最多保留的图片变体数量 */
    public static final int MAX_IMAGE_VARIANTS = 20;

    // ==================== 视觉风格 ====================

    /** 项目视觉风格 → 生图 prompt 后缀映射，确保同项目所有图片风格一致 */
    public static final java.util.Map<String, String> ART_STYLE_PROMPTS = java.util.Map.of(
        "american-comic", "美式漫画风格，粗线条，高饱和度色彩，强烈光影对比",
        "chinese-comic", "现代国漫动画风格，Chinese donghua 2D comic style，赛璐璐平涂上色，干净锐利的黑色线稿，平面化光影无真实景深，动漫人物比例（略放大双眼、修长身形），皮肤平滑无毛孔无写实肤质，国风服饰剪裁与材质细节清晰，色彩饱满通透，画面精致干净；禁止真人写实、摄影实拍、3D渲染、CGI、厚涂油画、写实皮肤纹理、景深虚化",
        "japanese-anime", "现代日系动漫风格，赛璐璐上色，清晰干净的线条，视觉小说CG感，高质量2D风格",
        "realistic", "真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感"
    );

    /** 默认视觉风格 */
    public static final String DEFAULT_ART_STYLE = "realistic";

    /** 查找 artStyle 对应的 prompt 后缀，找不到返回空字符串 */
    public static String artStylePrompt(String artStyle) {
        if (artStyle == null) return "";
        return ART_STYLE_PROMPTS.getOrDefault(artStyle, "");
    }

    /**
     * 由项目 id 派生一个稳定的生图随机种子。
     * 同项目内所有角色/形象共用同一颗种子，渲染基调（光影、配色、笔触）更趋一致；
     * 不同项目派生不同种子，避免跨项目撞图。返回值落在 [0, 2_000_000_000)，兼容各供应商。
     */
    public static Integer styleSeed(Long projectId) {
        if (projectId == null) return null;
        long h = projectId;
        h ^= (h >>> 32);
        return (int) Math.floorMod(h * 2654435761L, 2_000_000_000L);
    }
}
