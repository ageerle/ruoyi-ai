package org.ruoyi.service.chat.impl;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.McpAgent;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.dto.request.ReSumeRunner;
import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.chat.service.chatMessage.AbstractChatMessageService;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.core.utils.ObjectUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Validated
public abstract class AbstractStreamingChatService extends AbstractChatMessageService implements IChatService {

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
     * 获取工作流启用Bean对象
     */
    private static final IWorkFlowStarterService starterService = SpringUtils.getBean(IWorkFlowStarterService.class);

    /**
     * 定义聊天流程骨架
     */
    @Override
    public SseEmitter chat(ChatContext chatContext) {
        // 获取模型管理视图对象
        ChatModelVo chatModelVo = chatContext.getChatModelVo();
        // 获取对话请求对象
        ChatRequest chatRequest = chatContext.getChatRequest();
        // 获取SSe连接对象
        SseEmitter emitter = chatContext.getEmitter();
        // 获取用户ID
        Long userId = chatContext.getUserId();
        // 获取Token
        String tokenValue = chatContext.getTokenValue();
        // 获取响应处理器
        StreamingChatResponseHandler handler = chatContext.getHandler();
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

            // 判断用户是否重新输入
            boolean isResume = chatRequest.getIsResume() != null && chatRequest.getIsResume();
            if (isResume){
                ReSumeRunner reSumeRunner = chatRequest.getReSumeRunner();
                if (ObjectUtils.isNotEmpty(reSumeRunner)){
                    starterService.resumeFlow(reSumeRunner.getRuntimeUuid(), reSumeRunner.getFeedbackContent(), emitter);
                    return emitter;
                }
            }

            // 判断用户是否开启工作流
            boolean enableWorkFlow = chatRequest.getEnableWorkFlow() != null && chatRequest.getEnableWorkFlow();
            if (enableWorkFlow) {
                WorkFlowRunner runner = chatRequest.getWorkFlowRunner();
                if (ObjectUtils.isNotEmpty(runner)){
                    return starterService.streaming(ThreadContext.getCurrentUser(), runner.getUuid(), runner.getInputs(), chatRequest.getSessionId());
                }
            }

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
                handler = ObjectUtils.isEmpty(handler) ? createResponseHandler(chatRequest, userId, tokenValue, chatModelVo) : handler;
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
        // 工作流对话消息
        List<ChatMessage> chatMessages = chatRequest.getChatMessages();
        if (!CollectionUtils.isEmpty(chatMessages)){
            messages.addAll(chatMessages);
        }
        // 开启长期记忆
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
     * 执行聊天（钩子方法 - 子类必须实现）
     * 注意：messages 已包含完整的历史上下文和当前消息
     *
     * @param chatModelVo 模型配置
     * @param chatRequest 聊天请求
     * @param handler     响应处理器
     */
    protected abstract void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest,
                                   List<ChatMessage> messagesWithMemory, StreamingChatResponseHandler handler);

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
     * 获取提供者名称（子类必须实现）
     */
    public abstract String getProviderName();

    protected String doAgent(String userMessage, ChatModelVo chatModelVo) {
        log.info("执行Agent任务，消息: {}", userMessage);
        // 加载所有可用的 Agent，让 Supervisor 根据任务类型自动选择
        return doAgentWithAllAgents(userMessage, chatModelVo);
    }

    /**
     * 使用单一 Agent 处理所有任务
     * 不使用 Supervisor 模式，而是使用 MCP Agent 来处理所有任务
     *
     * @param userMessage 用户消息
     * @param chatModelVo 聊天模型配置
     * @return Agent 响应结果
     */
    protected String doAgentWithAllAgents(String userMessage, ChatModelVo chatModelVo) {

        try {
            // 1. 加载 LLM 模型
            QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();

            // 2. 获取统一工具提供工厂
            ToolProviderFactory toolProviderFactory = SpringUtils.getBean(ToolProviderFactory.class);

            // 3. 获取所有可用的工具

            // 3.1 添加 BUILTIN 工具对象（包括 SQL 工具）
            List<Object> builtinTools = toolProviderFactory.getAllBuiltinToolObjects();

            List<Object> allTools = new ArrayList<>(builtinTools);

            log.debug("Loaded {} builtin tools (including SQL tools)", builtinTools.size());

            log.debug("Total tools: {}", allTools.size());

            // 4. 获取 MCP 工具提供者
            ToolProvider mcpToolProvider = toolProviderFactory.getAllEnabledMcpToolsProvider();

            // 5. 创建 MCP Agent（包含所有工具）
            var agentBuilder = AgenticServices.agentBuilder(McpAgent.class).chatModel(qwenChatModel);

            // 添加所有工具
            if (!allTools.isEmpty()) {
                agentBuilder.tools(allTools.toArray(new Object[0]));
            }

            // 添加 MCP 工具
            if (mcpToolProvider != null) {
                agentBuilder.toolProvider(mcpToolProvider);
            }

            McpAgent mcpAgent = agentBuilder.build();

            // 6. 调用大模型LLM
            String result = mcpAgent.callMcpTool(userMessage);
            log.info("Agent 执行完成，结果长度: {}", result.length());
            return result;

        } catch (Exception e) {
            log.error("Agent 模式执行失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 创建流式聊天模型
     * 子类必须实现此方法，返回对应厂商的模型实例
     */
    protected abstract StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest);
}
