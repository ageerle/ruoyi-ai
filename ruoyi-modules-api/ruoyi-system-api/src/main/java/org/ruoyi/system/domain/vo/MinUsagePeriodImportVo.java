package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 最低使用年限表导入VO
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@NoArgsConstructor
public class MinUsagePeriodImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 固定资产类别
     */
    @ExcelProperty(value = "固定资产类别")
    private String category;

    /**
     * 内容
     */
    @ExcelProperty(value = "内容")
    private String content;

    /**
     * 最低使用年限（年）
     */
    @ExcelProperty(value = "最低使用年限（年）")
    private Integer minYears;

    /**
     * 国标代码
     */
    @ExcelProperty(value = "国标代码")
    private String gbCode;

}
