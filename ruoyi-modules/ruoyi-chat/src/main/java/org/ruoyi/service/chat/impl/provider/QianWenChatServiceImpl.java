package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.McpAgent;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.util.ArrayList;
import java.util.List;

/**
 * qianWenAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class QianWenChatServiceImpl extends AbstractStreamingChatService {

    /**
     * 千问开发者默认地址
     */
    private static final String QWEN_API_HOST = "https://dashscope.aliyuncs.com/api/v1";

    // 添加文档解析的前缀字段
    private static final String UPLOAD_FILE_API_PREFIX = "fileid";

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
     * 调用MCP服务（智能体）
     * 使用统一的ToolProviderFactory获取所有已配置的工具（BUILTIN + MCP）
     *
     * @param userMessage 用户信息
     * @param chatModelVo 模型信息
     * @return 返回LLM信息
     */
    protected String doAgent(String userMessage, ChatModelVo chatModelVo) {
        // 步骤1: 获取统一工具提供工厂
        ToolProviderFactory toolProviderFactory = SpringUtils.getBean(ToolProviderFactory.class);

        // 步骤2: 获取 BUILTIN 工具对象
        List<Object> builtinTools = toolProviderFactory.getAllBuiltinToolObjects();

        // 步骤3: 获取 MCP 工具提供者
        ToolProvider mcpToolProvider = toolProviderFactory.getAllEnabledMcpToolsProvider();

        log.info("doAgent: BUILTIN tools count = {}, MCP tools enabled = {}",
            builtinTools.size(), mcpToolProvider != null);

        // 步骤4: 加载LLM模型
        QwenChatModel qwenChatModel = QwenChatModel.builder()
            .baseUrl(QWEN_API_HOST)
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .build();

        // 步骤5: 创建MCP Agent，使用所有已配置的工具
        // 使用 .tools() 传入 BUILTIN 工具对象（Java 对象，带 @Tool 注解的方法）
        // 使用 .toolProvider() 传入 MCP 工具提供者（MCP 协议工具）
        var agentBuilder = AgenticServices.agentBuilder(McpAgent.class)
            .chatModel(qwenChatModel);

        // 添加 BUILTIN 工具（如果有）
        if (!builtinTools.isEmpty()) {
            agentBuilder.tools(builtinTools.toArray(new Object[0]));
            log.debug("Added {} BUILTIN tools to agent", builtinTools.size());
        }

        // 添加 MCP 工具（如果有）
        if (mcpToolProvider != null) {
            agentBuilder.toolProvider(mcpToolProvider);
            log.debug("Added MCP tool provider to agent");
        }

        McpAgent mcpAgent = agentBuilder.build();

        // 步骤6: 创建超级智能体协调MCP Agent
        SupervisorAgent supervisor = AgenticServices
            .supervisorBuilder()
            .chatModel(qwenChatModel)
            .subAgents(mcpAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        // 步骤7: 调用大模型LLM
        return supervisor.invoke(userMessage);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
