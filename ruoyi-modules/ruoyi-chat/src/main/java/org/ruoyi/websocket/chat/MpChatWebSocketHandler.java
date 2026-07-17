package org.ruoyi.websocket.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.agent.IAgentService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 小程序对话 WebSocket 处理器。
 * <p>
 * 收到前端 JSON 消息后：解析模型（智能体绑定 / 前端传入 / 默认兜底）→
 * 拼装 systemPrompt 与 RAG 增强后的 content → 调用 StreamingChatModel 流式生成 →
 * 将增量 token 通过当前 WS session 回推前端。
 * <p>
 * 输出协议（与前端 index.vue 现有接收逻辑兼容）：
 * <ul>
 *   <li>增量：<code>{"content":"token片段"}</code></li>
 *   <li>结束：<code>[DONE]</code></li>
 *   <li>错误：<code>{"data":"错误:xxx"}</code></li>
 * </ul>
 *
 * @author ruoyi team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MpChatWebSocketHandler extends AbstractWebSocketHandler {

    private final ChatServiceFactory chatServiceFactory;
    private final IChatModelService chatModelService;
    private final IAgentService agentService;
    private final IKnowledgeInfoService knowledgeInfoService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final IChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    @Value("${chat.default-model:}")
    private String defaultModel;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            sendError(session, "错误:消息格式不正确");
            return;
        }
        String content = asString(payload.get("content"));
        String agentIdRaw = asString(payload.get("agentId"));
        String model = asString(payload.get("model"));
        String systemPrompt = asString(payload.get("systemPrompt"));
        String knowledgeId = asString(payload.get("knowledgeId"));
        String sessionIdRaw = asString(payload.get("sessionId"));

        if (StringUtils.isBlank(content)) {
            sendError(session, "错误:对话消息不能为空");
            return;
        }

        Long userId = (Long) session.getAttributes().get(MpChatHandshakeInterceptor.USER_ID_KEY);
        Long sessionId = parseLong(sessionIdRaw);

        try {
            // 1. 解析智能体（若传了 agentId），取其绑定模型与 systemPrompt、知识库
            AgentVo agentVo = null;
            if (StringUtils.isNotBlank(agentIdRaw)) {
                Long agentId = parseLong(agentIdRaw);
                if (agentId != null) {
                    agentVo = agentService.queryById(agentId);
                }
            }

            // 2. 解析模型：智能体绑定 > 前端传入 > 默认配置 > 表内首个 chat 模型
            ChatModelVo modelVo = null;
            if (agentVo != null && agentVo.getModelId() != null) {
                modelVo = chatModelService.queryById(agentVo.getModelId());
            }
            if (modelVo == null && StringUtils.isNotBlank(model)) {
                modelVo = chatModelService.selectModelByName(model);
            }
            if (modelVo == null) {
                modelVo = resolveDefaultModel();
            }
            if (modelVo == null) {
                sendError(session, "错误:未找到可用对话模型，请联系管理员配置");
                return;
            }

            // 3. 拼装最终输入：RAG 增强 + systemPrompt 前置
            String finalSystemPrompt = (agentVo != null && StringUtils.isNotBlank(agentVo.getSystemPrompt()))
                ? agentVo.getSystemPrompt() : systemPrompt;
            String augmentedContent = augmentWithKnowledge(content, agentVo, knowledgeId);
            String finalContent = StringUtils.isNotBlank(finalSystemPrompt)
                ? finalSystemPrompt + "\n\n" + augmentedContent : augmentedContent;

            // 4. 落库用户消息（仅在具备用户与会话标识时）
            if (userId != null && sessionId != null) {
                try {
                    chatMessageService.saveChatMessage(userId, sessionId, content,
                        RoleType.USER.getName(), modelVo.getModelName());
                } catch (Exception e) {
                    log.warn("落库用户消息失败: {}", e.getMessage());
                }
            }

            // 5. 构造流式模型并异步生成
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setContent(content);
            chatRequest.setModel(modelVo.getModelName());
            chatRequest.setKnowledgeId(knowledgeId);
            StreamingChatModel streamingModel = chatServiceFactory
                .getOriginalService(modelVo.getProviderCode())
                .buildStreamingChatModel(modelVo, chatRequest);

            final String modelName = modelVo.getModelName();
            CompletableFuture.runAsync(() -> {
                StringBuilder buffer = new StringBuilder();
                // 是否已向前端发送 [DONE] 结束标记，避免重复发送或遗漏
                boolean[] doneSent = {false};
                StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        buffer.append(partialResponse);
                        sendJson(session, Map.of("content", partialResponse));
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        if (!doneSent[0]) {
                            doneSent[0] = true;
                            sendRaw(session, "[DONE]");
                        }
                        if (userId != null && sessionId != null && buffer.length() > 0) {
                            try {
                                chatMessageService.saveChatMessage(userId, sessionId, buffer.toString(),
                                    RoleType.ASSISTANT.getName(), modelName);
                            } catch (Exception e) {
                                log.warn("落库助手回复失败: {}", e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (buffer.length() == 0) {
                            // 一点内容都没输出就出错：向前端报错
                            sendError(session, "错误:" + safeMsg(error));
                        } else if (!doneSent[0]) {
                            // 已有部分内容但流式中途异常：补发 [DONE] 让前端正常收尾，不报错
                            doneSent[0] = true;
                            sendRaw(session, "[DONE]");
                            log.warn("mp-chat 流式中途异常（已输出内容，补发 [DONE]）: {}", safeMsg(error));
                        } else {
                            // onComplete 后的收尾异常：回复已正常结束，静默
                            log.warn("mp-chat 流式收尾异常（已结束，忽略）: {}", safeMsg(error));
                        }
                    }
                };
                try {
                    streamingModel.chat(finalContent, handler);
                } catch (Exception e) {
                    log.error("mp-chat 调用模型失败", e);
                    sendError(session, "错误:" + safeMsg(e));
                }
            });
        } catch (Exception e) {
            log.error("mp-chat 处理消息失败", e);
            sendError(session, "错误:" + safeMsg(e));
        }
    }

    /**
     * 智能体绑定知识库 / 前端传入 knowledgeId 时，对 content 做向量检索增强。
     * 复用 ChatServiceFacade.buildMultiKnowledgeAugmentor 的组装方式（简化为多库复合检索）。
     */
    private String augmentWithKnowledge(String content, AgentVo agentVo, String knowledgeId) {
        List<Long> kids = new ArrayList<>();
        if (agentVo != null && agentVo.getKnowledgeIds() != null) {
            kids.addAll(agentVo.getKnowledgeIds());
        }
        if (StringUtils.isBlank(knowledgeId) && kids.isEmpty()) {
            return content;
        }
        if (StringUtils.isNotBlank(knowledgeId)) {
            try {
                kids.add(Long.valueOf(knowledgeId));
            } catch (NumberFormatException ignored) {
            }
        }
        if (kids.isEmpty()) {
            return content;
        }
        try {
            RetrievalAugmentor augmentor = buildMultiKnowledgeAugmentor(kids);
            if (augmentor == null) {
                return content;
            }
            UserMessage userMessage = UserMessage.userMessage(content);
            Metadata metadata = Metadata.from(userMessage, null, new ArrayList<>());
            AugmentationResult result = augmentor.augment(new AugmentationRequest(userMessage, metadata));
            ChatMessage augmented = result.chatMessage();
            return augmented instanceof UserMessage ? ((UserMessage) augmented).singleText() : content;
        } catch (Exception e) {
            log.warn("mp-chat RAG 增强失败，回退原文: {}", e.getMessage());
            return content;
        }
    }

    private RetrievalAugmentor buildMultiKnowledgeAugmentor(List<Long> knowledgeIds) {
        List<ContentRetriever> retrievers = new ArrayList<>();
        for (Long kid : knowledgeIds) {
            try {
                KnowledgeInfoVo kb = knowledgeInfoService.queryById(kid);
                if (kb == null) {
                    continue;
                }
                ChatModelVo embModel = chatModelService.selectModelByName(kb.getEmbeddingModel());
                if (embModel == null) {
                    log.warn("mp-chat 知识库向量模型未配置: kid={}, emb={}", kid, kb.getEmbeddingModel());
                    continue;
                }
                retrievers.add(new CustomVectorRetriever(knowledgeRetrievalService, kb, embModel));
            } catch (Exception e) {
                log.warn("mp-chat 构建检索器失败: kid={}, err={}", kid, e.getMessage());
            }
        }
        if (retrievers.isEmpty()) {
            return null;
        }
        ContentRetriever composite = retrievers.size() == 1
            ? retrievers.get(0)
            : new CompositeContentRetriever(retrievers);
        return DefaultRetrievalAugmentor.builder().contentRetriever(composite).build();
    }

    /**
     * 默认模型兜底：优先用 chat.default-model 配置，其次取表内首个 chat 类模型。
     */
    private ChatModelVo resolveDefaultModel() {
        if (StringUtils.isNotBlank(defaultModel)) {
            ChatModelVo vo = chatModelService.selectModelByName(defaultModel);
            if (vo != null) {
                return vo;
            }
        }
        try {
            List<ChatModelVo> list = chatModelService.queryList(new ChatModelBo());
            if (list != null) {
                for (ChatModelVo vo : list) {
                    if ("chat".equalsIgnoreCase(vo.getCategory()) && "Y".equalsIgnoreCase(vo.getModelShow())) {
                        return vo;
                    }
                }
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
        } catch (Exception e) {
            log.warn("mp-chat 解析默认模型失败: {}", e.getMessage());
        }
        return null;
    }

    // ---------- WS 输出辅助 ----------

    private void sendJson(WebSocketSession session, Map<String, ?> data) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            log.warn("mp-chat 发送 WS 消息失败: {}", e.getMessage());
        }
    }

    private void sendRaw(WebSocketSession session, String raw) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(raw));
        } catch (Exception e) {
            log.warn("mp-chat 发送 WS 消息失败: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("data", msg);
        sendJson(session, err);
    }

    private static String safeMsg(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long parseLong(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 多知识库复合检索器：并发查询各库并合并结果。
     * （与 ChatServiceFacade 内部 CompositeContentRetriever 同构，独立保留以解耦公共门面）
     */
    private static class CompositeContentRetriever implements ContentRetriever {
        private final List<ContentRetriever> delegates;

        CompositeContentRetriever(List<ContentRetriever> delegates) {
            this.delegates = delegates;
        }

        @Override
        public List<Content> retrieve(Query query) {
            List<Content> all = new ArrayList<>();
            for (ContentRetriever r : delegates) {
                try {
                    List<Content> part = r.retrieve(query);
                    if (part != null) {
                        all.addAll(part);
                    }
                } catch (Exception e) {
                    log.warn("mp-chat 复合检索子检索器异常: {}", e.getMessage());
                }
            }
            return all;
        }
    }
}
