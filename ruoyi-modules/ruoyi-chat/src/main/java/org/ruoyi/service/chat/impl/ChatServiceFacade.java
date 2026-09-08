package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.ChitChatAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
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
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.core.DefaultTraceStreamSpan;
import org.ruoyi.common.trace.core.TraceContext;
import org.ruoyi.common.trace.core.TraceScope;
import org.ruoyi.common.trace.core.TraceStreamSpan;
import org.ruoyi.common.trace.domain.TraceNode;
import org.ruoyi.common.trace.domain.TraceRun;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService;
import org.ruoyi.observability.*;
import org.ruoyi.service.agent.IAgentService;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.ChatSessionOwnershipGuard;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.argtrace.RagTraceNodeTypes;
import org.ruoyi.argtrace.RagTracePayloadBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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

    static final String SAFE_CHAT_ERROR_MESSAGE = "对话处理失败，请稍后重试";

    private final IChatModelService chatModelService;

    private final ChatServiceFactory chatServiceFactory;

    private final IKnowledgeInfoService knowledgeInfoService;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    private final ChatSessionOwnershipGuard chatSessionOwnershipGuard;

    private final IWorkFlowStarterService workFlowStarterService;


    private final IAgentService agentService;

    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;

    private final TraceRecordService traceRecordService;

    private final TraceProperties traceProperties;

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
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        chatSessionOwnershipGuard.requireOwned(userId, chatRequest.getSessionId());

        boolean workflowMode = Boolean.TRUE.equals(chatRequest.getEnableWorkFlow());
        boolean agentMode = chatRequest.getAgentId() != null;
        if (workflowMode && agentMode) {
            throw new IllegalArgumentException("对话模式参数冲突：工作流和智能体不能同时启用");
        }

        // 工作流模式。工作流引擎负责创建并持有自己的 SSE，必须在普通聊天连接创建前路由。
        if (workflowMode) {
            chatMessageService.saveChatMessage(
                userId,
                chatRequest.getSessionId(),
                chatRequest.getContent(),
                RoleType.USER.getName(),
                chatRequest.getModel()
            );
            return handleWorkflowChat(chatRequest);
        }

        // 智能体解析：传入 agentId 时按智能体绑定的模型覆盖 model 字段
        AgentVo agentVo = null;
        if (agentMode) {
            agentVo = agentService.queryById(chatRequest.getAgentId());
            if (agentVo == null) {
                throw new IllegalArgumentException("智能体不存在");
            }
            if (!"0".equals(agentVo.getStatus())) {
                throw new IllegalArgumentException("智能体已停用");
            }
            if (agentVo != null && agentVo.getModelId() != null) {
                ChatModelVo agentModel = chatModelService.queryById(agentVo.getModelId());
                if (agentModel == null) {
                    throw new IllegalArgumentException("智能体绑定的模型不存在");
                }
                chatRequest.setModel(agentModel.getModelName());
            }
        }

        if (StringUtils.isBlank(chatRequest.getModel())) {
            throw new IllegalArgumentException(
                agentVo == null ? "对话模式必须指定模型" : "智能体未绑定模型，且请求未提供回退模型"
            );
        }

        // 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在");
        }

        // 对话和智能体模式共用按会话隔离的 SSE。
        SseEmitter emitter = sseEmitterManager.connect(String.valueOf(chatRequest.getSessionId()));

        // 构建上下文消息列表（系统提示词 + 历史消息 + 当前用户消息）
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest, agentVo);

        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        TraceRunHandle traceRun = startRagTraceRun(chatRequest, userId);

        // 智能体和普通对话互斥：有 agentId 为智能体，否则为普通模型对话。
        if (agentVo != null) {
            log.info("处理智能体对话,会话:{},agentId:{}", chatRequest.getSessionId(), chatRequest.getAgentId());
            return handleAgentChat(chatRequest, agentVo, traceRun);
        }
        log.info("处理普通对话,会话:{},模型:{}", chatRequest.getSessionId(), chatRequest.getModel());
        return handleModelChat(chatRequest, traceRun);
    }

    /**
     * 工作流模式。工作流运行时负责 SSE、节点执行和结束事件。
     */
    private SseEmitter handleWorkflowChat(ChatRequest chatRequest) {
        WorkFlowRunner runner = chatRequest.getWorkFlowRunner();
        if (ObjectUtils.isEmpty(runner) || StringUtils.isBlank(runner.getUuid())) {
            throw new IllegalArgumentException("工作流模式必须提供 workFlowRunner.uuid");
        }
        log.info("处理工作流对话,会话:{},workflowUuid:{}", chatRequest.getSessionId(), runner.getUuid());
        return workFlowStarterService.streaming(
            ThreadContext.getCurrentUser(),
            runner.getUuid(),
            runner.getInputs() == null ? List.of() : runner.getInputs(),
            chatRequest.getSessionId()
        );
    }

    /**
     * 普通对话模式：直接调用选定模型，不装配 Supervisor、MCP、Skills 或专业子 Agent。
     */
    private SseEmitter handleModelChat(ChatRequest chatRequest, TraceRunHandle traceRun) {
        ChatModelVo chatModelVo = chatRequest.getChatModelVo();
        AbstractChatService chatService = chatServiceFactory.getOriginalService(chatModelVo.getProviderCode());
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        List<ChatMessage> messages = buildModelChatMessages(chatRequest);

        TraceStreamSpan llmSpan = null;
        try (TraceScope ignored = openTraceScope(traceRun, chatRequest.getUserId())) {
            llmSpan = startLlmCallSpan(traceRun, chatRequest, "handleModelChat");
            streamingChatModel.chat(
                messages,
                createModelChatResponseHandler(chatRequest, traceRun, llmSpan)
            );
        } catch (Exception e) {
            if (llmSpan != null) {
                llmSpan.finishError(e);
                llmSpan.detach();
            }
            finishTraceRun(traceRun, TraceConstants.STATUS_ERROR, e);
            SseMessageUtils.sendError(String.valueOf(chatRequest.getSessionId()), SAFE_CHAT_ERROR_MESSAGE);
            SseMessageUtils.completeConnection(String.valueOf(chatRequest.getSessionId()));
            log.error("chat_operation operation=MODEL_CHAT status=FAILED errorType={}", errorType(e));
        }
        return chatRequest.getEmitter();
    }

    /**
     * 智能体对话模式：构建 Supervisor 多 Agent 编排并异步执行，结果通过 SSE 推送。
     *
     * @param chatRequest 聊天请求
     * @param agentVo    智能体配置
     */
    private SseEmitter handleAgentChat(ChatRequest chatRequest, AgentVo agentVo, TraceRunHandle traceRun) {
        ChatModelVo chatModelVo = chatRequest.getChatModelVo();

        // 配置监督者模型：统一按 providerCode 走对应 AbstractChatService.buildChatModel，
        // 兼容 ZhiPu/QianWen/Ollama/Dify/Coze/CustomApi 等非 OpenAI 协议；默认实现为 OpenAI 兼容。
        AbstractChatService chatService = chatServiceFactory.getOriginalService(chatModelVo.getProviderCode());
        ChatModel plannerModel = chatService.buildChatModel(chatModelVo);

        Long userId = chatRequest.getUserId();
        String sessionId = String.valueOf(chatRequest.getSessionId());

        // Only explicitly configured legacy MCP tools are installed. The old implicit
        // Playwright/filesystem fallback bypassed Harness leases and approvals.
        ToolProvider toolProvider = null;
        if (agentVo != null && agentVo.getMcpToolIds() != null && !agentVo.getMcpToolIds().isEmpty()) {
            toolProvider = langChain4jMcpToolProviderService.getToolProvider(agentVo.getMcpToolIds());
        }

        // 构建子 Agent
        var searchAgentBuilder = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .listener(new MyAgentListener());
        if (toolProvider != null) {
            searchAgentBuilder.toolProvider(toolProvider);
        }
        WebSearchAgent searchAgent = searchAgentBuilder.build();

        // Disk skills previously exposed unrestricted run_shell_command. Coding skills now live
        // behind the Harness catalog, policy engine, and per-call approval flow.
        if (agentVo != null && agentVo.getSkillNames() != null
            && !agentVo.getSkillNames().isEmpty()) {
            log.warn("Legacy shell-backed skills are disabled; use the coding Harness skill runtime");
        }

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

        // 构建子 Agent 6: ChitChatAgent - 简单闲聊兜底,避免无子 Agent 可用时 supervisor 空转
        ChitChatAgent chitChatAgent = AgenticServices.agentBuilder(ChitChatAgent.class)
            .chatModel(plannerModel)
            .build();

        // 构建监督者 Agent - 管理多个子 Agent
        var supervisorBuilder = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            .subAgents(searchAgent, sqlAgent, chartGenerationAgent, echartsAgent, chitChatAgent)
            .supervisorContext("仅当请求是问候或简单闲聊、不需要任何数据、搜索、技能或图表时,才使用 chitChatAgent;"
                + "其余情况必须使用对应的专业 Agent。"
                + "数据库问数交给 SqlAgent；用户已提供完整数据时交给 ChartGenerationAgent。"
                + "数据库转图表可交给 EchartsAgent；用户明确要求先查询再绘图时，先调用 SqlAgent，"
                + "将其 SQL、筛选条件、单位、完整结果行和截断状态传给 ChartGenerationAgent。"
                + "数据查询失败、为空或被截断时，返回限制说明，不得编造图表。"
                + "图表生成后直接结束任务，保留最后结果的 echarts 代码块，不再调用闲聊 Agent 改写。")
            .responseStrategy(SupervisorResponseStrategy.LAST);
        SupervisorAgent supervisor = supervisorBuilder.build();

        // 知识库增强：智能体绑定了知识库时，对 supervisor 输入做一次 RAG 增强（全程唯一一次检索）
        String augmentedInput = augmentAgentInput(chatRequest, agentVo);
        // 组装最终 prompt：系统提示词 → 多轮历史 → RAG 增强后的当前提问
        StringBuilder promptBuilder = new StringBuilder();
        if (agentVo != null && StringUtils.isNotBlank(agentVo.getSystemPrompt())) {
            promptBuilder.append(agentVo.getSystemPrompt()).append("\n\n");
        }
        String historyText = formatHistoryMessages(chatRequest.getContextMessages(), chatRequest.getContent());
        if (StringUtils.isNotBlank(historyText)) {
            promptBuilder.append("以下是本次会话的历史对话，请结合上下文理解用户最新提问：\n")
                .append(historyText).append("\n\n");
        }
        promptBuilder.append(augmentedInput);
        String prompt = promptBuilder.toString();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            TraceStreamSpan llmSpan = null;
            try (TraceScope ignored = openTraceScope(traceRun, userId)) {
                llmSpan = startLlmCallSpan(traceRun, chatRequest, "handleAgentChat");
                String result = supervisor.invoke(prompt);
                SseMessageUtils.sendContent(sessionId, result);
                SseMessageUtils.sendDone(sessionId);
                // 保存助手回复到数据库（智能体对话为默认路径后，需在此落库以保留历史）
                if (StringUtils.isNotBlank(result)) {
                    chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
                        result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                }
                if (llmSpan != null) {
                    llmSpan.finishSuccess(RagTracePayloadBuilder.streamOutputSummary(
                        result == null ? 0 : result.length()));
                }
                finishTraceRun(traceRun, TraceConstants.STATUS_SUCCESS, null);
            } catch (Exception e) {
                if (llmSpan != null) {
                    llmSpan.finishError(e);
                }
                finishTraceRun(traceRun, TraceConstants.STATUS_ERROR, e);
                log.error("chat_operation operation=SUPERVISOR status=FAILED errorType={}", errorType(e));
                SseMessageUtils.sendError(sessionId, SAFE_CHAT_ERROR_MESSAGE);
            } finally {
                if (llmSpan != null) {
                    llmSpan.detach();
                }
                SseMessageUtils.completeConnection(sessionId);
            }
        });
        return chatRequest.getEmitter();
    }

    private TraceRunHandle startRagTraceRun(ChatRequest chatRequest, Long userId) {
        if (!traceProperties.isEnabled()) {
            return null;
        }

        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startMillis = System.currentTimeMillis();
        TraceRun run = new TraceRun();
        run.setTraceId(traceId);
        run.setTraceName(RagTraceNodeTypes.TRACE_NAME_RAG_CHAT);
        run.setBusinessType(RagTraceNodeTypes.BUSINESS_TYPE_RAG_CHAT);
        run.setBusinessId(chatRequest.getSessionId() == null ? null : chatRequest.getSessionId().toString());
        run.setUserId(userId);
        run.setTenantId(safeGetTenantId());
        run.setStatus(TraceConstants.STATUS_RUNNING);
        run.setStartTime(new Date(startMillis));
        run.setMetadata(RagTracePayloadBuilder.chatRequestSummary(chatRequest));

        try {
            traceRecordService.startRun(run);
        } catch (Exception e) {
            log.warn("trace_persistence operation=START_RUN status=FAILED traceId={} errorType={}",
                traceId, errorType(e));
        }
        return new TraceRunHandle(traceId, startMillis, run.getBusinessId(), run.getTenantId());
    }

    private TraceScope openTraceScope(TraceRunHandle traceRun, Long userId) {
        if (traceRun == null) {
            return null;
        }
        return TraceContext.begin(traceRun.traceId, RagTraceNodeTypes.BUSINESS_TYPE_RAG_CHAT,
            traceRun.businessId, userId, traceRun.tenantId);
    }

    private TraceStreamSpan startLlmCallSpan(TraceRunHandle traceRun, ChatRequest chatRequest,
                                             String methodName) {
        if (traceRun == null || StringUtils.isBlank(TraceContext.getTraceId())) {
            return null;
        }

        String nodeId = UUID.randomUUID().toString().replace("-", "");
        long startMillis = System.currentTimeMillis();
        TraceNode node = new TraceNode();
        node.setTraceId(traceRun.traceId);
        node.setNodeId(nodeId);
        node.setParentNodeId(TraceContext.currentNodeId());
        node.setDepth(TraceContext.depth());
        node.setNodeName("llm-call");
        node.setNodeType(RagTraceNodeTypes.NODE_LLM_CALL);
        node.setClassName(ChatServiceFacade.class.getName());
        node.setMethodName(methodName);
        node.setStatus(TraceConstants.STATUS_RUNNING);
        node.setStartTime(new Date(startMillis));
        node.setInputPayload(RagTracePayloadBuilder.streamInputSummary(chatRequest));

        try {
            traceRecordService.startNode(node);
            TraceContext.pushNode(nodeId);
            return new DefaultTraceStreamSpan(traceRecordService, traceProperties, traceRun.traceId, nodeId, startMillis);
        } catch (Exception e) {
            log.warn("trace_persistence operation=START_NODE status=FAILED traceId={} nodeId={} errorType={}",
                traceRun.traceId, nodeId, errorType(e));
            return null;
        }
    }

    private void finishTraceRun(TraceRunHandle traceRun, String status, Throwable error) {
        if (traceRun == null || !traceRun.finished.compareAndSet(false, true)) {
            return;
        }
        try {
            traceRecordService.finishRun(traceRun.traceId, status, traceErrorSummary(error),
                new Date(), System.currentTimeMillis() - traceRun.startMillis);
        } catch (Exception e) {
            log.warn("trace_persistence operation=FINISH_RUN status=FAILED traceId={} errorType={}",
                traceRun.traceId, errorType(e));
        }
    }

    private String safeGetTenantId() {
        try {
            return LoginHelper.getTenantId();
        } catch (Exception e) {
            log.warn("trace_context operation=RESOLVE_TENANT status=FAILED errorType={}", errorType(e));
            return null;
        }
    }

    private static final class TraceRunHandle {

        private final String traceId;
        private final long startMillis;
        private final String businessId;
        private final String tenantId;
        private final AtomicBoolean finished = new AtomicBoolean(false);

        private TraceRunHandle(String traceId, long startMillis, String businessId, String tenantId) {
            this.traceId = traceId;
            this.startMillis = startMillis;
            this.businessId = businessId;
            this.tenantId = tenantId;
        }
    }

    /**
     * 智能体对话下的输入增强：智能体绑定知识库时，对原始 content 做多知识库 RAG 增强。
     * 无知识库时原样返回 content。
     */
    private String augmentAgentInput(ChatRequest chatRequest, AgentVo agentVo) {
        String content = chatRequest.getContent();
        List<Long> knowledgeIds = collectKnowledgeIds(chatRequest, agentVo);
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return content;
        }
        try {
            RetrievalAugmentor augmentor = buildMultiKnowledgeAugmentor(knowledgeIds);
            if (augmentor == null) {
                return content;
            }
            UserMessage userMessage = UserMessage.userMessage(content);
            Metadata metadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
            AugmentationResult result = augmentor.augment(new AugmentationRequest(userMessage, metadata));
            ChatMessage augmented = result.chatMessage();
            return augmented instanceof UserMessage ? ((UserMessage) augmented).singleText() : content;
        } catch (Exception e) {
            log.warn("chat_rag operation=AUGMENT status=FALLBACK errorType={}", errorType(e));
            return content;
        }
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
            throw new IllegalArgumentException("模型不存在");
        }

        // 3. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("chat_routing status=SELECTED");
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        // 4. 获取用户信息
        Long userId = LoginHelper.getUserId();

        // 5. 建立 SSE 连接（用于前端监听，按会话隔离）
        // 工作流调用时(externalHandler 非空), SSE 连接由工作流引擎创建并持有(WorkflowStarter#streaming),
        // connect 为替换语义(关闭同键旧连接), 此处重连会掐断工作流连接, 必须跳过
        if (externalHandler == null) {
            sseEmitterManager.connect(String.valueOf(chatRequest.getSessionId()));
        }

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 6. 创建组合 handler：同时发送到 SSE 和外部 handler
        StreamingChatResponseHandler combinedHandler = createCombinedHandler(String.valueOf(chatRequest.getSessionId()), externalHandler);

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
                log.warn("chat_memory operation=CREATE status=FAILED errorType={}", errorType(e));
                return null;
            }
        });
    }


    /**
     * 构建上下文消息列表
     * 消息顺序：系统提示词 → 历史消息 → 当前用户消息（确保 AI 正确理解对话上下文）
     *
     * @param chatRequest 聊天请求
     * @param agentVo     智能体配置（可为 null）
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest, AgentVo agentVo) {
        List<ChatMessage> messages = new ArrayList<>();

        // 0. 智能体自定义系统提示词（普通对话今天无 SystemMessage，这里新增注入点）
        if (agentVo != null && StringUtils.isNotBlank(agentVo.getSystemPrompt())) {
            messages.add(SystemMessage.from(agentVo.getSystemPrompt()));
        }

        // 1. 从数据库查询历史对话消息（放在前面）
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

        // 2. 添加当前用户消息（放在最后；RAG 增强在 handleAgentChat 中统一执行，避免重复检索）
        messages.add(UserMessage.userMessage(chatRequest.getContent()));

        return messages;
    }

    /**
     * 构建普通对话消息。保留历史上下文，并在请求指定知识库时仅增强当前用户消息。
     */
    private List<ChatMessage> buildModelChatMessages(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>(chatRequest.getContextMessages());
        String augmentedInput = augmentAgentInput(chatRequest, null);
        int lastIndex = messages.size() - 1;
        if (lastIndex >= 0 && messages.get(lastIndex) instanceof UserMessage) {
            messages.set(lastIndex, UserMessage.userMessage(augmentedInput));
        }
        return messages;
    }

    /**
     * 将上下文消息格式化为多轮对话文本（供只接受 String 输入的 Supervisor 使用）。
     * 跳过 SystemMessage（系统提示词单独前置）与最后一条当前用户消息（单独做 RAG 增强后拼接）。
     */
    private String formatHistoryMessages(List<ChatMessage> contextMessages, String currentContent) {
        if (contextMessages == null || contextMessages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = contextMessages.size();
        // 最后一条是当前用户消息，不纳入历史（避免与增强后的输入重复）
        if (limit > 0 && contextMessages.get(limit - 1) instanceof UserMessage) {
            limit--;
        }
        for (int i = 0; i < limit; i++) {
            ChatMessage msg = contextMessages.get(i);
            if (msg instanceof UserMessage userMsg) {
                sb.append("用户: ").append(userMsg.singleText()).append("\n");
            } else if (msg instanceof AiMessage aiMsg) {
                sb.append("助手: ").append(aiMsg.text()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 汇总本次对话要检索的知识库ID列表：智能体绑定的 knowledgeIds 优先，回退到请求的 knowledgeId
     */
    private List<Long> collectKnowledgeIds(ChatRequest chatRequest, AgentVo agentVo) {
        if (agentVo != null && agentVo.getKnowledgeIds() != null && !agentVo.getKnowledgeIds().isEmpty()) {
            return agentVo.getKnowledgeIds();
        }
        if (StringUtils.isNotBlank(chatRequest.getKnowledgeId())) {
            try {
                return List.of(Long.valueOf(chatRequest.getKnowledgeId()));
            } catch (NumberFormatException ignored) {
            }
        }
        return List.of();
    }

    /**
     * 构建多知识库复合检索增强器。
     * 单知识库直接用 DefaultRetrievalAugmentor + CustomVectorRetriever；
     * 多知识库用一个复合 ContentRetriever 合并各库检索结果。
     */
    private RetrievalAugmentor buildMultiKnowledgeAugmentor(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return null;
        }
        List<ContentRetriever> retrievers = new ArrayList<>();
        for (Long kid : knowledgeIds) {
            try {
                KnowledgeInfoVo kb = knowledgeInfoService.queryById(kid);
                if (kb == null) {
                    continue;
                }
                ChatModelVo embModel = chatModelService.selectModelByName(kb.getEmbeddingModel());
                if (embModel == null) {
                    log.warn("knowledge_retriever status=SKIPPED reason=EMBEDDING_MODEL_UNAVAILABLE");
                    continue;
                }
                retrievers.add(new CustomVectorRetriever(knowledgeRetrievalService, kb, embModel));
            } catch (Exception e) {
                log.warn("knowledge_retriever operation=BUILD status=FAILED errorType={}", errorType(e));
            }
        }
        if (retrievers.isEmpty()) {
            return null;
        }
        // 单库直接返回；多库用复合检索器
        ContentRetriever composite = retrievers.size() == 1
            ? retrievers.get(0)
            : new CompositeContentRetriever(retrievers);
        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(composite)
            .build();
    }

    /**
     * 复合内容检索器：对多个知识库检索器并发查询并合并结果
     */
    private static class CompositeContentRetriever implements ContentRetriever {
        private final List<ContentRetriever> delegates;

        CompositeContentRetriever(List<ContentRetriever> delegates) {
            this.delegates = delegates;
        }

        @Override
        public List<Content> retrieve(Query query) {
            List<CompletableFuture<List<Content>>> futures = delegates.stream()
                    .map(r -> CompletableFuture.supplyAsync(() -> {
                        try {
                            List<Content> part = r.retrieve(query);
                            return part == null ? List.<Content>of() : part;
                        } catch (Exception e) {
                            log.warn("knowledge_retriever operation=RETRIEVE status=FAILED errorType={}",
                                errorType(e));
                            return List.<Content>of();
                        }
                    })).toList();
            Map<String, Content> unique = new LinkedHashMap<>();
            for (CompletableFuture<List<Content>> future : futures) {
                for (Content content : future.join()) {
                    String key = content.textSegment().metadata().getString("kid") + "|"
                            + content.textSegment().metadata().getString("docId") + "|"
                            + content.textSegment().metadata().getString("fid");
                    if (key.endsWith("null|null|null")) key = content.textSegment().text();
                    unique.putIfAbsent(key, content);
                }
            }
            List<Content> bounded = new ArrayList<>();
            int chars = 0;
            for (Content content : unique.values()) {
                int next = content.textSegment().text().length();
                if (bounded.size() >= 20 || chars + next > 24000) break;
                bounded.add(content);
                chars += next;
            }
            return bounded;
        }
    }

    /**
     * 构建向量查询参数
     */
    private QueryVectorBo buildQueryVectorBo(ChatRequest chatRequest, KnowledgeInfoVo knowledgeInfoVo,
                                             ChatModelVo chatModel) {
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(chatRequest.getContent());
        queryVectorBo.setKid(chatRequest.getKnowledgeId());
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
     * 普通对话响应处理器：推送流式内容、保存助手消息并结束链路追踪。
     */
    private StreamingChatResponseHandler createModelChatResponseHandler(ChatRequest chatRequest,
                                                                         TraceRunHandle traceRun,
                                                                         TraceStreamSpan llmSpan) {
        String sessionId = String.valueOf(chatRequest.getSessionId());
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @Override
            public void onPartialResponse(String partialResponse) {
                messageBuffer.append(partialResponse);
                SseMessageUtils.sendContent(sessionId, partialResponse);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                SseMessageUtils.sendReasoning(sessionId, partialThinking.text());
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    String fullMessage = messageBuffer.toString();
                    if (StringUtils.isNotBlank(fullMessage)) {
                        chatMessageService.saveChatMessage(
                            chatRequest.getUserId(),
                            chatRequest.getSessionId(),
                            fullMessage,
                            RoleType.ASSISTANT.getName(),
                            chatRequest.getModel()
                        );
                    } else {
                        log.warn("chat_stream status=EMPTY_RESPONSE");
                    }
                    if (llmSpan != null) {
                        llmSpan.finishSuccess(RagTracePayloadBuilder.streamOutputSummary(fullMessage.length()));
                    }
                    finishTraceRun(traceRun, TraceConstants.STATUS_SUCCESS, null);
                    SseMessageUtils.sendDone(sessionId);
                } catch (Exception e) {
                    if (llmSpan != null) {
                        llmSpan.finishError(e);
                    }
                    finishTraceRun(traceRun, TraceConstants.STATUS_ERROR, e);
                    SseMessageUtils.sendError(sessionId, SAFE_CHAT_ERROR_MESSAGE);
                    log.error("chat_stream operation=COMPLETE status=FAILED errorType={}", errorType(e));
                } finally {
                    if (llmSpan != null) {
                        llmSpan.detach();
                    }
                    SseMessageUtils.completeConnection(sessionId);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (llmSpan != null) {
                    llmSpan.finishError(error);
                    llmSpan.detach();
                }
                finishTraceRun(traceRun, TraceConstants.STATUS_ERROR, error);
                SseMessageUtils.sendError(sessionId, SAFE_CHAT_ERROR_MESSAGE);
                SseMessageUtils.completeConnection(sessionId);
                log.error("chat_stream operation=MODEL_STREAM status=FAILED errorType={}", errorType(error));
            }
        };
    }

    /**
     * 创建组合响应处理器 - 同时发送到 SSE 和外部 handler
     *
     * @param sessionId       会话ID（SSE 按会话隔离推送）
     * @param externalHandler 外部响应处理器（可为 null）
     * @return 组合的流式响应处理器
     */
    protected StreamingChatResponseHandler createCombinedHandler(String sessionId,
                                                                  StreamingChatResponseHandler externalHandler) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 1. 追加到缓冲区
                messageBuffer.append(partialResponse);

                // 2. 发送内容事件到 SSE（前端可通过 SSE 监听）
                // 工作流调用时连接归工作流引擎所有, token 由引擎以 [NODE_CHUNK_] 事件推送, 不走聊天协议
                if (externalHandler == null) {
                    SseMessageUtils.sendContent(sessionId, partialResponse);
                }

                // 3. 转发给外部 handler（Workflow 等模块可处理）
                if (externalHandler != null) {
                    externalHandler.onPartialResponse(partialResponse);
                }
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                // 发送推理内容到 SSE（前端通过 reasoning 事件监听）, 工作流调用时不发送
                if (externalHandler == null) {
                    SseMessageUtils.sendReasoning(sessionId, partialThinking.text());
                }

                // 转发给外部 handler
                if (externalHandler != null) {
                    externalHandler.onPartialThinking(partialThinking);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 1&2. 发送完成事件并关闭 SSE 连接
                    // 工作流调用时流程可能还有后续节点, 连接关闭由工作流引擎统一负责, 此处不能关闭
                    if (externalHandler == null) {
                        SseMessageUtils.sendDone(sessionId);
                        SseMessageUtils.completeConnection(sessionId);
                    }

                    // 3. 转发给外部 handler
                    if (externalHandler != null) {
                        externalHandler.onCompleteResponse(completeResponse);
                    }
                } catch (Exception e) {
                    log.error("chat_stream operation=COMPLETE status=FAILED errorType={}", errorType(e));
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件（工作流调用时由工作流引擎统一上报）
                if (externalHandler == null) {
                    SseMessageUtils.sendError(sessionId, SAFE_CHAT_ERROR_MESSAGE);
                }
                log.error("chat_stream operation=COMBINED_STREAM status=FAILED errorType={}", errorType(error));

                // 转发给外部 handler
                if (externalHandler != null) {
                    // This is a trusted, in-process callback contract. Preserve the original
                    // throwable identity for workflow recovery; only durable/log/SSE boundaries
                    // redact untrusted exception messages.
                    externalHandler.onError(error);
                }
            }
        };
    }

    private static String errorType(Throwable error) {
        return error == null ? "unknown" : error.getClass().getName();
    }

    static String traceErrorSummary(Throwable error) {
        return error == null ? null : "CHAT_OPERATION_FAILED errorType=" + errorType(error);
    }
}
