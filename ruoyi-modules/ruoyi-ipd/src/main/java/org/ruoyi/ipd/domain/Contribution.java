package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 贡献度评定（BR-INC-09 / AC-INC-25~28）。
 *
 * <p>五维度加权得分 → tierCoefficient 修正因子 → 由 {@link>ContributionService} 维护，
 * 供 {@code BonusPoolService.calculateBonusPoolByZkFormulaWithModifiers} 读取以实现
 * ZK-IPD §三.2.5 完整 4 因子叠加。
 *
 * <p>权重：
 * <ul>
 *   <li>立项主导 25% + 差异化创新 25% + 上市节奏 20% + 市场结果 20% + 协同领导力 10% = 100%</li>
 *   <li>市场 PM 比例 ∈ [0.40, 0.65]；研发 PM 比例 ∈ [0.35, 0.60]；两者之和 = 1.0</li>
 * </ul>
 *
 * <p>删除走 DeletionRequestService + DeleteAuditService（软删除）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "contributions", autoResultMap = true)
public class Contribution extends BaseEntity implements SoftDeletable {

    /** 草稿（双 PM 任一方已保存自评） */
    public static final String ST_DRAFT = "DRAFT";
    /** 已提交（双 PM 自评均完成，等待产品组长确认） */
    public static final String ST_SUBMITTED = "SUBMITTED";
    /** 已确认（产品组长 APPROVE 后；tier_coefficient 落定） */
    public static final String ST_CONFIRMED = "CONFIRMED";

    /** PM 角色：MARKET_PM */
    public static final String ROLE_MARKET = "MARKET_PM";
    /** PM 角色：RD_PM */
    public static final String ROLE_RD = "RD_PM";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /**
     * 评定人 personId（DDL legacy v1 字段；新版本用 leaderId，本字段保留以兼容既有 INSERT/查询）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射（DDL P3-6.x 既有）。
     */
    @TableField("person_id")
    private Long personId;

    /**
     * 市场贡献度（0.4000-0.6500；DDL legacy v1 字段；新版本用 marketShare）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射。
     */
    @TableField("market_contribution_rate")
    private BigDecimal marketContributionRate;

    /** 评定状态 DRAFT | SUBMITTED | CONFIRMED */
    private String status;

    /** 市场 PM 比例（40-65%，即 0.40-0.65） */
    private BigDecimal marketShare;

    /** 研发 PM 比例（35-60%，联动 = 1.00 - marketShare） */
    private BigDecimal rdShare;

    // ===== 市场 PM 自评五维度（0~100） =====

    private BigDecimal marketSelfInitiation;
    private BigDecimal marketSelfInnovation;
    private BigDecimal marketSelfLaunch;
    private BigDecimal marketSelfMarketResult;
    private BigDecimal marketSelfLeadership;

    // ===== 研发 PM 自评五维度（0~100） =====

    private BigDecimal rdSelfInitiation;
    private BigDecimal rdSelfInnovation;
    private BigDecimal rdSelfLaunch;
    private BigDecimal rdSelfMarketResult;
    private BigDecimal rdSelfLeadership;

    /** 五维度修正因子 tierCoefficient = 五维度加权得分 / 100 */
    private BigDecimal tierCoefficient;

    /** 市场 PM 自评备注 */
    private String marketComment;
    /** 研发 PM 自评备注 */
    private String rdComment;

    // ===== 产品组长确认 =====

    /** 产品组长 ID（确认人） */
    private Long leaderId;
    /** 组长决策 APPROVE / REJECT */
    private String leaderDecision;
    /** 组长决策时间 */
    private Date leaderDecidedAt;
    /** 组长意见 */
    private String leaderOpinion;

    /** 双 PM 自评均完成时间（SUBMITTED 状态落定） */
    private Date submittedAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}