package org.ruoyi.common.chat.entity.chat;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 聊天消息对象 chat_message
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 会话id
     */
    private Long sessionId;

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
    private Long deductCost;

    /**
     * 累计 Tokens
     */
    private Long totalTokens;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 计费类型（1-token计费，2-次数计费）
     */
    private String billingType;

    /**
     * 备注
     */
    private String remark;


}
