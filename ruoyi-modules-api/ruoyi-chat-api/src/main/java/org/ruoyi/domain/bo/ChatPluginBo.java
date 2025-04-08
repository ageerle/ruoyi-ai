package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.ChatPlugin;


/**
 * 插件管理业务对象 chat_plugin
 *
 * @author ageerle
 * @date 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatPlugin.class, reverseConvertGenerate = false)
public class ChatPluginBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 插件名称
     */
    @NotBlank(message = "插件名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 插件编码
     */
    @NotBlank(message = "插件编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String code;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
