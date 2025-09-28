package org.ruoyi.asset.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.asset.domain.MinUsagePeriod;

/**
 * 最低使用年限表业务对象 min_usage_period
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MinUsagePeriod.class, reverseConvertGenerate = false)
public class MinUsagePeriodBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 固定资产类别
     */
    @NotBlank(message = "固定资产类别不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "固定资产类别不能超过{max}个字符")
    private String category;

    /**
     * 内容
     */
    @Size(min = 0, max = 500, message = "内容不能超过{max}个字符")
    private String content;

    /**
     * 最低使用年限（年）
     */
    private Integer minYears;

    /**
     * 国标代码
     */
    @NotBlank(message = "国标代码不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 20, message = "国标代码不能超过{max}个字符")
    private String gbCode;

}
