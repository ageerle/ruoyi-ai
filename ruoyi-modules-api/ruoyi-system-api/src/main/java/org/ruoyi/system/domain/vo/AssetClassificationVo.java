package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 高等学校固定资产分类与代码视图对象 asset_classification
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@ExcelIgnoreUnannotated
public class AssetClassificationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

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

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private String createBy;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private String updateBy;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
