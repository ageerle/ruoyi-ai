//package org.ruoyi.service.chat.handler;
//
//import dev.langchain4j.agentic.AgenticServices;
//import dev.langchain4j.community.model.dashscope.QwenChatModel;
//import dev.langchain4j.service.tool.ToolProvider;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ruoyi.agent.McpAgent;
//import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
//import org.ruoyi.common.chat.entity.chat.ChatContext;
//import org.ruoyi.common.chat.service.chatMessage.IChatMessageService;
//import org.ruoyi.common.sse.utils.SseMessageUtils;
//import org.ruoyi.mcp.service.core.ToolProviderFactory;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Agent 深度思考处理器
// * <p>
// * 处理 enableThinking=true 的场景，使用 Agent 进行深度思考和工具调用
// *
// * @author ageerle@163.com
// * @date 2025/12/13
// */
//@Slf4j
//@Component
//@Order(3)
//@RequiredArgsConstructor
//public class AgentChatHandler implements ChatHandler {
//
//    private final ToolProviderFactory toolProviderFactory;
//
//    @Override
//    public boolean supports(ChatContext context) {
//        Boolean enableThinking = context.getChatRequest().getEnableThinking();
//        return enableThinking != null && enableThinking;
//    }
//
//    @Override
//    public SseEmitter handle(ChatContext context) {
//        log.info("处理 Agent 深度思考，用户: {}", context.getUserId());
//
//        Long userId = context.getUserId();
//        String tokenValue = context.getTokenValue();
//        ChatModelVo chatModelVo = context.getChatModelVo();
//
//        try {
//            // 1. 保存用户消息
//            String content = extractUserContent(context);
////            saveChatMessage(context.getChatRequest(), userId, content,
////                RoleType.USER.getName(), chatModelVo);
//
//            // 2. 执行 Agent 任务
//            String result = doAgent(content, chatModelVo);
//
//            // 3. 发送结果并保存
//            SseMessageUtils.sendMessage(userId, result);
//            SseMessageUtils.completeConnection(userId, tokenValue);
//
////            saveChatMessage(context.getChatRequest(), userId, result,
////                RoleType.ASSISTANT.getName(), chatModelVo);
//            // todo 保存消息
//        } catch (Exception e) {
//            log.error("Agent 执行失败: {}", e.getMessage(), e);
//            SseMessageUtils.sendMessage(userId, "Agent 执行失败：" + e.getMessage());
//            SseMessageUtils.completeConnection(userId, tokenValue);
//        }
//
//        return context.getEmitter();
//    }
//
//    /**
//     * 执行 Agent 任务
//     */
//    private String doAgent(String userMessage, ChatModelVo chatModelVo) {
//        log.info("执行 Agent 任务，消息: {}", userMessage);
//
//        try {
//            // 1. 加载 LLM 模型
//            QwenChatModel qwenChatModel = QwenChatModel.builder()
//                .apiKey(chatModelVo.getApiKey())
//                .modelName(chatModelVo.getModelName())
//                .build();
//
//            // 2. 获取内置工具
//            List<Object> builtinTools = toolProviderFactory.getAllBuiltinToolObjects();
//            List<Object> allTools = new ArrayList<>(builtinTools);
//            log.debug("加载 {} 个内置工具", builtinTools.size());
//
//            // 3. 获取 MCP 工具提供者
//            ToolProvider mcpToolProvider = toolProviderFactory.getAllEnabledMcpToolsProvider();
//
//            // 4. 创建 MCP Agent
//            var agentBuilder = AgenticServices.agentBuilder(McpAgent.class)
//                .chatModel(qwenChatModel);
//
//            if (!allTools.isEmpty()) {
//                agentBuilder.tools(allTools.toArray(new Object[0]));
//            }
//            if (mcpToolProvider != null) {
//                agentBuilder.toolProvider(mcpToolProvider);
//            }
//
//            McpAgent mcpAgent = agentBuilder.build();
//
//            // 5. 调用 Agent
//            String result = mcpAgent.callMcpTool(userMessage);
//            log.info("Agent 执行完成，结果长度: {}", result.length());
//            return result;
//
//        } catch (Exception e) {
//            log.error("Agent 模式执行失败: {}", e.getMessage(), e);
//            return "Agent 执行失败: " + e.getMessage();
//        }
//    }
//
//    /**
//     * 提取用户消息内容
//     */
//    private String extractUserContent(ChatContext context) {
//        var messages = context.getChatRequest().getMessages();
//        if (messages != null && !messages.isEmpty()) {
//            return messages.get(0).getContent();
//        }
//        return "";
//    }
//
//}
