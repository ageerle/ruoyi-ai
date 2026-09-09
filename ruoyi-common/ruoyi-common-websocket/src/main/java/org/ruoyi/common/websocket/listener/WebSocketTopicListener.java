package org.ruoyi.common.websocket.listener;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.websocket.holder.WebSocketSessionHolder;
import org.ruoyi.common.websocket.utils.WebSocketUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * WebSocket 主题订阅监听器
 *
 * @author zendwang
 */
@Slf4j
public class WebSocketTopicListener implements ApplicationRunner, Ordered {

    /**
     * 在Spring Boot应用程序启动时初始化WebSocket主题订阅监听器
     *
     * @param args 应用程序参数
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 订阅WebSocket消息
        WebSocketUtils.subscribeMessage((message) -> {
            boolean broadcast = CollUtil.isEmpty(message.getSessionKeys());
            int recipientCount = broadcast ? WebSocketSessionHolder.getSessionsAll().size()
                : message.getSessionKeys().size();
            log.info("websocket_topic_received broadcast={} recipientCount={} payloadLength={}",
                broadcast, recipientCount, message.getMessage() == null ? 0 : message.getMessage().length());
            // 如果key不为空就按照key发消息 如果为空就群发
            if (CollUtil.isNotEmpty(message.getSessionKeys())) {
                message.getSessionKeys().forEach(key -> {
                    if (WebSocketSessionHolder.existSession(key)) {
                        WebSocketUtils.sendMessage(key, message.getMessage());
                    }
                });
            } else {
                WebSocketSessionHolder.getSessionsAll().forEach(key -> {
                    WebSocketUtils.sendMessage(key, message.getMessage());
                });
            }
        });
        log.info("websocket_topic_listener_initialized status=SUCCESS");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
