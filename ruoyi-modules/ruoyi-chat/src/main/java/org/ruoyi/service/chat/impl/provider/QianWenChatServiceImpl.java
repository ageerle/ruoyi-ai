package org.ruoyi.service.chat.impl.provider;

import java.util.ArrayList;
import java.util.List;

import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.config.McpSseConfig;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * qianWenAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class QianWenChatServiceImpl extends AbstractStreamingChatService {


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
     * @param userMessage 用户信息
     * @param chatModelVo 模型信息
     * @return 返回LLM信息
     */
    protected String doAgent(String userMessage,ChatModelVo chatModelVo) {
   
        // 步骤4：加载LLM模型对话
        QwenChatModel qwenChatModel = QwenChatModel.builder()
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .build();
        McpTransport echart = new StdioMcpTransport.Builder()
            .command(List.of("uv",
                "--directory",
                "/Users/zhangmingming/data/coder/LLM/MCP/cicd-pipeline-example/",
                "run",
                "text2sql-mcp"
            ))
            .logEvents(true)
            .build();

        // 步骤2: 创建MCP客户端
        McpClient echartClient = new DefaultMcpClient.Builder()
            .transport(echart)
            .build();

        // 步骤3: 配置工具提供者
        ToolProvider echartToolProvider = McpToolProvider.builder()
            .mcpClients(List.of(
                        echartClient))
                .build();
            
        EchartsAgent chartGenerationAgent = AgenticServices.agentBuilder(
                EchartsAgent.class)
            .chatModel(qwenChatModel)
            .toolProvider(echartToolProvider)
            .build();


        // 步骤6：将所有MCP对象由超级智能体管控
        SupervisorAgent supervisor = AgenticServices
            .supervisorBuilder()
            .chatModel(qwenChatModel)
            .subAgents(chartGenerationAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        // 步骤7：调用大模型LLM
        return supervisor.invoke(userMessage);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
