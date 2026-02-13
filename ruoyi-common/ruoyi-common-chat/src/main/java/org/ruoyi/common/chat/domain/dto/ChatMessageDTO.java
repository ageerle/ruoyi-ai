package org.ruoyi.common.chat.domain.dto;

import lombok.Data;

/**
 * 聊天消息DTO - 用于上下文传递
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Data
public class ChatMessageDTO {

    /**
     * 消息角色: system/user/assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    public static ChatMessageDTO system(String content) {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.role = "system";
        msg.content = content;
        return msg;
    }

    public static ChatMessageDTO user(String content) {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.role = "user";
        msg.content = content;
        return msg;
    }

    public static ChatMessageDTO assistant(String content) {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.role = "assistant";
        msg.content = content;
        return msg;
    }
}

