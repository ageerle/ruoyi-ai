package com.xmzs.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.xmzs.common.excel.annotation.ExcelDictFormat;
import com.xmzs.common.excel.convert.ExcelDictConvert;
import com.xmzs.system.domain.SysTenantPackage;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 租户套餐视图对象 sys_tenant_package
 *
 * @author Michelle.Chung
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysTenantPackage.class)
public class SysTenantPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户套餐id
     */
    @ExcelProperty(value = "租户套餐id")
    private Long packageId;

    /**
     * 套餐名称
     */
    @ExcelProperty(value = "套餐名称")
    private String packageName;

    /**
     * 关联菜单id
     */
    @ExcelProperty(value = "关联菜单id")
    private String menuIds;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 菜单树选择项是否关联显示
     */
    @ExcelProperty(value = "菜单树选择项是否关联显示")
    private Boolean menuCheckStrictly;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;


}
