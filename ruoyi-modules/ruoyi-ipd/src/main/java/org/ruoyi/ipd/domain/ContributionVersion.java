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
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 贡献度确认归档快照（BR-INC-09「归档版本可追溯；奖金引用同一版本」）。
 *
 * <p>每次产品组长 APPROVE（DRAFT/SUBMITTED → CONFIRMED）时对 contributions 当前行
 * 做一份不可变快照；REJECT 退回 DRAFT 后再次确认产生新 versionNo。版本列表即
 * 「该项目的历次确认版次」，奖金池结算应引用确认时刻的快照而非可变当前行。
 *
 * <p>2026-09-08 前端契约对照轮补交：此前仅有单行 contributions，无版本追溯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "contribution_versions", autoResultMap = true)
public class ContributionVersion extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 来源 contributions.id（非外键约束，逻辑关联） */
    private Long sourceId;

    private Long projectId;

    /** 确认版次，同项目内从 1 递增（APPROVE 时 max+1） */
    private Integer versionNo;

    /** 快照时刻状态，恒为 CONFIRMED（归档点即确认点） */
    private String status;

    // ===== 以下为 contributions 业务列的确认时刻快照 =====

    private BigDecimal marketShare;
    private BigDecimal rdShare;

    private BigDecimal marketSelfInitiation;
    private BigDecimal marketSelfInnovation;
    private BigDecimal marketSelfLaunch;
    private BigDecimal marketSelfMarketResult;
    private BigDecimal marketSelfLeadership;

    private BigDecimal rdSelfInitiation;
    private BigDecimal rdSelfInnovation;
    private BigDecimal rdSelfLaunch;
    private BigDecimal rdSelfMarketResult;
    private BigDecimal rdSelfLeadership;

    private BigDecimal tierCoefficient;
    private String marketComment;
    private String rdComment;

    private Long leaderId;
    private String leaderDecision;
    private Date leaderDecidedAt;
    private String leaderOpinion;
    private Date submittedAt;

    /** 归档操作人（确认的组长/超管） */
    private Long archivedBy;
    /** 归档时间 */
    private Date archivedAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
