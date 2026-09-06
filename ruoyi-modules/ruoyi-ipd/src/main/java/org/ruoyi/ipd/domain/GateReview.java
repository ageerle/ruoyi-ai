package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD Gate 评审双签记录（每方一条；v3 TS-06）
 * 市场PM、研发PM、组长、超管四方评审，双签通过方可放行
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gate_reviews", autoResultMap = true)
public class GateReview extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Gate 实例ID
     */
    private Long gateId;

    /**
     * 签署方 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN
     */
    private String reviewerType;

    /**
     * 签署人ID
     */
    private Long reviewerId;

    /**
     * 决定 APPROVE|REJECT|ABSTAIN
     */
    private String decision;

    /**
     * 意见（双方都提交前互不可见）
     */
    private String opinion;

    /**
     * 签署时间
     */
    private Date signedAt;

    /**
     * 签署期限（BR-GATE-04，3 天超时弃权）
     */
    private Date dueAt;

    /**
     * 评审轮次
     */
    private Integer round;

    /**
     * 软删除标志（0正常 1已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }

    /** 签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21） */
    private Date signDueAt;

    /** 签署期限已延长次数（AC-GATE-21 上限 3） */
    private Integer signExtensionCount;
}
