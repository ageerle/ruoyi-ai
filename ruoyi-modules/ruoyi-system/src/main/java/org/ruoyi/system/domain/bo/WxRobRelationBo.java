package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.WxRobRelation;

/**
 * 【请填写功能名称】业务对象 wx_rob_relation
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WxRobRelation.class, reverseConvertGenerate = false)
public class WxRobRelationBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 外接唯一码
     */
    @NotBlank(message = "外接唯一码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String outKey;

    /**
     * 机器唯一码
     */
    @NotBlank(message = "机器唯一码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String uniqueKey;

    /**
     * 目标昵称
     */
    @NotBlank(message = "目标昵称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String nickName;

    /**
     * 群1好友0
     */
    @NotNull(message = "群1好友0不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer toGroup;

    /**
     * 启用1禁用0
     */
    @NotNull(message = "启用1禁用0不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer enable;

    /**
     * IP白名单
     */
    @NotBlank(message = "IP白名单不能为空", groups = { AddGroup.class, EditGroup.class })
    private String whiteList;


}
