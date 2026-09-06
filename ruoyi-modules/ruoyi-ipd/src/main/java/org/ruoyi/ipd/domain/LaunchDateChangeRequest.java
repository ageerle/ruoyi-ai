package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 上市日期变更双签申请（AC-INC-33 / P1-2.2）。
 * <p>一方提议 → 另一方确认 → 写入 {@code projects.launch_date}；禁止单方面修改。
 */
@TableName(value = "launch_date_change_requests", autoResultMap = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LaunchDateChangeRequest extends BaseEntity {

    public static final String ST_PENDING_SECOND = "PENDING_SECOND";
    public static final String ST_CONFIRMED = "CONFIRMED";
    public static final String ST_REJECTED = "REJECTED";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("proposed_launch_date")
    private Date proposedLaunchDate;

    @TableField("previous_launch_date")
    private Date previousLaunchDate;

    @TableField("reason")
    private String reason;

    @TableField("proposer_id")
    private Long proposerId;

    @TableField("proposer_role")
    private String proposerRole;

    @TableField("status")
    private String status;

    @TableField("confirmer_id")
    private Long confirmerId;

    @TableField("confirmer_role")
    private String confirmerRole;

    @TableField("confirmed_at")
    private Date confirmedAt;

    @TableField("decision")
    private String decision;

    @TableField("opinion")
    private String opinion;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @TableField("remark")
    private String remark;
}
