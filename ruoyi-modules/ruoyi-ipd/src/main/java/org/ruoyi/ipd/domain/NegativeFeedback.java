package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 负反馈执行（BR-INC-10；AC-INC-36b/37/38/39/40；P3-8.2）
 *
 * <p>四种触发情形 × 主责方 / 连带方映射（ZK-IPD Prompt §三.2.5）：
 * <ul>
 *   <li>REWORK_EXCEEDED 需求返工率超标：MARKET_PM（主责，停发）+ RD_PM（连带，减半）</li>
 *   <li>QUALITY_ACCIDENT 质量事故：RD_PM（主责，停发）+ MARKET_PM（连带，减半）</li>
 *   <li>SPEC_PILE_COPY 参数堆砌/对标抄袭：RD_PM（主责，停发）+ MARKET_PM（连带，减半）</li>
 *   <li>MISSED_MARKET_WINDOW 错过市场窗口：BOTH 双PM共同担责（无主责/连带区分）</li>
 * </ul>
 *
 * <p>状态机：DRAFT → PENDING_DECISION → EXECUTED → LIFTED；任意阶段可 REJECTED。
 * <p>AC-INC-40：同项目同 triggerType 唯一（uk_nf_project_trigger_active），重复事件不重复扣减。
 *
 * <p>删除走 DeletionRequestService + DeleteAuditService（软删除，del_flag='0'/'1'）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "negative_feedbacks", autoResultMap = true)
public class NegativeFeedback extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 来源渠道（DDL legacy v1 字段；新版本用 triggerType/触发情形；本字段保留以兼容既有 INSERT/查询）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射。
     */
    private String source;

    /**
     * 内容（DDL legacy v1 字段；新版本用 triggerEvidence/触发证据；本字段保留以兼容既有 INSERT/查询）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射。
     */
    private String content;

    /**
     * 严重程度 LOW|MEDIUM|HIGH|CRITICAL（DDL legacy v1 字段；新版本用 mainExecution；本字段保留以兼容既有 INSERT/查询）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射。
     */
    private String severity;

    /**
     * 触发情形（REWORK_EXCEEDED|QUALITY_ACCIDENT|SPEC_PILE_COPY|MISSED_MARKET_WINDOW）
     */
    private String triggerType;

    /**
     * 主责方角色（MARKET_PM|RD_PM|BOTH）
     */
    private String mainRole;

    /**
     * 主责人 person.id（BOTH 时也填一个主记录人）
     */
    private Long mainPersonId;

    /**
     * 连带方角色（MARKET_PM|RD_PM；MISSED_MARKET_WINDOW 为 NULL）
     */
    private String relatedRole;

    /**
     * 连带人 person.id
     */
    private Long relatedPersonId;

    /**
     * 主责方执行（STOP_ALLOWANCE|HALVE_ALLOWANCE|BONUS_DOWNGRADE|BONUS_DISQUALIFY）
     */
    private String mainExecution;

    /**
     * 连带方执行（HALT_ALLOWANCE|...；可空）
     */
    private String relatedExecution;

    /**
     * 是否取消分配资格（AC-INC-39）
     */
    private Integer bonusDisqualify;

    /**
     * 贡献度系数降低值（默认 -0.50）
     */
    private BigDecimal tierDelta;

    /**
     * 生效月份 YYYY-MM
     */
    private String triggerMonth;

    /**
     * 解除月份 YYYY-MM（NULL=未解除）
     */
    private String recoveryMonth;

    /**
     * 触发证据描述（≤1000）
     */
    private String triggerEvidence;

    /**
     * 状态（DRAFT/PENDING_DECISION/EXECUTED/LIFTED/REJECTED）
     */
    private String status;

    /**
     * 录入人 person.id（AC-INC-40）
     */
    private Long triggeredBy;

    /**
     * 认定人（GROUP_LEADER / SUPER_ADMIN）
     */
    private Long decidedBy;

    /**
     * 认定时间
     */
    private Date decidedAt;

    /**
     * 解除人
     */
    private Long liftedBy;

    /**
     * 解除时间
     */
    private Date liftedAt;

    /**
     * 认定/解除备注（≤500）
     */
    private String decisionComment;

    /**
     * 软删除标志（0=正常 1=已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}