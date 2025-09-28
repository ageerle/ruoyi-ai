package org.ruoyi.asset.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 高等学校固定资产分类与代码导入视图对象
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
public class AssetClassificationImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类代码
     */
    @ExcelProperty(value = "分类代码")
    private String classificationCode;

    /**
     * 分类名称
     */
    @ExcelProperty(value = "分类名称")
    private String classificationName;

    /**
     * 国标名称
     */
    @ExcelProperty(value = "国标名称")
    private String gbName;
}
