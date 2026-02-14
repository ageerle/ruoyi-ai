package org.ruoyi.common.chat.domain.entity.chat;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天对话上下文对象
 *
 * @author zengxb
 * @date 2026-02-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ChatContext {

    /**
     * 模型管理视图对象
     */
    @NotNull(message = "模型管理视图对象不能为空")
    private ChatModelVo chatModelVo;

    /**
     * 对话请求对象
     */
    @NotNull(message = "对话请求对象不能为空")
    private ChatRequest chatRequest;

    /**
     * SSe连接对象
     */
    @NotNull(message = "SSe连接对象不能为空")
    private SseEmitter emitter;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * Token
     */
    @NotNull(message = "Token不能为空")
    private String tokenValue;

    /**
     * 响应处理器
     */
    private StreamingChatResponseHandler handler;
}
