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

import java.math.BigDecimal;
import java.util.Date;

/**
 * S/B 级差异化系数定值申请（AC-INC-15c / Q4）。
 * <p>双PM 联合提议 → 产品组长确认 → 写入 {@code projects.level_coefficient}。
 */
@TableName(value = "coefficient_change_requests", autoResultMap = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CoefficientChangeRequest extends BaseEntity {

    public static final String ST_PENDING_LEADER = "PENDING_LEADER";
    public static final String ST_CONFIRMED = "CONFIRMED";
    public static final String ST_REJECTED = "REJECTED";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("proposed_coefficient")
    private BigDecimal proposedCoefficient;

    @TableField("reason")
    private String reason;

    @TableField("market_pm_id")
    private Long marketPmId;

    @TableField("rd_pm_id")
    private Long rdPmId;

    @TableField("proposer_id")
    private Long proposerId;

    /** PENDING_LEADER|CONFIRMED|REJECTED */
    @TableField("status")
    private String status;

    @TableField("leader_id")
    private Long leaderId;

    /** APPROVE|REJECT */
    @TableField("leader_decision")
    private String leaderDecision;

    @TableField("leader_decided_at")
    private Date leaderDecidedAt;

    @TableField("leader_opinion")
    private String leaderOpinion;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @TableField("remark")
    private String remark;
}
