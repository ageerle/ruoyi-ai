package org.ruoyi.observability;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Supervisor 流式监听器
 *
 * 捕获 Agent 生命周期事件、工具执行前后事件，推送到 OutputChannel
 * inheritedBySubagents() = true -> 注册在 Supervisor 上，自动继承到所有子 Agent
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
@Slf4j
public class SupervisorStreamListener implements dev.langchain4j.agentic.observability.AgentListener {

    private final OutputChannel channel;

    /**
     * 用于在 AgenticScope 中存储 userId 的 key
     */
    public static final String USER_ID_KEY = "userId";

    public SupervisorStreamListener(OutputChannel channel) {
        this.channel = channel;
    }

    // ==================== Agent 调用生命周期 ====================

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentInstance agent = agentRequest.agent();
        AgenticScope scope = agentRequest.agenticScope();
        Map<String, Object> inputs = agentRequest.inputs();
        // 只记录日志，不推送输入信息（避免干扰流式输出）
        log.info("[Agent开始] {} 输入: {}", agent.name(), inputs);
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Map<String, Object> inputs = agentResponse.inputs();
        Object output = agentResponse.output();
        String outputStr = output != null ? output.toString() : "";

        // 只记录日志，不推送输出信息
        // 流式输出由 StreamingOutputWrapper 处理
        // 当无子Agent被调用时，由 ChatServiceFacade 用 plannerModel 生成回复
        log.info("[Agent完成] {} 输出长度: {}", agent.name(), outputStr.length());
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Map<String, Object> inputs = error.inputs();
        Throwable throwable = error.error();

        channel.send("\n[Agent错误] " + agent.name()
            + " 异常: " + throwable.getMessage());
        log.error("[Agent错误] {} 异常: {}", agent.name(), throwable.getMessage(), throwable);
    }

    // ==================== AgenticScope 生命周期 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("[AgenticScope创建] memoryId: {}", agenticScope.memoryId());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("[AgenticScope销毁] memoryId: {}", agenticScope.memoryId());
    }

    // ==================== 工具执行生命周期 ====================

//    @Override
//    public void beforeToolExecution(BeforeToolExecution beforeToolExecution) {
//        var toolRequest = beforeToolExecution.request();
////        channel.send("\n[工具即将执行] " + toolRequest.name()
////            + " 参数: " + truncate(toolRequest.arguments(), 150));
//        log.info("[工具即将执行] {} 参数: {}", toolRequest.name(), toolRequest.arguments());
//    }

//    @Override
//    public void afterToolExecution(ToolExecution toolExecution) {
//        var toolRequest = toolExecution.request();
////        channel.send("\n[工具执行完成] " + toolRequest.name()
////            + " 结果: " + truncate(String.valueOf(toolExecution.result()), 300));
//        log.info("[工具执行完成] {} 结果: {}", toolRequest.name(), toolExecution.result());
//    }

    // ==================== 继承机制 ====================

    /**
     * 返回 true，让此监听器自动继承给所有子 Agent
     */
    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    // ==================== 辅助方法 ====================

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "null";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
