package org.ruoyi.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 会话管理对象 chat_session
 *
 * @author ageerle
 * @date 2025-12-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String sessionTitle;

    /**
     * 会话内容
     */
    private String sessionContent;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 备注
     */
    private String remark;

}
