package org.ruoyi.common.sse.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.dto.SseMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * SSE 主题订阅监听器
 *
 * @author Lion Li
 */
@Slf4j
public class SseTopicListener implements ApplicationRunner, Ordered {

    @Autowired
    private SseEmitterManager sseEmitterManager;

    /**
     * 在Spring Boot应用程序启动时初始化SSE主题订阅监听器
     *
     * @param args 应用程序参数
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        sseEmitterManager.subscribeMessage((message) -> {
            log.info("SSE主题订阅收到消息session:{} session keys={} message={}",
                message.getSessionId(), message.getUserIds(), message.getMessage());
            // 优先按会话路由（对话流式响应）
            if (StrUtil.isNotBlank(message.getSessionId())) {
                if (message.getEventDto() != null) {
                    sseEmitterManager.sendEvent(message.getSessionId(), message.getEventDto());
                } else if (message.getMessage() != null) {
                    // 兼容按会话发纯文本的场景
                    sseEmitterManager.sendEvent(message.getSessionId(),
                        org.ruoyi.common.sse.dto.SseEventDto.content(message.getMessage()));
                }
                return;
            }
            // 否则按用户/群发路由（全局通知）
            if (CollUtil.isNotEmpty(message.getUserIds())) {
                message.getUserIds().forEach(key -> {
                    sseEmitterManager.sendMessage(key, message.getMessage());
                });
            } else {
                sseEmitterManager.sendMessage(message.getMessage());
            }
        });
        log.info("初始化SSE主题订阅监听器成功");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
