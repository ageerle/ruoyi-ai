package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.ChatUsageToken;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 用户token使用详情业务对象 chat_usage_token
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatUsageToken.class, reverseConvertGenerate = false)
public class ChatUsageTokenBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户
     */
    @NotNull(message = "用户不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 待结算token
     */
    @NotNull(message = "待结算token不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer token;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelName;

    /**
     * 累计使用token
     */
    @NotBlank(message = "累计使用token不能为空", groups = { AddGroup.class, EditGroup.class })
    private String totalToken;


}
