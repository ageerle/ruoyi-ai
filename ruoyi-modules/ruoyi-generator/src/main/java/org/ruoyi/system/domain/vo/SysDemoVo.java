package org.ruoyi.system.domain.vo;

    import java.util.Date;
    import java.io.Serializable;
import org.ruoyi.system.domain.SysDemo;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;


/**
 * dome管理视图对象 sys_demo
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDemo.class)
public class SysDemoVo implements Serializable {

            /**
             * ID
             */
            @ExcelProperty(value = "ID")
        private Integer id;

            /**
             * 系统代码
             */
            @ExcelProperty(value = "系统代码")
        private String sysCode;

            /**
             * 系统名称
             */
            @ExcelProperty(value = "系统名称")
        private String sysName;

            /**
             * 系统状态
             */
            @ExcelProperty(value = "系统状态")
        private Integer sysStatus;

            /**
             * 创建部门
             */
            @ExcelProperty(value = "创建部门")
        private Integer createDept;

            /**
             * 创建者
             */
            @ExcelProperty(value = "创建者")
        private Integer createBy;

            /**
             * 创建时间
             */
            @ExcelProperty(value = "创建时间")
        private Date createTime;

            /**
             * 更新者
             */
            @ExcelProperty(value = "更新者")
        private Integer updateBy;

            /**
             * 更新时间
             */
            @ExcelProperty(value = "更新时间")
        private Date updateTime;

            /**
             * 备注
             */
            @ExcelProperty(value = "备注")
        private String remark;


}
