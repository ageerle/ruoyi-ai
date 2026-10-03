package org.ruoyi.service.shortdrama.impl;

import java.util.ArrayList;
import java.util.List;

/** Identity/geometry and visual style have distinct roles in the provider's ordered images. */
final class ShortDramaImageStyleReferences {
    static List<String> ordered(String primary, List<String> styles) {
        if (styles == null || styles.isEmpty()) return primary == null ? List.of() : List.of(primary);
        if (styles.size() > 3) throw new IllegalArgumentException("最多绑定3张风格参考图");
        if (primary == null || primary.isBlank()) throw new IllegalArgumentException("风格参考须同时提供身份或空间参考图");
        var result = new ArrayList<String>(); result.add(primary);
        for (String style : styles) {
            if (style == null || style.isBlank()) throw new IllegalArgumentException("风格参考图不能为空");
            if (!result.contains(style)) result.add(style);
        }
        if (result.size() == 1) throw new IllegalArgumentException("风格参考须与身份或空间参考图区分");
        return List.copyOf(result);
    }

    static String direction(boolean location, int count) {
        return "\n【有序参考图用途】第1张仅用于" + (location ? "本场景的门墙、出入口和方位" : "本角色的身份、年龄、服装与配色")
            + "；第2至" + count + "张仅用于影视画面中的光色、明暗层次、表面质感与摄影呈现。"
            + "将后者的视觉处理用到第1张的内容，不复制后者的人脸、衣装、建筑、道具或构图，输出一个连续单幅。";
    }
}
