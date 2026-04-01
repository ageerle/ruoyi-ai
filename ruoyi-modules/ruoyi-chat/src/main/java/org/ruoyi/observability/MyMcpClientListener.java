package org.ruoyi.observability;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 自定义的 McpClientListener 的监听器。
 * 监听 MCP 客户端相关的所有可观测性事件，包括：
 * <ul>
 *   <li>MCP 工具执行的开始/成功/错误事件</li>
 *   <li>MCP 资源读取的开始/成功/错误事件</li>
 *   <li>MCP 提示词获取的开始/成功/错误事件</li>
 * </ul>
 *
 * @author evo
 */
@Slf4j
public class MyMcpClientListener implements McpClientListener {

    // ==================== 工具执行 ====================

    @Override
    public void beforeExecuteTool(McpCallContext context) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP工具执行前】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP工具执行前】MCP消息ID: {}", message.getId());
        log.info("【MCP工具执行前】MCP方法: {}", message.method);
    }

    @Override
    public void afterExecuteTool(McpCallContext context, ToolExecutionResult result, Map<String, Object> rawResult) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP工具执行后】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP工具执行后】MCP消息ID: {}", message.getId());
        log.info("【MCP工具执行后】MCP方法: {}", message.method);
        log.info("【MCP工具执行后】工具执行结果: {}", result);
        log.info("【MCP工具执行后】原始结果: {}", rawResult);
    }

    @Override
    public void onExecuteToolError(McpCallContext context, Throwable error) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.error("【MCP工具执行错误】调用唯一标识符: {}", invocationContext.invocationId());
        log.error("【MCP工具执行错误】MCP消息ID: {}", message.getId());
        log.error("【MCP工具执行错误】MCP方法: {}", message.method);
        log.error("【MCP工具执行错误】错误类型: {}", error.getClass().getName());
        log.error("【MCP工具执行错误】错误信息: {}", error.getMessage(), error);
    }

    // ==================== 资源读取 ====================

    @Override
    public void beforeResourceGet(McpCallContext context) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP资源读取前】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP资源读取前】MCP消息ID: {}", message.getId());
        log.info("【MCP资源读取前】MCP方法: {}", message.method);
    }

    @Override
    public void afterResourceGet(McpCallContext context, McpReadResourceResult result, Map<String, Object> rawResult) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP资源读取后】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP资源读取后】MCP消息ID: {}", message.getId());
        log.info("【MCP资源读取后】MCP方法: {}", message.method);
        log.info("【MCP资源读取后】资源内容数量: {}", result.contents() != null ? result.contents().size() : 0);
        log.info("【MCP资源读取后】原始结果: {}", rawResult);
    }

    @Override
    public void onResourceGetError(McpCallContext context, Throwable error) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.error("【MCP资源读取错误】调用唯一标识符: {}", invocationContext.invocationId());
        log.error("【MCP资源读取错误】MCP消息ID: {}", message.getId());
        log.error("【MCP资源读取错误】MCP方法: {}", message.method);
        log.error("【MCP资源读取错误】错误类型: {}", error.getClass().getName());
        log.error("【MCP资源读取错误】错误信息: {}", error.getMessage(), error);
    }

    // ==================== 提示词获取 ====================

    @Override
    public void beforePromptGet(McpCallContext context) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP提示词获取前】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP提示词获取前】MCP消息ID: {}", message.getId());
        log.info("【MCP提示词获取前】MCP方法: {}", message.method);
    }

    @Override
    public void afterPromptGet(McpCallContext context, McpGetPromptResult result, Map<String, Object> rawResult) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.info("【MCP提示词获取后】调用唯一标识符: {}", invocationContext.invocationId());
        log.info("【MCP提示词获取后】MCP消息ID: {}", message.getId());
        log.info("【MCP提示词获取后】MCP方法: {}", message.method);
        log.info("【MCP提示词获取后】提示词描述: {}", result.description());
        log.info("【MCP提示词获取后】提示词消息数量: {}", result.messages() != null ? result.messages().size() : 0);
        log.info("【MCP提示词获取后】原始结果: {}", rawResult);
    }

    @Override
    public void onPromptGetError(McpCallContext context, Throwable error) {
        InvocationContext invocationContext = context.invocationContext();
        McpClientMessage message = context.message();

        log.error("【MCP提示词获取错误】调用唯一标识符: {}", invocationContext.invocationId());
        log.error("【MCP提示词获取错误】MCP消息ID: {}", message.getId());
        log.error("【MCP提示词获取错误】MCP方法: {}", message.method);
        log.error("【MCP提示词获取错误】错误类型: {}", error.getClass().getName());
        log.error("【MCP提示词获取错误】错误信息: {}", error.getMessage(), error);
    }
}
