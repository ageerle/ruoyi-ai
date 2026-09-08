package org.ruoyi.observability;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
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
        log.info("supervisor_agent agentType={} status=STARTED inputCount={} priorInvocationCount={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            scope.agentInvocations() == null ? 0 : scope.agentInvocations().size());
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Map<String, Object> inputs = agentResponse.inputs();
        Object output = agentResponse.output();
        int outputChars = output instanceof CharSequence text ? text.length() : -1;
        log.info("supervisor_agent agentType={} status=COMPLETED inputCount={} outputType={} outputChars={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            output == null ? "none" : output.getClass().getName(), outputChars);
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Map<String, Object> inputs = error.inputs();
        Throwable throwable = error.error();

        channel.send("\n[Agent错误] 执行失败");
        log.error("supervisor_agent agentType={} status=FAILED inputCount={} errorType={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            throwable == null ? "unknown" : throwable.getClass().getName());
    }

    // ==================== AgenticScope 生命周期 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("supervisor_scope status=CREATED invocationCount={}",
            agenticScope.agentInvocations() == null ? 0 : agenticScope.agentInvocations().size());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("supervisor_scope status=DESTROYED invocationCount={}",
            agenticScope.agentInvocations() == null ? 0 : agenticScope.agentInvocations().size());
    }

    // ==================== 继承机制 ====================

    /**
     * 返回 true，让此监听器自动继承给所有子 Agent
     */
    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

}
