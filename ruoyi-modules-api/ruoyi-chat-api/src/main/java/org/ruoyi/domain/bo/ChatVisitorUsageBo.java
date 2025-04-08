package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.ChatVisitorUsage;


/**
 * 访客管理业务对象 chat_visitor_usage
 *
 * @author Lion Li
 * @date 2024-07-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatVisitorUsage.class, reverseConvertGenerate = false)
public class ChatVisitorUsageBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 浏览器指纹
     */
    @NotBlank(message = "浏览器指纹不能为空", groups = { AddGroup.class, EditGroup.class })
    private String fingerprint;

    /**
     * 使用次数
     */
    @NotBlank(message = "使用次数不能为空", groups = { AddGroup.class, EditGroup.class })
    private String usageCount;

    /**
     * ip地址
     */
    @NotBlank(message = "ip地址不能为空", groups = { AddGroup.class, EditGroup.class })
    private String ipAddress;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;

    /**
     * 更新IP
     */
    @NotBlank(message = "更新IP不能为空", groups = { AddGroup.class, EditGroup.class })
    private String updateIp;


}
