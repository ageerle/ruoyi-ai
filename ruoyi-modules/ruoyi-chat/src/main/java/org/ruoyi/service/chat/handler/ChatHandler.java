package org.ruoyi.service.chat.handler;

import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话处理器接口
 * <p>
 * 使用策略模式，每种对话场景独立实现
 * 通过 Order 注解控制优先级
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
public interface ChatHandler {

    /**
     * 是否支持处理该请求
     *
     * @param context 对话上下文
     * @return true-支持处理，false-不支持
     */
    boolean supports(ChatContext context);

    /**
     * 处理对话
     *
     * @param context 对话上下文
     * @return SSE发射器
     */
    SseEmitter handle(ChatContext context);

    /**
     * 优先级（越小越优先）
     * 默认 100，数字越小优先级越高
     *
     * @return 优先级数值
     */
    default int getOrder() {
        return 100;
    }
}