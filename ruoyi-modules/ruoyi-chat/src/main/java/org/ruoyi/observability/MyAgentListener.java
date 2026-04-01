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
import java.util.concurrent.atomic.AtomicReference;

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

    /** 最终捕获到的思考结果（主 Agent 完成后写入，供外部获取） */
    private final AtomicReference<String> sharedOutputRef = new AtomicReference<>();

    public String getCapturedResult() {
        return sharedOutputRef.get();
    }

    // ==================== Agent 调用生命周期 ====================

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentInstance agent = agentRequest.agent();
        AgenticScope scope = agentRequest.agenticScope();
        Map<String, Object> inputs = agentRequest.inputs();

        log.info("【Agent调用前】Agent名称: {}", agent.name());
        log.info("【Agent调用前】Agent ID: {}", agent.agentId());
        log.info("【Agent调用前】Agent类型: {}", agent.type().getName());
        log.info("【Agent调用前】Agent描述: {}", agent.description());
        log.info("【Agent调用前】Planner类型: {}", agent.plannerType().getName());
        log.info("【Agent调用前】输出类型: {}", agent.outputType());
        log.info("【Agent调用前】输出Key: {}", agent.outputKey());
        log.info("【Agent调用前】是否为异步: {}", agent.async());
        log.info("【Agent调用前】是否为叶子节点: {}", agent.leaf());
        log.info("【Agent调用前】Agent参数列表:");
        for (var arg : agent.arguments()) {
            log.info("  - 参数名: {}, 类型: {}, 默认值: {}",
                    arg.name(), arg.rawType().getName(), arg.defaultValue());
        }
        log.info("【Agent调用前】Agent输入参数: {}", inputs);
        log.info("【Agent调用前】AgenticScope memoryId: {}", scope.memoryId());
        log.info("【Agent调用前】AgenticScope当前状态: {}", scope.state());
        log.info("【Agent调用前】Agent调用历史记录数: {}", scope.agentInvocations().size());

        // 打印嵌套的子Agent信息
        if (!agent.subagents().isEmpty()) {
            log.info("【Agent调用前】子Agent列表:");
            for (AgentInstance sub : agent.subagents()) {
                log.info("  - 子Agent: {} ({})", sub.name(), sub.type().getName());
            }
        }

        // 打印父Agent信息
        if (agent.parent() != null) {
            log.info("【Agent调用前】父Agent: {}", agent.parent().name());
        }
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Map<String, Object> inputs = agentResponse.inputs();
        Object output = agentResponse.output();
        String outputStr = output != null ? output.toString() : "";

        log.info("【Agent调用后】Agent名称: {}", agent.name());
        log.info("【Agent调用后】Agent ID: {}", agent.agentId());
        log.info("【Agent调用后】Agent输入参数: {}", inputs);
        log.info("【Agent调用后】Agent输出结果: {}", output);
        log.info("【Agent调用后】是否为叶子节点: {}", agent.leaf());

        // 捕获主 Agent 的最终输出，供外部获取
        if ("invoke".equals(agent.agentId()) && !outputStr.isEmpty()) {
            sharedOutputRef.set(outputStr);
            log.info("【Agent调用后】已捕获主Agent输出: {}", outputStr);
        }
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Map<String, Object> inputs = error.inputs();
        Throwable throwable = error.error();

        log.error("【Agent执行错误】Agent名称: {}", agent.name());
        log.error("【Agent执行错误】Agent ID: {}", agent.agentId());
        log.error("【Agent执行错误】Agent类型: {}", agent.type().getName());
        log.error("【Agent执行错误】Agent输入参数: {}", inputs);
        log.error("【Agent执行错误】错误类型: {}", throwable.getClass().getName());
        log.error("【Agent执行错误】错误信息: {}", throwable.getMessage(), throwable);
    }

    // ==================== AgenticScope 生命周期 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("【AgenticScope已创建】memoryId: {}", agenticScope.memoryId());
        log.info("【AgenticScope已创建】初始状态: {}", agenticScope.state());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("【AgenticScope即将销毁】memoryId: {}", agenticScope.memoryId());
        log.info("【AgenticScope即将销毁】最终状态: {}", agenticScope.state());
        log.info("【AgenticScope即将销毁】总调用次数: {}", agenticScope.agentInvocations().size());
    }

    // ==================== 工具执行生命周期 ====================

    @Override
    public void beforeToolExecution(BeforeToolExecution beforeToolExecution) {
        var toolRequest = beforeToolExecution.request();
        log.info("【工具执行前】工具请求ID: {}", toolRequest.id());
        log.info("【工具执行前】工具名称: {}", toolRequest.name());
        log.info("【工具执行前】工具参数: {}", toolRequest.arguments());
    }

    @Override
    public void afterToolExecution(ToolExecution toolExecution) {
        var toolRequest = toolExecution.request();
        log.info("【工具执行后】工具请求ID: {}", toolRequest.id());
        log.info("【工具执行后】工具名称: {}", toolRequest.name());
        log.info("【工具执行后】工具执行结果: {}", toolExecution.result());
        log.info("【工具执行后】工具执行是否失败: {}", toolExecution.hasFailed());
    }
}
