package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
    import java.util.Date;
    import java.io.Serializable;

import org.ruoyi.core.domain.BaseEntity;

/**
 * dome管理对象 sys_demo
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_demo")
public class SysDemo extends BaseEntity {


    /**
     * ID
     */
        @TableId(value = "id")
    private Integer id;

    /**
     * 系统代码
     */
    private String sysCode;

    /**
     * 系统名称
     */
    private String sysName;

    /**
     * 系统状态
     */
    private Integer sysStatus;

    /**
     * 创建部门
     */
    private Integer createDept;

    /**
     * 创建者
     */
    private Integer createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private Integer updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;


}
