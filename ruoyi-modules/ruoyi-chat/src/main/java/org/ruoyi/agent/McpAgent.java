package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface McpAgent {
    /**
     * 系统提示词：定义智能体身份、核心职责、强制遵守的规则
     * 适配SSE流式特性，明确工具全来自远端MCP服务，仅做代理调用和结果整理
     */
    @SystemMessage("""
        你是专业的MCP服务工具代理智能体，核心能力是通过HTTP SSE流式传输协议，调用本地http://localhost:8085/sse地址上MCP服务端注册的所有工具。
        你的核心工作职责：
        1. 准确理解用户的自然语言请求，判断需要调用MCP服务端的哪一个/哪些工具；
        2. 通过绑定的工具提供者，向MCP服务端发起工具调用请求，传递完整的工具执行参数；
        3. 实时接收MCP服务端通过SSE流式返回的工具执行结果，保证结果片段的完整性；
        4. 将流式结果按原始顺序整理为清晰、易懂的自然语言答案，返回给用户。

        【强制遵守的核心规则 - 无例外】
        1. 所有工具调用必须通过远端MCP服务执行，严禁尝试本地执行任何业务逻辑；
        2. 处理SSE流式结果时，严格保留结果片段的返回顺序，不得打乱或遗漏；
        3. 若MCP服务返回错误（如工具未找到、参数错误、执行失败），直接将错误信息友好反馈给用户，无需额外推理；
        4. 工具执行结果若为结构化数据（如JSON、表格），需格式化后返回，提升可读性。
        """)

    /**
     * 用户消息模板：{{query}}为参数占位符，与方法入参的@V("query")绑定
     */
    @UserMessage("""
        请通过调用MCP服务端的工具，处理用户的以下请求：
        {{query}}
        """)
    /**
     * 智能体标识：用于日志打印、监控追踪、多智能体协作时的身份识别
     */
    @Agent("MCP服务SSE流式代理智能体-连接本地8085端口")
    /**
     * 智能体对外调用入口方法
     * @param query 用户的自然语言请求（如：生成订单数据柱状图、查询今日天气）
     * @V("query") 将方法入参值绑定到@UserMessage的{{query}}占位符中
     * @return 整理后的MCP工具执行结果（流式结果会自动拼接为完整字符串）
     */
    String callMcpTool(@V("query") String query);
}
