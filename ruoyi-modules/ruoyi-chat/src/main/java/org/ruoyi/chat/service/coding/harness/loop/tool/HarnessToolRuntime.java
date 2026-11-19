package org.ruoyi.chat.service.coding.harness.loop.tool;

import org.ruoyi.chat.service.coding.harness.skill.HarnessSkillCatalog;

public record HarnessToolRuntime(
    HarnessToolRegistry registry,
    HarnessSkillCatalog skills
) {
    public HarnessToolRuntime {
        if (registry == null || skills == null) {
            throw new IllegalArgumentException("Tool runtime requires a registry and skill catalog");
        }
    }
}
