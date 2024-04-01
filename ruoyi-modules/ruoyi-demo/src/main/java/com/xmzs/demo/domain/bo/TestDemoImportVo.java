package com.xmzs.demo.domain.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 测试单表业务对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
public class TestDemoImportVo {

    /**
     * 部门id
     */
    @NotNull(message = "部门id不能为空")
    @ExcelProperty(value = "部门id")
    private Long deptId;

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空")
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 排序号
     */
    @NotNull(message = "排序号不能为空")
    @ExcelProperty(value = "排序号")
    private Long orderNum;

    /**
     * key键
     */
    @NotBlank(message = "key键不能为空")
    @ExcelProperty(value = "key键")
    private String testKey;

    /**
     * 值
     */
    @NotBlank(message = "值不能为空")
    @ExcelProperty(value = "值")
    private String value;

}
