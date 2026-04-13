package org.ruoyi.common.chat.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Agent 对话请求对象（简化版）
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
@Data
public class AgentChatRequest {

    /**
     * 对话消息
     */
    @NotEmpty(message = "对话消息不能为空")
    private String content;

    /**
     * 会话id（可选，不传则不保存历史）
     */
    private Long sessionId;

}
