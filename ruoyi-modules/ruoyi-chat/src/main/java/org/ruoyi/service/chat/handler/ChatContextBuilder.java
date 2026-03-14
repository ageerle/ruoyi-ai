package org.ruoyi.service.chat.handler;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.ChatMessageDTO;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.common.chat.factory.ChatServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话上下文构建器
 * <p>
 * 负责构建完整的对话上下文，包括：
 * 1. 模型配置查询
 * 2. 知识库检索增强
 * 3. SSE连接创建
 * 4. 用户信息注入
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final IChatModelService chatModelService;
    private final IKnowledgeInfoService knowledgeInfoService;
    private final VectorStoreService vectorStoreService;
    private final SseEmitterManager sseEmitterManager;
    private final ChatServiceFactory chatServiceFactory;

    /**
     * 构建对话上下文
     *
     * @param chatRequest 对话请求
     * @return 完整的对话上下文
     */
    public ChatContext build(ChatRequest chatRequest) {
        // 1. 查询模型配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息（知识库增强）
        List<ChatMessageDTO> contextMessages = buildContextMessages(chatRequest);
        chatRequest.setMessages(contextMessages);

        // 3. 获取用户信息
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();

        // 4. 创建SSE连接
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);

        // 5. 获取服务提供商
        String category = chatModelVo.getProviderCode();
        IChatService chatService = chatServiceFactory.getOriginalService(category);
        log.info("路由到服务提供商: {}, 模型: {}", category, chatRequest.getModel());

        // 6. 构建上下文对象
        return ChatContext.builder()
            .chatModelVo(chatModelVo)
            .chatRequest(chatRequest)
            .emitter(emitter)
            .userId(userId)
            .tokenValue(tokenValue)
            .chatService(chatService)
            .build();
    }

    /**
     * 构建上下文消息列表（知识库增强）
     */
    private List<ChatMessageDTO> buildContextMessages(ChatRequest chatRequest) {
        List<ChatMessageDTO> messages = chatRequest.getMessages();

        // 从向量库查询相关历史消息
        if (chatRequest.getKnowledgeId() != null) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo == null) {
                log.warn("知识库信息不存在，kid: {}", chatRequest.getKnowledgeId());
                return messages;
            }

            // 查询向量模型配置
            ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
            if (chatModel == null) {
                log.warn("向量模型配置不存在，模型名称: {}", knowledgeInfoVo.getEmbeddingModel());
                return messages;
            }

            // 构建向量查询参数并检索
            QueryVectorBo queryVectorBo = buildQueryVectorBo(chatRequest, knowledgeInfoVo, chatModel);
            List<String> nearestList = vectorStoreService.getQueryVector(queryVectorBo);

            // 知识库内容作为系统上下文添加
            for (String prompt : nearestList) {
                messages.add(ChatMessageDTO.system(prompt));
            }
        }

        return messages;
    }

    /**
     * 构建向量查询参数
     */
    private QueryVectorBo buildQueryVectorBo(ChatRequest chatRequest, KnowledgeInfoVo knowledgeInfoVo,
                                             ChatModelVo chatModel) {
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(chatRequest.getMessages().get(0).getContent());
        queryVectorBo.setKid(chatRequest.getKnowledgeId());
        queryVectorBo.setApiKey(chatModel.getApiKey());
        queryVectorBo.setBaseUrl(chatModel.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());
        return queryVectorBo;
    }
}
