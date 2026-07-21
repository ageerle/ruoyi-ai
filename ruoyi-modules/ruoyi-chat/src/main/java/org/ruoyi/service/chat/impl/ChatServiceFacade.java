package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.shell.ShellSkills;
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
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.SkillsAgent;
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
import org.ruoyi.config.agent.SkillsPathResolver;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.observability.*;
import org.ruoyi.service.agent.IAgentService;
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
import java.util.LinkedHashMap;
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

    private final IAgentService agentService;

    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;

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

        // 0. 智能体解析：传入 agentId 时按智能体绑定的模型覆盖 model 字段
        //    （前端默认走智能体；enableThinking 不再作为对话模式开关，Supervisor 多 Agent 编排成为默认智能体路径）
        AgentVo agentVo = null;
        if (chatRequest.getAgentId() != null) {
            agentVo = agentService.queryById(chatRequest.getAgentId());
            if (agentVo != null && agentVo.getModelId() != null) {
                ChatModelVo agentModel = chatModelService.queryById(agentVo.getModelId());
                if (agentModel != null) {
                    chatRequest.setModel(agentModel.getModelName());
                }
            } else {
                log.warn("智能体不存在或未配置模型，回退到 model 字段: agentId={}", chatRequest.getAgentId());
            }
        }

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息列表（系统提示词 + 历史消息 + 当前用户消息）
        //    注意：RAG 检索增强统一在 handleAgentChat 中执行一次，此处不再重复检索
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest, agentVo);

        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 3. 路由对话模式：工作流对话 / 智能体对话（两者均返回各自的 SseEmitter）
        return handleSpecialChatModes(chatRequest, agentVo);
    }

    /**
     * 路由对话模式：仅两种情况——工作流对话 / 智能体对话。
     *
     * @param chatRequest 聊天请求
     * @param agentVo    智能体配置（可为 null）
     * @return 对应模式的 SseEmitter
     */
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest, AgentVo agentVo) {
        // 模式1：工作流对话（前端应用市场选工作流后携带 workFlowRunner）
        if (Boolean.TRUE.equals(chatRequest.getEnableWorkFlow())) {
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
        // 模式2：智能体对话（默认走 Supervisor 多 Agent 编排）
        return handleAgentChat(chatRequest, agentVo);
    }

    /**
     * 智能体对话模式（默认）：构建 Supervisor 多 Agent 编排并异步执行，结果通过 SSE 推送。
     *
     * @param chatRequest 聊天请求
     * @param agentVo    智能体配置（可为 null，无智能体时用请求 model 兜底）
     */
    private SseEmitter handleAgentChat(ChatRequest chatRequest, AgentVo agentVo) {
        ChatModelVo chatModelVo = chatRequest.getChatModelVo();

        // 配置监督者模型：统一按 providerCode 走对应 AbstractChatService.buildChatModel，
        // 兼容 ZhiPu/QianWen/Ollama/Dify/Coze/CustomApi 等非 OpenAI 协议；默认实现为 OpenAI 兼容。
        AbstractChatService chatService = chatServiceFactory.getOriginalService(chatModelVo.getProviderCode());
        ChatModel plannerModel = chatService.buildChatModel(chatModelVo);

        Long userId = chatRequest.getUserId();

        // 工具装配：智能体有关联工具ID时按ID装配，否则回退到原有硬编码 MCP 客户端
        ToolProvider toolProvider;
        if (agentVo != null && agentVo.getMcpToolIds() != null && !agentVo.getMcpToolIds().isEmpty()) {
            toolProvider = langChain4jMcpToolProviderService.getToolProvider(agentVo.getMcpToolIds());
        } else {
            toolProvider = buildDefaultMcpToolProvider(userId);
        }

        // Skills 装配：智能体有勾选技能名时按名过滤磁盘 skills，否则加载全部
        ShellSkills skills = buildShellSkills(agentVo);

        // 构建子 Agent
        WebSearchAgent searchAgent  = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider)
            .listener(new MyAgentListener())
            .build();

        // SkillsAgent：仅当有可用 skills 时才注入 systemMessage + toolProvider
        var skillsAgentBuilder = AgenticServices.agentBuilder(SkillsAgent.class)
            .chatModel(plannerModel);
        if (skills != null) {
            skillsAgentBuilder
                .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                    + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
                .toolProvider(skills.toolProvider());
        }
        SkillsAgent skillsAgent = skillsAgentBuilder.build();

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

        // 构建监督者 Agent - 管理多个子 Agent
        var supervisorBuilder = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            .subAgents(skillsAgent, searchAgent, sqlAgent, chartGenerationAgent, echartsAgent)
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

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(prompt);
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
                // 保存助手回复到数据库（智能体对话为默认路径后，需在此落库以保留历史）
                if (StringUtils.isNotBlank(result)) {
                    chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
                        result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                }
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
     * 兜底 MCP 工具装配（无智能体时使用，保留原有 3 个硬编码客户端逻辑）
     */
    private ToolProvider buildDefaultMcpToolProvider(Long userId) {
        String npxCommand = resolveNpxCommand();
        McpTransport playwrightTransport = new StdioMcpTransport.Builder()
            .command(List.of(npxCommand, "-y", "@playwright/mcp@latest"))
            .logEvents(true)
            .build();
        McpClient playwrightMcpClient = new DefaultMcpClient.Builder()
            .transport(playwrightTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        String userDir = System.getProperty("user.dir");
        McpTransport filesystemTransport = new StdioMcpTransport.Builder()
            .command(List.of(npxCommand, "-y",
                "@modelcontextprotocol/server-filesystem", userDir))
            .logEvents(true)
            .build();
        McpClient filesystemMcpClient = new DefaultMcpClient.Builder()
            .transport(filesystemTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        return McpToolProvider.builder()
            .mcpClients(List.of(playwrightMcpClient, filesystemMcpClient))
            .build();
    }

    private String resolveNpxCommand() {
        String configured = System.getProperty("mcp.npx.command");
        if (StringUtils.isNotBlank(configured)) return configured;
        String fromEnv = System.getenv("MCP_NPX_COMMAND");
        if (StringUtils.isNotBlank(fromEnv)) return fromEnv;
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "npx.cmd" : "npx";
    }

    /**
     * 装配磁盘 ShellSkills：智能体勾选了技能名时按名过滤，否则加载全部。
     * 无 skills 时返回 null（调用方据此跳过 SkillsAgent 的 toolProvider 注入）
     */
    private ShellSkills buildShellSkills(AgentVo agentVo) {
        java.nio.file.Path skillsPath = SkillsPathResolver.resolveSkillsPath();
        List<FileSystemSkill> skillsList = FileSystemSkillLoader.loadSkills(skillsPath);
        if (skillsList == null || skillsList.isEmpty()) {
            return null;
        }
        if (agentVo != null && agentVo.getSkillNames() != null && !agentVo.getSkillNames().isEmpty()) {
            skillsList = skillsList.stream()
                .filter(s -> agentVo.getSkillNames().contains(s.name()))
                .toList();
            if (skillsList.isEmpty()) {
                return null;
            }
        }
        return ShellSkills.from(skillsList);
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
            log.warn("智能体对话 RAG 增强失败，回退原始输入: {}", e.getMessage());
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
                    log.warn("知识库向量模型未配置或不存在: kid={}, embeddingModel={}", kid, kb.getEmbeddingModel());
                    continue;
                }
                retrievers.add(new CustomVectorRetriever(knowledgeRetrievalService, kb, embModel));
            } catch (Exception e) {
                log.warn("构建知识库检索器失败: kid={}, err={}", kid, e.getMessage());
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
                            log.warn("复合检索子检索器异常: {}", e.getMessage());
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

