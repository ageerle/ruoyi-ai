package org.ruoyi.workflow.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.workflow.domain.TestLeave;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 请假业务对象 test_leave
 *
 * @author may
 * @date 2023-07-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = TestLeave.class, reverseConvertGenerate = false)
public class TestLeaveBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 流程code
     */
    private String flowCode;

    /**
     * 申请编号
     */
    private String applyCode;

    /**
     * 请假类型
     */
    @NotBlank(message = "请假类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String leaveType;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空", groups = {AddGroup.class, EditGroup.class})
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空", groups = {AddGroup.class, EditGroup.class})
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /**
     * 请假天数
     */
    private Integer leaveDays;

    /**
     * 开始时间
     */
    private Integer startLeaveDays;

    /**
     * 结束时间
     */
    private Integer endLeaveDays;

    /**
     * 请假原因
     */
    private String remark;

    /**
     * 状态
     */
    private String status;


}
