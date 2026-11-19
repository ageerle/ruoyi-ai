package org.ruoyi.chat.service.coding.harness.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class HarnessSkillTools {

    private final HarnessSkillCatalog catalog;

    public HarnessSkillTools(HarnessSkillCatalog catalog) {
        this.catalog = catalog;
    }

    @Tool(name = "activate_skill", value = {
        "Load the full instructions for one available skill. Call only with an exact listed skill name."
    })
    public String activateSkill(@P(name = "name", description = "Exact available skill name") String name) {
        HarnessSkillDefinition skill = catalog.activate(name);
        return "# Skill: " + skill.name() + "\n\n" + skill.content();
    }

    @Tool(name = "read_skill_resource", value = {
        "Read one resource declared by an activated skill using a relative path."
    })
    public String readSkillResource(
        @P(name = "skillName", description = "Exact activated skill name") String skillName,
        @P(name = "relativePath", description = "Relative resource path") String relativePath) {
        return catalog.readResource(skillName, relativePath);
    }
}
