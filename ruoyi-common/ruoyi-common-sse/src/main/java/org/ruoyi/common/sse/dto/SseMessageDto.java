package org.ruoyi.common.sse.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 消息的dto
 *
 * @author zendwang
 */
@Data
public class SseMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 需要推送到的session key 列表
     */
    private List<Long> userIds;

    /**
     * 需要发送的消息
     */
    private String message;

    /**
     * 按会话定向推送的会话ID（非空时优先按会话路由，忽略 userIds）
     */
    private String sessionId;

    /**
     * 结构化事件（按会话定向推送时使用，message 为兼容旧逻辑保留）
     */
    private SseEventDto eventDto;
}
