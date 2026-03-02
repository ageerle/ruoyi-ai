package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.McpAgent;
import org.ruoyi.config.McpSseConfig;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * qianWenAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class QianWenChatServiceImpl extends AbstractStreamingChatService {

    @Autowired
    private McpSseConfig mcpSseConfig;

    // 添加文档解析的前缀字段
    private static final String UPLOAD_FILE_API_PREFIX = "fileid";

    // 缓存不同API Key和模型的MCP智能体实例
    private final ConcurrentHashMap<String, SupervisorAgent> supervisorCache = new ConcurrentHashMap<>();

    // 缓存不同API Key和模型的MCP客户端实例
    private final ConcurrentHashMap<String, McpClient> mcpClientCache = new ConcurrentHashMap<>();

    // 缓存不同API Key和模型的MCP工具提供者实例
    private final ConcurrentHashMap<String, ToolProvider> toolProviderCache = new ConcurrentHashMap<>();
    // 用于线程安全的锁
    private final ReentrantLock cacheLock = new ReentrantLock();

    @Override
    protected StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        return QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();
    }

    @Override
    protected void doChat(ChatModelVo chatModelVo,ChatRequest chatRequest,List<ChatMessage> messagesWithMemory,
                          StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo,chatRequest);
        // 判断是否存在需要使用阿里千问的文档解析功能
        List<ChatMessage> chatMessages = hasFileIdData(messagesWithMemory);
        streamingChatModel.chat(chatMessages, handler);
    }

    /**
     * 检查是否包含fileId数据
     */
    private List<ChatMessage> hasFileIdData(List<ChatMessage> messagesWithMemory) {
        if (CollectionUtils.isEmpty(messagesWithMemory)) {
            return messagesWithMemory;
        }

        // 找到包含阿里上传文件前缀的用户信息
        var foundUserMessage = messagesWithMemory.stream()
            .filter(message -> message instanceof UserMessage)
            .map(message -> (UserMessage) message)
            .filter(userMessage ->
                userMessage.singleText().toLowerCase().contains(UPLOAD_FILE_API_PREFIX.toLowerCase())
            )
            .findFirst();

        // 找到原本SystemMessage
        var systemMessage = messagesWithMemory.stream()
            .filter(message -> message instanceof SystemMessage)
            .map(message -> (SystemMessage) message)
            .findFirst();

        // 判断是否存在并重新构建信息体(符合千问文档解析格式)
        return foundUserMessage.map(userMsg -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(userMsg.singleText()));
            systemMessage.ifPresent(sysMsg -> messages.add(new UserMessage(sysMsg.text())));
            return messages;
        }).orElse(messagesWithMemory);
    }

    /**
     * 获取缓存键
     */
    private String getCacheKey(ChatModelVo chatModelVo) {
        return chatModelVo.getApiKey() + ":" + chatModelVo.getModelName();
    }

    /**
     * 初始化MCP客户端连接
     */
    private McpClient initializeMcpClient() {
        // 步骤1：根据SSE对外暴露端点连接
        McpTransport httpMcpTransport = new StreamableHttpMcpTransport.Builder().
            url(mcpSseConfig.getUrl()).
            logRequests(true).
            build();

        // 步骤2：开启客户端连接
        return new DefaultMcpClient.Builder()
            .transport(httpMcpTransport)
            .build();
    }

    /**
     * 调用MCP服务（智能体）
     * @param userMessage 用户信息
     * @param chatModelVo 模型信息
     * @return 返回LLM信息
     */
    protected String doAgent(String userMessage, ChatModelVo chatModelVo) {
        // 判断是否开启MCP服务
        if (!mcpSseConfig.isEnabled()) {
            return "";
        }
        // 生成缓存键
        String cacheKey = getCacheKey(chatModelVo);
        // 尝试从缓存获取监督智能体
        SupervisorAgent cachedSupervisor = supervisorCache.get(cacheKey);
        if (cachedSupervisor != null) {
            // 如果已存在缓存的监督智能体，直接使用
            return cachedSupervisor.invoke(userMessage);
        }
        cacheLock.lock();
        try {
            // 双重检查，防止并发情况下的重复初始化
            cachedSupervisor = supervisorCache.get(cacheKey);
            if (cachedSupervisor != null) {
                return cachedSupervisor.invoke(userMessage);
            }

            // 获取或初始化MCP客户端
            McpClient mcpClient = mcpClientCache.computeIfAbsent(cacheKey, k -> initializeMcpClient());

            // 步骤3：将mcp对象包装
            ToolProvider toolProvider = toolProviderCache.computeIfAbsent(cacheKey, k -> McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build());

            // 步骤4：加载LLM模型对话
            QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();

            // 步骤5：将MCP对象由智能体Agent管控
            McpAgent mcpAgent = AgenticServices.agentBuilder(McpAgent.class)
                .chatModel(qwenChatModel)
                .toolProvider(toolProvider)
                .build();

            // 步骤6：将所有MCP对象由超级智能体管控
            SupervisorAgent supervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(qwenChatModel)
                .subAgents(mcpAgent)
                .responseStrategy(SupervisorResponseStrategy.LAST)
                .build();

            // 缓存监督智能体
            supervisorCache.put(cacheKey, supervisor);

            // 步骤7：调用大模型LLM
            return supervisor.invoke(userMessage);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
