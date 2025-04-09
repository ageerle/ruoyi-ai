package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 【请填写功能名称】视图对象 sys_user_model
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysUserModel.class)
public class SysUserModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 模型id
     */
    @ExcelProperty(value = "模型id")
    private Long mid;

    /**
     * 用户组id
     */
    @ExcelProperty(value = "用户组id")
    private Long gid;


}
