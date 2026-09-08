package org.ruoyi.observability;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 自定义的 AgentListener 的监听器。
 * 监听 Agent 相关的所有可观测性事件，包括：
 * <ul>
 *   <li>Agent 调用前/后的生命周期事件</li>
 *   <li>Agent 执行错误事件</li>
 *   <li>AgenticScope 的创建/销毁事件</li>
 *   <li>工具执行前/后的生命周期事件</li>
 * </ul>
 *
 * @author evo
 */
@Slf4j
public class MyAgentListener implements dev.langchain4j.agentic.observability.AgentListener {

    // ==================== Agent 调用生命周期 ====================

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentInstance agent = agentRequest.agent();
        AgenticScope scope = agentRequest.agenticScope();
        Map<String, Object> inputs = agentRequest.inputs();

        log.info("agent_invocation agentType={} status=STARTED inputCount={} argumentCount={} "
                + "subagentCount={} priorInvocationCount={} async={} leaf={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            agent.arguments() == null ? 0 : agent.arguments().size(),
            agent.subagents() == null ? 0 : agent.subagents().size(),
            scope.agentInvocations() == null ? 0 : scope.agentInvocations().size(),
            agent.async(), agent.leaf());
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Map<String, Object> inputs = agentResponse.inputs();
        Object output = agentResponse.output();

        log.info("agent_invocation agentType={} status=COMPLETED inputCount={} outputType={} leaf={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            output == null ? "none" : output.getClass().getName(), agent.leaf());
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Map<String, Object> inputs = error.inputs();
        Throwable throwable = error.error();

        log.error("agent_invocation agentType={} status=FAILED inputCount={} errorType={}",
            agent.type().getName(), inputs == null ? 0 : inputs.size(),
            throwable == null ? "unknown" : throwable.getClass().getName());
    }

    // ==================== AgenticScope 生命周期 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("agentic_scope status=CREATED invocationCount={}",
            agenticScope.agentInvocations() == null ? 0 : agenticScope.agentInvocations().size());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("agentic_scope status=DESTROYED invocationCount={}",
            agenticScope.agentInvocations() == null ? 0 : agenticScope.agentInvocations().size());
    }

}
