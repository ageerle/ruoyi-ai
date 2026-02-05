package org.ruoyi.domain.bo.chat;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.entity.chat.ChatConfig;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 配置信息业务对象 chat_config
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatConfig.class, reverseConvertGenerate = false)
public class ChatConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 配置类型
     */
    @NotBlank(message = "配置类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String category;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String configName;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空", groups = { AddGroup.class, EditGroup.class })
    private String configValue;

    /**
     * 说明
     */
    private String configDict;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新IP
     */
    private String updateIp;


}
