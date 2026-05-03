package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.CodingAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.SkillsAgent;
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
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.mcp.tools.CreateFileTool;
import org.ruoyi.mcp.tools.EditFileTool;
import org.ruoyi.mcp.tools.ListDirectoryTool;
import org.ruoyi.mcp.tools.ReadFileTool;
import org.ruoyi.mcp.tools.RunCommandTool;
import org.ruoyi.mcp.tools.TaskPlannerTool;
import org.ruoyi.observability.*;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    private final IWorkFlowStarterService workFlowStarterService;

    private final ToolProviderFactory toolProviderFactory;

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

        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 3. 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
        SseEmitter sseEmitter = handleSpecialChatModes(chatRequest);
        if (sseEmitter != null) {
            return sseEmitter;
        }

        // 4. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        StreamingChatResponseHandler handler = createResponseHandler(userId, tokenValue,chatRequest);


        // 5. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(contextMessages, handler);
        return emitter;
    }

    /**
     * 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
     *
     * @param chatRequest      聊天请求
     * @return 如果需要提前返回则返回SseEmitter，否则返回null
     */
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest) {
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
            }
            workFlowStarterService.resumeFlow(
                reSumeRunner.getRuntimeUuid(),
                reSumeRunner.getFeedbackContent(),
                chatRequest.getEmitter()
            );

            return chatRequest.getEmitter();

        }
        // 处理思考模式
        if (chatRequest.getEnableThinking()) {
           return handleThinkingMode(chatRequest);
        }

        return null;
    }

    /**
     * 处理思考模式
     *
     * @param chatRequest     聊天请求

     */
    private SseEmitter handleThinkingMode(ChatRequest chatRequest) {
        // 配置监督者模型
        OpenAiChatModel plannerModel = OpenAiChatModel.builder()
            .baseUrl(chatRequest.getChatModelVo().getApiHost())
            .apiKey(chatRequest.getChatModelVo().getApiKey())
            .modelName(chatRequest.getChatModelVo().getModelName())
            .build();

        // Bing 搜索 MCP 客户端
        McpTransport bingTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "bing-cn-mcp"))
            .logEvents(true)
            .build();

        Long userId = chatRequest.getUserId();
        McpClient bingMcpClient = new DefaultMcpClient.Builder()
            .transport(bingTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // Playwright MCP 客户端 - 浏览器自动化工具
        McpTransport playwrightTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "@playwright/mcp@latest"))
            .logEvents(true)
            .build();

        McpClient playwrightMcpClient = new DefaultMcpClient.Builder()
            .transport(playwrightTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // Filesystem MCP 客户端 - 文件管理工具
        // 允许 AI 读取、写入、搜索文件（基于当前项目根目录）
        String userDir = System.getProperty("user.dir");
        McpTransport filesystemTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y",
                "@modelcontextprotocol/server-filesystem", userDir))
            .logEvents(true)

            .build();

        McpClient filesystemMcpClient = new DefaultMcpClient.Builder()
            .transport(filesystemTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // 合并三个 MCP 客户端的工具
        ToolProvider toolProvider = McpToolProvider.builder()
            // bingMcpClient,
            .mcpClients(List.of(playwrightMcpClient, filesystemMcpClient))
            .build();

        // ========== LangChain4j Skills 基本用法 ==========
        // 通过 SKILL.md 文件定义，LLM 按需通过 activate_skill 工具加载
        // 加载 Skills - 使用相对路径，基于项目根目录
        java.nio.file.Path skillsPath = java.nio.file.Path.of(userDir, "ruoyi-admin/src/main/resources/skills");
        List<dev.langchain4j.skills.FileSystemSkill> skillsList = dev.langchain4j.skills.FileSystemSkillLoader
            .loadSkills(skillsPath)
            ;

        ShellSkills skills = ShellSkills.from(skillsList);

        // 构建子 Agent
        WebSearchAgent searchAgent  = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 2: SkillsAgent - 负责文档处理技能（docx、pdf、xlsx）
        // 独立管理 Skills 工具
        SkillsAgent skillsAgent = AgenticServices.agentBuilder(SkillsAgent.class)
            .chatModel(plannerModel)
            .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
            .toolProvider(skills.toolProvider())
            .build();

        // 构建子 Agent 3: SqlAgent - 负责数据库查询
        SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 4: ChartGenerationAgent - 负责图表生成
        ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
            .chatModel(plannerModel)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 5: EchartsAgent - 负责数据可视化（结合 SQL 查询生成 Echarts 图表）
        EchartsAgent echartsAgent = AgenticServices.agentBuilder(EchartsAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 6: CodingAgent - 负责通用开发任务落地
        CodingAgent codingAgent = AgenticServices.agentBuilder(CodingAgent.class)
            .chatModel(plannerModel)
            .tools(
                new TaskPlannerTool(),
                new ListDirectoryTool(),
                new ReadFileTool(),
                new CreateFileTool(),
                new EditFileTool(),
                new RunCommandTool()
            )
            .listener(new MyAgentListener())
            .build();

        // 构建监督者 Agent - 管理多个子 Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            //.listener(new SupervisorStreamListener(null))
            .subAgents(skillsAgent,searchAgent, sqlAgent, chartGenerationAgent, echartsAgent, codingAgent)
            // 加入历史上下文 - 使用 ChatMemoryProvider 提供持久化的聊天内存
            //.chatMemoryProvider(memoryId -> createChatMemory(chatRequest.getSessionId()))
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(chatRequest.getContent());
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
            } catch (Exception e) {
                log.error("Supervisor 执行失败", e);
                SseMessageUtils.sendError(userId, e.getMessage());
            } finally {
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });
        return chatRequest.getEmitter();
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
     * 消息顺序：历史消息 → 当前用户消息（确保 AI 正确理解对话上下文）
     *
     * @param chatRequest 聊天请求
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 初始化当前用户消息
        UserMessage userMessage = UserMessage.userMessage(chatRequest.getContent());

        // 2. 知识库检索增强 (RAG)
        if (chatRequest.getKnowledgeId() != null) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo != null) {
                ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
                if (chatModel != null) {
                    log.info("执行高级 RAG 流程: kid={}", chatRequest.getKnowledgeId());

                    // 构建自定义检索器
                    CustomVectorRetriever retriever = new CustomVectorRetriever(
                            knowledgeRetrievalService, knowledgeInfoVo, chatModel);

                    // 构建增强流水线
                    RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                            .contentRetriever(retriever)
                            .build();

                    // 执行增强：编织上下文到 UserMessage
                    Metadata metadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
                    AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                    AugmentationResult result = augmentor.augment(augmentationRequest);

                    ChatMessage augmented = result.chatMessage();
                    if (augmented instanceof UserMessage) {
                        userMessage = (UserMessage) augmented;
                        log.debug("RAG 增强完成，UserMessage 已注入背景知识");
                    }
                }
            }
        }

        // 3. 从数据库查询历史对话消息（放在前面）
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

        // 4. 添加经过增强的用户消息（放在最后）
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

        // 设置重排序参数
        queryVectorBo.setEnableRerank(knowledgeInfoVo.getEnableRerank() != null && knowledgeInfoVo.getEnableRerank() == 1);
        queryVectorBo.setRerankModelName(knowledgeInfoVo.getRerankModel());
        queryVectorBo.setRerankTopN(knowledgeInfoVo.getRerankTopN());
        queryVectorBo.setRerankScoreThreshold(knowledgeInfoVo.getRerankScoreThreshold());

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
