package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.ChatMessage;

/**
 * 聊天消息业务对象 chat_message
 *
 * @author Lion Li
 * @date 2023-11-26
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
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 对话角色
     */
    private String role;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 扣除费用
     */
    private Double deductCost;

    /**
     * 累计 Tokens
     */
    private Integer totalTokens;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 备注
     */
    private String remark;

}
