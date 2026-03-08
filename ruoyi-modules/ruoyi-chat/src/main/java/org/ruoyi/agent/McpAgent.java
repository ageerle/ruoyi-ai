package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface McpAgent extends Agent {

    /**
     * 系统提示词：通用工具调用智能体
     * 不限定具体工具类型，让 LangChain4j 自动传递工具描述给 LLM
     */
    @SystemMessage("""
        你是一个AI助手，可以通过调用各种工具来帮助用户完成不同的任务。

        【工具使用规则】
        1. 根据用户的请求，判断需要使用哪些工具
        2. 仔细阅读每个工具的描述，确保理解工具的功能和参数要求
        3. 使用正确的参数调用工具
        4. 如果工具执行失败，向用户友好地说明错误原因，并尝试提供替代方案
        5. 对于复杂任务，可以分步骤使用多个工具完成
        6. 将工具执行结果以清晰易懂的方式呈现给用户

        【响应格式】
        - 直接回答用户的问题
        - 如果使用了工具，说明使用了什么工具以及结果
        - 如果遇到错误，提供友好的错误信息和解决建议
        """)

    @UserMessage("""
        {{query}}
        """)

    @Agent("通用工具调用智能体")
    /**
     * 智能体对外调用入口
     * @param query 用户的自然语言请求
     * @return 处理结果
     */
    String callMcpTool(@V("query") String query);
}
