package org.ruoyi.chat.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 使用当前智能体关联的技能，技能清单在每次对话时注入。 */
public interface SkillsAgent {

    @UserMessage("{{query}}")
    @Agent(name = "skillsAgent", description = "根据已关联的技能处理用户任务")
    String process(@V("query") String query);
}
