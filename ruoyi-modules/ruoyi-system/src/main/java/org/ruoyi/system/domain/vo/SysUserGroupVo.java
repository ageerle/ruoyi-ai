package org.ruoyi.system.domain.vo;

import org.ruoyi.system.domain.SysUserGroup;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 【请填写功能名称】视图对象 sys_user_group
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysUserGroup.class)
public class SysUserGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 用户组名称
     */
    @ExcelProperty(value = "用户组名称")
    private String groupName;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 更新IP
     */
    @ExcelProperty(value = "更新IP")
    private String updateIp;


}
