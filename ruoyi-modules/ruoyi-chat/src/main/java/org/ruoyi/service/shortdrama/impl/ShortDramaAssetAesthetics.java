package org.ruoyi.service.shortdrama.impl;

import cn.hutool.crypto.digest.DigestUtil;

/** Shared aesthetic direction with separate identity, space, prop and actual-shot responsibilities. */
final class ShortDramaAssetAesthetics {
    private ShortDramaAssetAesthetics() {}

    private static final class Bundled {
        static final String TEXT = ShortDramaDirectorSkills.read("asset-aesthetics");
        static final String VERSION = "asset-aesthetics-v1-" + DigestUtil.sha256Hex(TEXT).substring(0, 12);
    }

    static String version() { return Bundled.VERSION; }

    private static String scope(String instruction) {
        return "\n[asset-aesthetics:" + version() + "]\n"
            + "【本次审美规范适用范围】\n" + instruction
            + "审美增强服从用户画风、当前年代、身份、年龄、发展阶段及已批准连续性；只把已有内容的造型、材质与构图做清楚，不新增人物、装备、剧情成果、皮肤记号或豪华规模。\n";
    }

    static String characterReference() {
        return scope("人物身份参照：清楚展示本张既定身份的脸形、年龄、发式和服饰裁片、层次、扣结、材质；多个视角是同一身份，不是多名演员。非人形主体按自身几何与材质审美，不套用人脸、服装或人体比例。");
    }

    static String locationReference() {
        return scope("场景空间参照：使用本张地点规模、阶段、时点和明确陈设建立可读的空间层次、建筑构造与材质。保持本张无人物参照用途，不套用人物定妆图布局，不扩大贫困或建设初期地点，也不从总世界观加入未来物件。");
    }

    static String propReference(String artStyle, String worldbuilding) {
        return scope("物品形制参照：单件已有道具，中性背景，结构、用途、接合方式、材质和尺度感清楚；只展示该物既有形制，不额外增加人物、另一种道具、装饰或可执行武器制造细节。")
            + ShortDramaAssetWorldPrompt.style(artStyle) + ShortDramaAssetWorldPrompt.context(worldbuilding)
            + ShortDramaAssetWorldPrompt.currentStateRules();
    }

    static String shotFrame() {
        return scope("实际镜头首帧：身份图锁身份、已批准外观；空间图锁几何，当前镜头决定机位、景别、光色和0秒状态。只用一幅镜头构图，不套用多视角定妆图。严格保持起始动作、原文、登记演员与匿名群演人数、CG比例、持物、出入口及局部构图指令，不能为了好看提前演结果或复制演员。");
    }
}
