package org.ruoyi.service.chat.handler;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.McpAgent;
import org.ruoyi.common.chat.domain.bo.chat.ChatMessageBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chatMessage.IChatMessageService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 深度思考处理器
 * <p>
 * 处理 enableThinking=true 的场景，使用 Agent 进行深度思考和工具调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class AgentChatHandler implements ChatHandler {

    private final ToolProviderFactory toolProviderFactory;
    private final IChatMessageService chatMessageService;

    @Override
    public boolean supports(ChatContext context) {
        Boolean enableThinking = context.getChatRequest().getEnableThinking();
        return enableThinking != null && enableThinking;
    }

    @Override
    public SseEmitter handle(ChatContext context) {
        log.info("处理 Agent 深度思考，用户: {}", context.getUserId());

        Long userId = context.getUserId();
        String tokenValue = context.getTokenValue();
        ChatModelVo chatModelVo = context.getChatModelVo();

        try {
            // 1. 保存用户消息
            String content = extractUserContent(context);
            saveChatMessage(context.getChatRequest(), userId, content,
                RoleType.USER.getName(), chatModelVo);

            // 2. 执行 Agent 任务
            String result = doAgent(content, chatModelVo);

            // 3. 发送结果并保存
            SseMessageUtils.sendMessage(userId, result);
            SseMessageUtils.completeConnection(userId, tokenValue);
            saveChatMessage(context.getChatRequest(), userId, result,
                RoleType.ASSISTANT.getName(), chatModelVo);

        } catch (Exception e) {
            log.error("Agent 执行失败: {}", e.getMessage(), e);
            SseMessageUtils.sendMessage(userId, "Agent 执行失败：" + e.getMessage());
            SseMessageUtils.completeConnection(userId, tokenValue);
        }

        return context.getEmitter();
    }

    /**
     * 执行 Agent 任务
     */
    private String doAgent(String userMessage, ChatModelVo chatModelVo) {
        log.info("执行 Agent 任务，消息: {}", userMessage);

        try {
            // 1. 加载 LLM 模型
            ChatModel chatModel = generateChatModel(chatModelVo);
            if (chatModel == null) {
                return "Agent 执行失败: LLM 模型创建失败";
            }

            // 2. 获取内置工具
            List<Object> builtinTools = toolProviderFactory.getAllBuiltinToolObjects();
            List<Object> allTools = new ArrayList<>(builtinTools);
            log.debug("加载 {} 个内置工具", builtinTools.size());

            // 3. 获取 MCP 工具提供者
            ToolProvider mcpToolProvider = toolProviderFactory.getAllEnabledMcpToolsProvider();

            // 4. 创建 MCP Agent
            var agentBuilder = AgenticServices.agentBuilder(McpAgent.class)
                .chatModel(chatModel);

            if (!allTools.isEmpty()) {
                agentBuilder.tools(allTools.toArray(new Object[0]));
            }
            if (mcpToolProvider != null) {
                agentBuilder.toolProvider(mcpToolProvider);
            }

            McpAgent mcpAgent = agentBuilder.build();

            // 5. 调用 Agent
            String result = mcpAgent.callMcpTool(userMessage);
            log.info("Agent 执行完成，结果长度: {}", result.length());
            return result;

        } catch (Exception e) {
            log.error("Agent 模式执行失败: {}", e.getMessage(), e);
            return "Agent 执行失败: " + e.getMessage();
        }
    }


    /**
     * 根据 providerCode 加载对应的 LLM 模型
     * @param chatModelVo
     * @return LLM 模型
     */
    private ChatModel generateChatModel(ChatModelVo chatModelVo) {
        ChatModel targetChatModel = null;
        String providerCode = chatModelVo.getProviderCode();

        if (StringUtils.isBlank(providerCode) || ChatModeType.QIAN_WEN.getCode().equals(providerCode.toLowerCase())) {
            // 默认使用千问服务
            targetChatModel = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();
        } else if (ChatModeType.DEEP_SEEK.getCode().equals(providerCode.toLowerCase())) {
            targetChatModel = OpenAiChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build();
        } else if (ChatModeType.OPEN_AI.getCode().equals(providerCode.toLowerCase())) {
            targetChatModel = OpenAiChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build();
        } else if (ChatModeType.OLLAMA.getCode().equals(providerCode.toLowerCase())) {
            targetChatModel = OllamaChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build();
        } else if (ChatModeType.ZHI_PU.getCode().equals(providerCode.toLowerCase())) {
            targetChatModel = ZhipuAiChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .baseUrl(chatModelVo.getApiHost())
                .model(chatModelVo.getModelName())
                .build();
        } else {
            log.error("未识别有效的模型提供商: providerCode-{}", providerCode);
        }

        return targetChatModel;
    }

    /**
     * 提取用户消息内容
     */
    private String extractUserContent(ChatContext context) {
        var messages = context.getChatRequest().getMessages();
        if (messages != null && !messages.isEmpty()) {
            return messages.get(0).getContent();
        }
        return "";
    }

    /**
     * 保存聊天消息
     */
    private void saveChatMessage(org.ruoyi.common.chat.domain.dto.request.ChatRequest chatRequest,
                                  Long userId, String content, String role, ChatModelVo chatModelVo) {
        try {
            if (chatRequest == null || userId == null) {
                log.warn("缺少必要的聊天上下文信息，无法保存消息");
                return;
            }

            ChatMessageBo messageBO = new ChatMessageBo();
            messageBO.setUserId(userId);
            messageBO.setSessionId(chatRequest.getSessionId());
            messageBO.setContent(content);
            messageBO.setRole(role);
            messageBO.setModelName(chatRequest.getModel());
            messageBO.setRemark(null);

            chatMessageService.insertByBo(messageBO);
        } catch (Exception e) {
            log.error("保存聊天消息时出错: {}", e.getMessage(), e);
        }
    }
}
