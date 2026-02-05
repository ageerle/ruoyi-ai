package org.ruoyi.controller.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.dto.request.ChatRequest;
import org.ruoyi.service.chat.impl.ChatServiceFacade;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * 聊天管理
 *
 * @author ageerle@163.com
 * @date 2023-03-01
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatServiceFacade chatService;

    /**
     * 聊天接口
     */
    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest, HttpServletRequest request) {
        return chatService.sseChat(chatRequest,request);
    }

}
