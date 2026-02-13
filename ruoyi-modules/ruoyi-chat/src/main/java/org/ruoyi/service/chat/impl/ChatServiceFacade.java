package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.Service.IChatModelService;
import org.ruoyi.common.chat.Service.IChatService;
import org.ruoyi.common.chat.domain.dto.ChatMessageDTO;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.factory.ChatServiceFactory;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天服务业务实现
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceFacade {

    private final IChatModelService chatModelService;

    private final ChatServiceFactory chatServiceFactory;

    private final IKnowledgeInfoService knowledgeInfoService;

    private final VectorStoreService vectorStoreService;

    private final SseEmitterManager sseEmitterManager;

    /**
     * 统一聊天入口 - SSE流式响应
     *
     * @param chatRequest 聊天请求
     * @param request HTTP 请求对象
     * @return SseEmitter
     */
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息列表
        List<ChatMessageDTO> contextMessages = buildContextMessages(chatRequest);
        chatRequest.setMessages(contextMessages);

        // 3. 路由服务提供商
        String category = chatModelVo.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", category, chatRequest.getModel());
        IChatService chatService = chatServiceFactory.getOriginalService(category);

        // 4. 具体的服务实现
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);
        return chatService.chat(chatModelVo, chatRequest,emitter,userId, tokenValue);
    }

    /**
     * 构建上下文消息列表
     *
     * @param chatRequest 聊天请求
     * @return 上下文消息列表
     */
    private List<ChatMessageDTO> buildContextMessages(ChatRequest chatRequest) {

        List<ChatMessageDTO> messages = chatRequest.getMessages();

        // 从向量库查询相关历史消息
        if (chatRequest.getKnowledgeId() != null) {
            // 查询知识库信息
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo == null) {
                log.warn("知识库信息不存在，kid: {}", chatRequest.getKnowledgeId());
                return messages;
            }

            // 查询向量模型配置信息
            ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
            if (chatModel == null) {
                log.warn("向量模型配置不存在，模型名称: {}", knowledgeInfoVo.getEmbeddingModel());
                return messages;
            }

            // 构建向量查询参数
            QueryVectorBo queryVectorBo = buildQueryVectorBo(chatRequest, knowledgeInfoVo, chatModel);

            // 获取向量查询结果
            List<String> nearestList = vectorStoreService.getQueryVector(queryVectorBo);
            for (String prompt : nearestList) {
                // 知识库内容作为系统上下文添加
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

