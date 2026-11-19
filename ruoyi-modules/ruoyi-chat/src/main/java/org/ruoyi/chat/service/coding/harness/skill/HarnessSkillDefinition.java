package org.ruoyi.chat.service.coding.harness.skill;

import java.nio.file.Path;
import java.util.Map;

public record HarnessSkillDefinition(
    String name,
    String description,
    String content,
    String source,
    Path basePath,
    Map<String, String> bundledResources
) {
    public HarnessSkillDefinition {
        bundledResources = bundledResources == null ? Map.of() : Map.copyOf(bundledResources);
        if (name == null || name.isBlank() || description == null || description.isBlank()
            || content == null || source == null || source.isBlank()) {
            throw new IllegalArgumentException("Invalid Harness skill definition");
        }
    }
}
