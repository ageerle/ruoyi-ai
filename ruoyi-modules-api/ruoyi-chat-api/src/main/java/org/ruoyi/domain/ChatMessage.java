package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.math.BigDecimal;

import java.io.Serial;

/**
 * 聊天消息对象 chat_message
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

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
     * 消息内容
     */
    private String content;

    /**
     * 对话角色
     */
    private String role;

    /**
     * 扣除金额
     */
    private BigDecimal deductCost;

    /**
     * 累计 Tokens
     */
    private Long totalTokens;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 备注
     */
    private String remark;


}
