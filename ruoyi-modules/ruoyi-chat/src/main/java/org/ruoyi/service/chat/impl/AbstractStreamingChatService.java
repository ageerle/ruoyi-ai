package org.ruoyi.service.chat.impl;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.common.chat.Service.IChatService;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.core.utils.ObjectUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.domain.bo.chat.ChatMessageBo;
import org.ruoyi.enums.RoleType;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式聊天服务抽象基类 - 支持上下文和长期记忆
 * 使用模板方法模式，抽取公共逻辑
 * <p>
 * 设计原则：
 * 1. 抽象层只依赖业务模型，不依赖具体SDK
 * 2. 子类负责将业务模型转换为厂商SDK格式
 * 3. 提供钩子方法，子类可灵活覆盖
 * 4. 支持长期记忆 - 自动维护会话的消息历史
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Slf4j
public abstract class AbstractStreamingChatService implements IChatService {

    /**
     * 默认保留的消息窗口大小（用于长期记忆）
     */
    private static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * 是否启用长期记忆功能
     */
    private static final boolean enablePersistentMemory = true;

    /**
     * 内存实例缓存，避免同一会话重复创建
     * Key: sessionId, Value: MessageWindowChatMemory实例
     */
    private static final Map<Object, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();



    
    /**
     * 定义聊天流程骨架
     */
    @Override
    public SseEmitter chat(ChatModelVo chatModelVo, ChatRequest chatRequest, SseEmitter emitter, Long userId, String tokenValue) {
        return executeChat(chatModelVo, chatRequest, emitter, userId, tokenValue, null);
    }

    /**
     * 定义聊天流程骨架（包含流式回调结构）
     */
    @Override
    public SseEmitter chat(ChatModelVo chatModelVo, ChatRequest chatRequest, SseEmitter emitter, Long userId, String tokenValue, StreamingChatResponseHandler handler) {
        return executeChat(chatModelVo, chatRequest, emitter, userId, tokenValue, handler);
    }

    /**
     * 定义聊天流程骨架
     */
    public SseEmitter executeChat(ChatModelVo chatModelVo, ChatRequest chatRequest, SseEmitter emitter, Long userId, String tokenValue, StreamingChatResponseHandler handler) {
        try {
            String content = Optional.ofNullable(chatRequest.getMessages()).filter(messages -> !messages.isEmpty())
                // 对话逻辑：从 messages 筛选第一个元素
                .map(messages -> messages.get(0).getContent())
                .filter(StringUtils::isNotBlank)
                // 工作流逻辑：从 chatMessages 筛选 UserMessage 的文本
                .orElseGet(() -> Optional.ofNullable(chatRequest.getChatMessages()).orElse(List.of()).stream()
                        .filter(message -> message instanceof UserMessage um)
                        .map(message -> ((UserMessage) message).singleText())
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse(""));

            // 保存用户消息
            saveChatMessage(chatRequest, userId, content, RoleType.USER.getName(), chatModelVo);
            // 使用长期记忆增强的消息列表
            List<ChatMessage> messagesWithMemory = buildMessagesWithMemory(chatRequest);

            if (chatRequest.getEnableThinking()) {
                String msg = doAgent(content, chatModelVo);
                SseMessageUtils.sendMessage(userId, msg);
                SseMessageUtils.completeConnection(userId, tokenValue);
                // 保存助手回复消息
                saveChatMessage(chatRequest, userId, msg, RoleType.ASSISTANT.getName(), chatModelVo);
            } else {
                // 创建包含内存管理的响应处理器
                if (ObjectUtils.isEmpty(handler)) {
                    handler = createResponseHandler(chatRequest, userId, tokenValue, chatModelVo);
                }
                // 调用具体实现的聊天方法
                doChat(chatModelVo, chatRequest, messagesWithMemory, handler);
            }

        } catch (Exception e) {
            SseMessageUtils.sendMessage(userId, "对话出错：" + e.getMessage());
            SseMessageUtils.completeConnection(userId, tokenValue);
            log.error("{}请求失败：{}", getProviderName(), e.getMessage(), e);
        }
        return emitter;
    }

    /**
     * 构建包含历史消息和当前请求的完整消息列表（长期记忆）
     * 返回: 历史消息 + 当前请求消息
     * 确保即使第一次对话也有消息上下文
     *
     * @param chatRequest 聊天请求
     * @return 包含历史消息和当前请求消息的完整消息列表
     */
    protected List<ChatMessage> buildMessagesWithMemory(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();
        if (enablePersistentMemory && chatRequest.getSessionId() != null) {
            MessageWindowChatMemory memory = createChatMemory(chatRequest.getSessionId());
            if (memory != null) {
                List<ChatMessage> historicalMessages = memory.messages();
                if (historicalMessages != null && !historicalMessages.isEmpty()) {
                    messages.addAll(historicalMessages);
                    log.debug("已加载 {} 条历史消息用于会话 {}", historicalMessages.size(), chatRequest.getSessionId());
                }
            }
            return messages;
        }
        // 工作流方式
        List<ChatMessage> chatMessages = chatRequest.getChatMessages();
        if (!CollectionUtils.isEmpty(chatMessages)){
            messages.addAll(chatMessages);
        }
        return messages;
    }

    /**
     * 创建或获取聊天内存实例（缓存机制）
     * 同一个会话ID会返回同一个内存实例，避免重复创建和消息丢失
     *
     * @param memoryId 内存ID（会话ID）
     * @return MessageWindowChatMemory实例
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        // 先从缓存中获取
        return memoryCache.computeIfAbsent(memoryId, key -> {
            try {
                PersistentChatMemoryStore store = new PersistentChatMemoryStore();
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(DEFAULT_MAX_MESSAGES)
                        .chatMemoryStore(store)
                        .build();
            } catch (Exception e) {
                log.warn("创建聊天内存失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 清理指定会话的内存缓存（可选）
     * 在会话结束时调用，释放内存资源
     *
     * @param sessionId 会话ID
     */
    public static void clearChatMemory(Object sessionId) {
        memoryCache.remove(sessionId);
        log.debug("已清理会话 {} 的内存缓存", sessionId);
    }

    /**
     * 执行聊天（钩子方法 - 子类必须实现）
     * 注意：messages 已包含完整的历史上下文和当前消息
     *
     * @param chatModelVo 模型配置
     * @param chatRequest 聊天请求
     * @param handler     响应处理器
     */
    protected abstract void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest, List<ChatMessage> messagesWithMemory, StreamingChatResponseHandler handler);

    /**
     * 创建标准的响应处理器
     *
     * @param chatRequest 聊天请求，包含sessionId等上下文信息
     * @param userId      用户ID
     * @param tokenValue  会话令牌
     * @param chatModelVo 模型配置
     * @return 标准的流式响应处理器
     */
    protected StreamingChatResponseHandler createResponseHandler(ChatRequest chatRequest, Long userId,
                                                                 String tokenValue, ChatModelVo chatModelVo) {
        return new StreamingChatResponseHandler() {
            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 将消息片段追加到缓冲区
                messageBuffer.append(partialResponse);

                // 实时发送消息片段到客户端
                SseMessageUtils.sendMessage(userId, partialResponse);
                log.debug("收到{}消息片段: {}", getProviderName(), partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 消息流完成，保存消息到数据库和内存
                    String fullMessage = messageBuffer.toString();

                    if (fullMessage.isEmpty()) {
                        log.warn("{}接收到空消息", getProviderName());
                    } else {
                        // 保存助手回复消息
                        saveChatMessage(chatRequest, userId, fullMessage, RoleType.ASSISTANT.getName(), chatModelVo);
                    }

                    // 关闭SSE连接
                    SseMessageUtils.completeConnection(userId, tokenValue);
                    log.info("{}消息结束，已保存到数据库", getProviderName());
                } catch (Exception e) {
                    log.error("{}完成响应时出错: {}", getProviderName(), e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("{}流式响应错误: {}", getProviderName(), error.getMessage(), error);

                // 发送错误消息到前端
                try {
                    String errorMessage = String.format("模型调用失败: %s", error.getMessage());
                    SseMessageUtils.sendMessage(userId, errorMessage);
                } catch (Exception e) {
                    log.error("发送错误消息失败: {}", e.getMessage(), e);
                }

                // 关闭SSE连接，避免前端一直等待
                try {
                    SseMessageUtils.completeConnection(userId, tokenValue);
                } catch (Exception e) {
                    log.error("关闭SSE连接失败: {}", e.getMessage(), e);
                }
            }
        };
    }

    /**
     * 保存聊天消息到数据库
     *
     * @param chatRequest 聊天请求
     * @param userId      用户ID
     * @param content     消息内容
     * @param role        消息角色
     * @param chatModelVo 模型配置
     */
    private void saveChatMessage(ChatRequest chatRequest, Long userId, String content, String role, ChatModelVo chatModelVo) {
        try {
            // 验证必要的上下文信息
            if (chatRequest == null || userId == null) {
                log.warn("缺少必要的聊天上下文信息，无法保存消息");
                return;
            }

            // 创建ChatMessageBo对象
            ChatMessageBo messageBO = new ChatMessageBo();
            messageBO.setUserId(userId);
            messageBO.setSessionId(chatRequest.getSessionId());
            messageBO.setContent(content);
            messageBO.setRole(role);
            messageBO.setModelName(chatRequest.getModel());
            messageBO.setBillingType(chatModelVo.getModelType());
            messageBO.setRemark(null);

            IChatMessageService chatMessageService = SpringUtils.getBean(IChatMessageService.class);
            chatMessageService.insertByBo(messageBO);
        } catch (Exception e) {
            log.error("保存{}聊天消息时出错: {}", getProviderName(), e.getMessage(), e);
        }
    }

    /**
     * 构建具体厂商的 StreamingChatModel
     * 子类必须实现此方法，返回对应厂商的模型实例
     */
    protected abstract StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest);


    /**
     * 获取提供者名称（子类必须实现）
     */
    public abstract String getProviderName();

    protected String doAgent(String userMessage, ChatModelVo chatModelVo) {
        // 步骤1: 配置MCP传输层 - 连接到bing-cn-mcp服务器
        // 该服务提供两个工具: bing_search (必应搜索) 和 crawl_webpage (网页抓取)
        McpTransport transport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y",
                "bing-cn-mcp"
            ))
            .logEvents(true)
            .build();

        // 步骤2: 创建MCP客户端
        McpClient mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();

        // 步骤3: 配置工具提供者
        ToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient))
            .build();


        McpTransport transport1 = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y",
                "mcp-echarts"
            ))
            .logEvents(true)
            .build();

        // 步骤2: 创建MCP客户端
        McpClient mcpClient1 = new DefaultMcpClient.Builder()
            .transport(transport1)
            .build();

        // 步骤3: 配置工具提供者
        ToolProvider toolProvider1 = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient1))
            .build();

        // 步骤4: 配置OpenAI模型
        OpenAiChatModel PLANNER_MODEL = OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .build();

        SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
            .chatModel(PLANNER_MODEL)
            .tools(
                new QueryAllTablesTool(),
                new QueryTableSchemaTool(),
                new ExecuteSqlQueryTool()
            )
            .build();

        WebSearchAgent searchAgent = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(PLANNER_MODEL)
            .toolProvider(toolProvider)
            .build();

        ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
            .chatModel(PLANNER_MODEL)
            .toolProvider(toolProvider1)
            .build();

        SupervisorAgent supervisor = AgenticServices
            .supervisorBuilder()
            .chatModel(PLANNER_MODEL)
            .subAgents(sqlAgent, chartGenerationAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String invoke = supervisor.invoke(userMessage);
        System.out.println(invoke);
        return invoke;
    }
}
