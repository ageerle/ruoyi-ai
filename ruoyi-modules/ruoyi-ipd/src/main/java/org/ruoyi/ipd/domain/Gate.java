package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD Gate 实例（五大联合 Gate；v3 TS-06）
 * G1..G5 对应六阶段出口评审，双签机制由 GateReviewService 实现
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gates", autoResultMap = true)
public class Gate extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * Gate 编码 G1..G5
     */
    private String gateCode;

    /**
     * 状态 PENDING|APPROVED|REJECTED|ABSTAINED_TIMEOUT
     */
    private String status;

    /**
     * 当前评审轮次（BR-GATE-06：第3轮组长列席/第5轮超管介入）
     */
    private Integer currentRound;

    /**
     * Gate 系数（G1 双签决定，bonus.coefficientDecider）
     */
    private BigDecimal gateCoefficient;

    /**
     * 计划评审时间
     */
    private Date plannedAt;

    /**
     * 开始评审时间
     */
    private Date startedAt;

    /**
     * 完成评审时间
     */
    private Date concludedAt;

    /**
     * 软删除标志（0正常 1已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21） */
    private Date signDueAt;

    /** 签署期限已延长次数（AC-GATE-21 上限 1 次） */
    private Integer signExtensionCount;

    /** 要素快照（GateElement 的 JSON 备份，便于后续审计回溯） */
    private String elementSnapshot;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
