package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import java.util.Date;

/**
 * IPD 删除申请（差异化两级审核：组长 2 工作日 → 超管 2 工作日）
 * 依据：开发说明书 BR-DEL + deletion.leaderDeadlineDays/adminDeadlineDays；禁止直接物理删除
 */
@TableName(value = "deletion_requests", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DeletionRequest extends BaseEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("entity_type")
    private String entityType;
    @TableField("entity_id")
    private Long entityId;
    /** 删除前快照（JSON） */
    @TableField("entity_snapshot")
    private String entitySnapshot;
    @TableField("reason")
    private String reason;
    @TableField("requester_id")
    private Long requesterId;
    /** DRAFT|LEADER_REVIEW|ADMIN_REVIEW|DELETED|REJECTED */
    @TableField("status")
    private String status;
    @TableField("leader_id")
    private Long leaderId;
    /** APPROVE|REJECT */
    @TableField("leader_decision")
    private String leaderDecision;
    @TableField("leader_decided_at")
    private Date leaderDecidedAt;
    @TableField("leader_due_at")
    private Date leaderDueAt;
    @TableField("admin_id")
    private Long adminId;
    @TableField("admin_decision")
    private String adminDecision;
    @TableField("admin_decided_at")
    private Date adminDecidedAt;
    @TableField("admin_due_at")
    private Date adminDueAt;
    @TableField("executed_at")
    private Date executedAt;
    @TableField("tenant_id")
    private String tenantId;
    @TableLogic @TableField("del_flag")
    private String delFlag;
    @TableField("remark")
    private String remark;
}