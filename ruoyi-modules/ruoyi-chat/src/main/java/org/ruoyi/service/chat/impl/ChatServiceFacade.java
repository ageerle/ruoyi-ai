package org.ruoyi.service.chat.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.service.chat.handler.ChatContextBuilder;
import org.ruoyi.service.chat.handler.ChatHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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
public class ChatServiceFacade {

    private final ChatContextBuilder contextBuilder;
    private final List<ChatHandler> handlers;

    /**
     * 统一聊天入口 - SSE流式响应
     *
     * @param chatRequest 聊天请求
     * @param request     HTTP请求对象
     * @return SseEmitter
     */
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        // 1. 构建对话上下文
        ChatContext context = contextBuilder.build(chatRequest);

        // 2. 路由到对应的处理器
        return handlers.stream()
            .filter(handler -> handler.supports(context))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("无可用对话处理器"))
            .handle(context);
    }
}
