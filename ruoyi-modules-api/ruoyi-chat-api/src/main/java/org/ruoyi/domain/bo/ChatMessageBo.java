package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.ChatMessage;

import java.math.BigDecimal;

/**
 * 聊天消息业务对象 chat_message
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatMessage.class, reverseConvertGenerate = false)
public class ChatMessageBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 会话id
     */
    private Long sessionId;

    /**
     * 对话角色
     */
    @NotBlank(message = "对话角色不能为空", groups = { AddGroup.class, EditGroup.class })
    private String role;

    /**
     * 扣除金额
     */
    @NotNull(message = "扣除金额不能为空", groups = { AddGroup.class, EditGroup.class })
    private Double deductCost;

    /**
     * 累计 Tokens
     */
    @NotNull(message = "累计 Tokens不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer totalTokens;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelName;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
