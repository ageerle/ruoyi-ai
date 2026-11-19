package org.ruoyi.chat.controller.chat;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.chat.service.chat.IChatMessageService;
import org.ruoyi.chat.service.chat.impl.ChatServiceFacade;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final IChatMessageService messages;

    @SaCheckLogin
    @GetMapping("/messages/{messageId}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long messageId) {
        return messages.findOwnedImage(LoginHelper.getUserId(), messageId)
            .map(image -> ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .body(image.data()))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 聊天接口
     */
    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest) {
        return chatService.sseChat(chatRequest);
    }

}
