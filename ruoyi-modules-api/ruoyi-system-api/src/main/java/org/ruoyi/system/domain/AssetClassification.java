package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 高等学校固定资产分类与代码对象 asset_classification
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_classification")
public class AssetClassification extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 分类代码
     */
    private String classificationCode;

    /**
     * 分类名称
     */
    private String classificationName;

    /**
     * 国标名称
     */
    private String gbName;
}
