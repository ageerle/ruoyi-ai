package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.*;
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
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.dto.request.ReSumeRunner;
import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.core.utils.ObjectUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.knowledge.rerank.ScoringModelFactory;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务门面层
 * <p>
 * 作为统一入口，负责：
 * 1. 构建对话上下文
 * 2. 路由到对应的处理器
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceFacade implements IChatService {

    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    private final IChatModelService chatModelService;

    private final ChatServiceFactory chatServiceFactory;

    private final IKnowledgeInfoService knowledgeInfoService;

    private final VectorStoreService vectorStoreService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    private final IWorkFlowStarterService workFlowStarterService;

    private final ToolProviderFactory toolProviderFactory;

    private final ScoringModelFactory scoringModelFactory;

    /**
     * 内存实例缓存，避免同一会话重复创建
     * Key: sessionId, Value: MessageWindowChatMemory实例
     */
    private static final Map<Object, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();



    /**
     * 统一聊天入口 - SSE流式响应
     *
     * @param chatRequest 聊天请求
     * @return SseEmitter
     */
    public SseEmitter sseChat(ChatRequest chatRequest) {

        // 4. 具体的服务实现
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息列表
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest);

        // 注意：buildContextMessages() 最后返回的列表中，最新的带有增强知识的 UserMessage 在最后。
        // 对于有些模型API（非langchain4j的代理），它们可能不识别增强后的复杂文本（取决于供应商适配度）
        // 但是通过标准流，它被解析为 String。
        SseEmitter specialResult = handleSpecialChatModes(chatRequest, contextMessages, chatModelVo, emitter);
        if (specialResult != null) {
            return specialResult;
        }

        // 4. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);


        StreamingChatResponseHandler handler = createResponseHandler(userId, tokenValue,chatRequest);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 5. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(contextMessages, handler);
        return emitter;
    }

    /**
     * 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
     *
     * @param chatRequest      聊天请求
     * @param contextMessages  上下文消息列表（可能被修改）
     * @param chatModelVo      聊天模型配置
     * @param emitter          SSE发射器
     * @return 如果需要提前返回则返回SseEmitter，否则返回null
     */
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest, List<ChatMessage> contextMessages,
                                              ChatModelVo chatModelVo, SseEmitter emitter) {
        // 处理工作流对话
        if (chatRequest.getEnableWorkFlow()) {
            log.info("处理工作流对话,会话: {}", chatRequest.getSessionId());

            WorkFlowRunner runner = chatRequest.getWorkFlowRunner();
            if (ObjectUtils.isEmpty(runner)) {
                log.warn("工作流参数为空");
            }

            return workFlowStarterService.streaming(
                ThreadContext.getCurrentUser(),
                runner.getUuid(),
                runner.getInputs(),
                chatRequest.getSessionId()
            );
        }

        // 处理人机交互恢复
        if (chatRequest.getIsResume()) {
            log.info("处理人机交互恢复");

            ReSumeRunner reSumeRunner = chatRequest.getReSumeRunner();
            if (ObjectUtils.isEmpty(reSumeRunner)) {
                log.warn("人机交互恢复参数为空");
                return emitter;
            }

            workFlowStarterService.resumeFlow(
                reSumeRunner.getRuntimeUuid(),
                reSumeRunner.getFeedbackContent(),
                emitter
            );

            return emitter;
        }

        // 处理思考模式
        if (chatRequest.getEnableThinking()) {
            handleThinkingMode(chatRequest, contextMessages, chatModelVo);
        }

        return null;
    }

    /**
     * 处理思考模式
     *
     * @param chatRequest      聊天请求
     * @param contextMessages  上下文消息列表
     * @param chatModelVo      聊天模型配置
     */
    private void handleThinkingMode(ChatRequest chatRequest, List<ChatMessage> contextMessages, ChatModelVo chatModelVo) {
        // 步骤1: 配置MCP传输层 - 连接到bing-cn-mcp服务器
        McpTransport transport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "bing-cn-mcp"))
            .logEvents(true)
            .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();

        ToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient))
            .build();

        // 配置echarts MCP
        McpTransport transport1 = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "mcp-echarts"))
            .logEvents(true)
            .build();

        McpClient mcpClient1 = new DefaultMcpClient.Builder()
            .transport(transport1)
            .build();

        ToolProvider toolProvider1 = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient1))
            .build();

        // 配置模型
        OpenAiChatModel plannerModel = OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .build();

        // 构建各Agent
        SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .build();

        WebSearchAgent searchAgent = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider)
            .build();

        ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider1)
            .build();

        // 构建监督者Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            .subAgents(sqlAgent, chartGenerationAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String invoke = supervisor.invoke(chatRequest.getContent());
        contextMessages.add(AiMessage.from(invoke));
    }

    /**
     * 支持外部 handler 的对话接口（跨模块调用）
     * 同时发送到 SSE 和外部 handler
     *
     * @param chatRequest     聊天请求
     * @param externalHandler 外部响应处理器（可为 null）
     */
    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler externalHandler) {
        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 3. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("跨模块调用 - 路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        // 4. 获取用户信息
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();

        // 5. 建立 SSE 连接（用于前端监听）
        sseEmitterManager.connect(userId, tokenValue);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 6. 创建组合 handler：同时发送到 SSE 和外部 handler
        StreamingChatResponseHandler combinedHandler = createCombinedHandler(userId, tokenValue, externalHandler);

        // 7. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(chatRequest.getContent(), combinedHandler);
    }

    /**
     * 实现接口默认方法 - 不带 handler 的调用
     */
    @Override
    public SseEmitter chat(ChatRequest chatRequest) {
        return sseChat(chatRequest);
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
                PersistentChatMemoryStore store = new PersistentChatMemoryStore(chatMessageService);
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
     * 构建上下文消息列表
     *
     * @param chatRequest 聊天请求
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();

        // 初始化用户消息
        UserMessage userMessage = UserMessage.userMessage(chatRequest.getContent());

        // 使用 LangChain4j 的 RetrievalAugmentor 进行检索增强
        if (chatRequest.getKnowledgeId() != null) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo != null) {
                ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
                if (chatModel != null) {

                    // 1. 构建适配器（Retriever）
                    CustomVectorRetriever retriever = new CustomVectorRetriever(
                            vectorStoreService, knowledgeInfoVo, chatModel);

                    // 2. 构建重排聚合器 (Aggregator)
                    ContentAggregator contentAggregator;
                    if (knowledgeInfoVo.getEnableRerank() != null && knowledgeInfoVo.getEnableRerank() == 1
                            && knowledgeInfoVo.getRerankModel() != null) {

                        ChatModelVo scoringModelConfig = chatModelService.selectModelByName(knowledgeInfoVo.getRerankModel());
                        ScoringModel scoringModel = scoringModelFactory.createScoringModel(scoringModelConfig);

                        if (scoringModel != null) {
                            contentAggregator = ReRankingContentAggregator.builder()
                                    .scoringModel(scoringModel)
                                    // 默认重排后只留前 5 条，避免上下文过长
                                    .maxResults(5)
                                    .build();
                            log.info("启用重排模型: {}", knowledgeInfoVo.getRerankModel());
                        } else {
                            contentAggregator = new DefaultContentAggregator();
                        }
                    } else {
                        contentAggregator = new DefaultContentAggregator();
                    }

                    // 3. 构造流水线
                    RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                            .contentRetriever(retriever)
                            .contentAggregator(contentAggregator)
                            .build();

                    // 4. 执行 Augmentor 增强：将检索到的知识内容编织进 UserMessage 中
                    Metadata ragMetadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
                    AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, ragMetadata);

                    AugmentationResult augmentedResult = augmentor.augment(augmentationRequest);

                    ChatMessage augmentedMessage = augmentedResult.chatMessage();
                    if (augmentedMessage instanceof UserMessage) {
                         userMessage = (UserMessage) augmentedMessage;
                    }
                    log.info("RAG 增强完成: UserMessage 已重构并附加上下文背景。");

                }
            }
        }

        // 从数据库查询历史对话消息（历史消息应放在当前提问前）
        if (chatRequest.getSessionId() != null) {
            MessageWindowChatMemory memory = createChatMemory(chatRequest.getSessionId());
            if (memory != null) {
                List<ChatMessage> historicalMessages = memory.messages();
                if (historicalMessages != null && !historicalMessages.isEmpty()) {
                    messages.addAll(historicalMessages);
                    log.debug("已加载 {} 条历史消息用于会话 {}", historicalMessages.size(), chatRequest.getSessionId());
                }
            }
        }

        // 注入本次用户提问（经过 RAG 增强后的 UserMessage）
        messages.add(userMessage);

        return messages;
    }

    /**
     * 构建向量查询参数
     */
    private QueryVectorBo buildQueryVectorBo(ChatRequest chatRequest, KnowledgeInfoVo knowledgeInfoVo,
                                             ChatModelVo chatModel) {
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(chatRequest.getContent());
        queryVectorBo.setKid(chatRequest.getKnowledgeId());
        queryVectorBo.setApiKey(chatModel.getApiKey());
        queryVectorBo.setBaseUrl(chatModel.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());
        return queryVectorBo;
    }

    /**
     * 创建标准的响应处理器
     *
     * @param userId      用户ID
     * @param tokenValue  会话令牌
     * @return 标准的流式响应处理器
     */
    protected StreamingChatResponseHandler createResponseHandler(Long userId, String tokenValue,ChatRequest chatRequest) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 将消息片段追加到缓冲区
                messageBuffer.append(partialResponse);

                // 实时发送内容事件到客户端
                SseMessageUtils.sendContent(userId, partialResponse);
                log.debug("收到消息片段: {}",  partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 消息流完成，保存消息到数据库和内存
                    String fullMessage = messageBuffer.toString();

                    if (fullMessage.isEmpty()) {
                          log.warn("接收到空消息");
                    } else {
                        // 保存助手回复消息
                        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), fullMessage, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                    }

                    // 关闭SSE连接
                    SseMessageUtils.completeConnection(userId, tokenValue);
                     log.info("消息结束，已保存到数据库");
                } catch (Exception e) {
                      log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage());
            }
        };
    }

    /**
     * 创建组合响应处理器 - 同时发送到 SSE 和外部 handler
     *
     * @param userId          用户ID
     * @param tokenValue      会话令牌
     * @param externalHandler 外部响应处理器（可为 null）
     * @return 组合的流式响应处理器
     */
    protected StreamingChatResponseHandler createCombinedHandler(Long userId, String tokenValue,
                                                                  StreamingChatResponseHandler externalHandler) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 1. 追加到缓冲区
                messageBuffer.append(partialResponse);

                // 2. 发送内容事件到 SSE（前端可通过 SSE 监听）
                SseMessageUtils.sendContent(userId, partialResponse);

                // 3. 转发给外部 handler（Workflow 等模块可处理）
                if (externalHandler != null) {
                    externalHandler.onPartialResponse(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 1. 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 2. 关闭 SSE 连接
                    SseMessageUtils.completeConnection(userId, tokenValue);

                    // 3. 转发给外部 handler
                    if (externalHandler != null) {
                        externalHandler.onCompleteResponse(completeResponse);
                    }
                } catch (Exception e) {
                    log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage(), error);

                // 转发给外部 handler
                if (externalHandler != null) {
                    externalHandler.onError(error);
                }
            }
        };
    }


}

