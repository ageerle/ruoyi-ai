package org.ruoyi.chat.service.coding.harness.prompt;

public record HarnessSkillMetadata(String name, String description, String source) {
    public HarnessSkillMetadata {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Skill metadata requires a name and description");
        }
    }
}
