package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.StrUtil;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;

/** Project-specific evidence and world rules, kept separate from screenplay action. */
public final class ShortDramaScriptPreparation {
    private ShortDramaScriptPreparation() {}

    public static String worldContext(ShortDramaScript script) {
        return (StrUtil.isBlank(script.getTone()) ? "" : "\n【剧本审阅中维护的风格与基调】\n" + script.getTone() + "\n按当前剧本维护的方向处理角色、场景、材质、光色和表演；具体已批准素材的身份与时点继续保留。\n")
            + (StrUtil.isBlank(script.getWorldbuilding()) ? "" : "\n【当前项目世界观，人物与场景必须遵守】\n" + script.getWorldbuilding());
    }
}
