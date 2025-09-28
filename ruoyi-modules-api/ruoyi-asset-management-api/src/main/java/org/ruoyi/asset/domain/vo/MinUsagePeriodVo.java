package org.ruoyi.asset.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.asset.domain.MinUsagePeriod;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 最低使用年限表视图对象 min_usage_period
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MinUsagePeriod.class)
public class MinUsagePeriodVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

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

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
