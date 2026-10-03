package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.constant.ShortDramaImageConstants;

/** World rules constrain an asset's era/materials; they are not a list of things to add to its image. */
final class ShortDramaAssetWorldPrompt {
    private ShortDramaAssetWorldPrompt() {}

    static String context(String worldbuilding) {
        if (worldbuilding == null || worldbuilding.isBlank()) return "";
        return "\n【当前项目世界观：年代、材料与发展阶段上下文】\n" + worldbuilding
            + "\n【上下文使用边界】以上提供年代、材料可获得性、资源规模与阶段因果约束，不是本图物件清单。"
            + "只画下面本张资产描述确定的当前时刻；世界观中的后续发展、未来装备、其他年代人物与计划成果不自动进入本图。"
            + "本张描述决定当前可见物件与阶段，未写已建成、已成熟或已引进的内容保持未发生，不从总纲补入。\n";
    }

    static String style(String artStyle) {
        String style = artStyle == null ? ShortDramaImageConstants.DEFAULT_ART_STYLE : artStyle;
        return ShortDramaImageConstants.artStylePrompt(style) + ("chinese-3d".equals(style)
            ? "\n【可见三维动画造型】明确可辨的风格化CG雕塑造型，轮廓与体块有设计，清晰体面、受控色面和材质层次，动画电影的三维布景与角色渲染；"
                + "保持描述中的身份比例与年龄，不靠夸大五官改脸。发束成组设计，布料细节服从CG造型，皮肤使用受控色阶与克制散射；"
                + "不用仿真人数字人的摄影皮肤、逐根写真发丝或真人棚拍来假扮三维动画，也不变成二维平涂、塑料玩偶。\n"
            : "");
    }

    static String currentStateRules() {
        return "\n【本张年代与当前状态】服装、建筑和物件按本张所处年代及明确剧情依据实现；世界观若包含多个年代，不混用其他时点的服装和材料。"
            + "前现代时点且没有明确的已引进依据时，不添现代塑料薄膜、塑料育苗盘、工业玻璃棚或西式翻领衬衫。"
            + "贫困或建设初期的空间保持描述中的小规模与未完成状态，不为了电影感增加繁华都市、巨大水面、豪华铺装或已竣工的壮阔城防。"
            + "作物种类、生长阶段、叶片数量和密度遵从本张描述；苗床、幼苗与初次试种不能画成成熟稻穗、满田丰收或提前长成的作物。"
            + "默认已批准参考的身份与衣物仍保持；发现其与时代约束冲突须明确修订，不以年代规则擅自换脸换衣。\n";
    }

    static String location(String description, String artStyle, String worldbuilding) {
        return location(null, description, artStyle, worldbuilding);
    }

    static String location(String name, String description, String artStyle, String worldbuilding) {
        String timeAndPlace = name == null || name.isBlank() ? "" : "【本张场景名称与时点】\n" + name
            + "\n名称中的年代、季候与昼夜是当前图的时间约束，描述中未重复时也保留；不把深夜画成白昼窗景，不从名称猜测额外建筑或陈设。\n";
        return ShortDramaDirectorSkills.media("visual-world")
            + ShortDramaAssetAesthetics.locationReference()
            + "【用途：当前阶段的无人物空间参照图】\n" + style(artStyle) + context(worldbuilding)
            + timeAndPlace
            + "【本张场景实际描述】\n" + (description == null ? "" : description)
            + "\n按上述场景实际规模建立完整空间构图，入口、窗、工作区和主要陈设清楚；不靠扩大城镇、河湖或建筑尺度制造宏大感。"
            + ShortDramaImageConstants.LOCATION_PROMPT_SUFFIX + "。牌匾、城名、招牌与可读文字不凭空创造，需精确文字留给后期。"
            + currentStateRules();
    }
}
