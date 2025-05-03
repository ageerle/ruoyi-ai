package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.ChatSession;

/**
 * 会话管理业务对象 chat_session
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatSession.class, reverseConvertGenerate = false)
public class ChatSessionBo extends BaseEntity {

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
     * 会话标题
     */
    @NotBlank(message = "会话标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sessionTitle;

    /**
     * 会话内容
     */
    @NotBlank(message = "会话内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sessionContent;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
