package org.ruoyi.workflow.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.workflow.domain.TestLeave;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 请假视图对象 test_leave
 *
 * @author may
 * @date 2023-07-21
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestLeave.class)
public class TestLeaveVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 申请编号
     */
    @ExcelProperty(value = "申请编号")
    private String applyCode;

    /**
     * 请假类型
     */
    @ExcelProperty(value = "请假类型")
    private String leaveType;

    /**
     * 开始时间
     */
    @ExcelProperty(value = "开始时间")
    private Date startDate;

    /**
     * 结束时间
     */
    @ExcelProperty(value = "结束时间")
    private Date endDate;

    /**
     * 请假天数
     */
    @ExcelProperty(value = "请假天数")
    private Integer leaveDays;

    /**
     * 备注
     */
    @ExcelProperty(value = "请假原因")
    private String remark;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

}
