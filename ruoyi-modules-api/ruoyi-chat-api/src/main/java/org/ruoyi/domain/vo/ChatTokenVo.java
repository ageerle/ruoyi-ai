package org.ruoyi.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

import java.io.Serializable;

/**
 * 用户token chat_token
 *
 * @author Lion Li
 * @date 2023-11-26
 */
@Data
public class ChatTokenVo implements Serializable {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID", groups = { AddGroup.class, EditGroup.class })
    private Long UserId;

    /**
     * 待结算token
     */
    private Integer token;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelName;

}
