package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 项目绩效评定（BR-KPI-08）
 * 自评 20% + 市场组长 40% + 研发组长 40%（AC-KPI-16）
 * 两 PM 各自独立评分，删除走软删除
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_scores", autoResultMap = true)
public class ProjectScore extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 被评 PM（市场 / 研发） */
    private Long personId;

    /**
     * 五维度·维度1（25%，DDL legacy v1 字段；新版本使用 self_score/market_leader_score/rd_leader_score）。
     * [QA-04-B-FIX] 2026-09-06：补齐 DDL NOT NULL 无默认值列映射。
     */
    @TableField("dimension_1")
    private BigDecimal dimension1;

    /**
     * 五维度·维度2（25%）。
     */
    @TableField("dimension_2")
    private BigDecimal dimension2;

    /**
     * 五维度·维度3（20%）。
     */
    @TableField("dimension_3")
    private BigDecimal dimension3;

    /**
     * 五维度·维度4（20%）。
     */
    @TableField("dimension_4")
    private BigDecimal dimension4;

    /**
     * 五维度·维度5（10%）。
     */
    @TableField("dimension_5")
    private BigDecimal dimension5;

    /**
     * 评定人（组长）DDL legacy v1 字段；新版本使用 author_id（本字段保留以兼容既有 INSERT/查询）。
     */
    @TableField("evaluator_id")
    private Long evaluatorId;

    /** PM 角色 MARKET_PM / RD_PM */
    private String pmRole;

    /** 自评 0~100 */
    private BigDecimal selfScore;

    /** 市场组长评 0~100（双 PM 各自打分；与被评人无关） */
    private BigDecimal marketLeaderScore;

    /** 研发组长评 0~100（双 PM 各自打分；与被评人无关） */
    private BigDecimal rdLeaderScore;

    /** 加权汇总 = self×0.2 + ml×0.4 + rl×0.4 */
    private BigDecimal weightedScore;

    /** 评分状态 DRAFT/FINALIZED */
    private String status;

    /** 评分日期 */
    private Date scoredAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    public void setDelFlag(String flag) { this.delFlag = flag; }
}
