package org.ruoyi.asset.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;

/**
 * 高等学校固定资产分类与代码业务对象 asset_classification
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetClassificationBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 分类代码
     */
    @NotBlank(message = "分类代码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String classificationCode;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String classificationName;

    /**
     * 国标名称
     */
    @NotBlank(message = "国标名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String gbName;
}
