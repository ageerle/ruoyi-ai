package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.ChatRobConfig;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 聊天机器人配置业务对象 chat_rob_config
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatRobConfig.class, reverseConvertGenerate = false)
public class ChatRobConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 所属用户
     */
    @NotNull(message = "所属用户不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 机器人名称
     */
    @NotBlank(message = "机器人名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String botName;

    /**
     * 机器唯一码
     */
    @NotBlank(message = "机器唯一码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String uniqueKey;

    /**
     * 默认好友回复开关
     */
    @NotBlank(message = "默认好友回复开关不能为空", groups = { AddGroup.class, EditGroup.class })
    private String defaultFriend;

    /**
     * 默认群回复开关
     */
    @NotBlank(message = "默认群回复开关不能为空", groups = { AddGroup.class, EditGroup.class })
    private String defaultGroup;

    /**
     * 机器人状态  0正常 1启用
     */
    @NotBlank(message = "机器人状态  0正常 1启用不能为空", groups = { AddGroup.class, EditGroup.class })
    private String enable;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
