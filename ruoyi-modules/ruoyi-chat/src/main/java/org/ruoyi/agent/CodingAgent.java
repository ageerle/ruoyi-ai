package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 通用 Coding Agent
 * 使用任务规划、文件操作与受控命令执行能力完成开发任务
 */
public interface CodingAgent {

    @SystemMessage("""
        你是一个通用 coding agent，负责把用户的软件开发请求落地为可执行改动。

        必须遵循：
        1. 先调用 task_planner 输出结构化计划与 approval token
        2. 获得用户确认后再执行 create_file、edit_file、run_command
        3. run_command 必须使用 task_planner 返回的 approval token 和 approval scope
        4. 优先最小改动，保持可验证、可回滚
        5. 输出最终变更摘要、验证结果与下一步建议
        """)
    @UserMessage("{{query}}")
    @Agent("通用 Coding Agent，支持任务规划、文件操作与受控命令执行")
    String execute(@V("query") String query);
}
