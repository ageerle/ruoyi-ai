package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * P3 阶段月度账务切换验收（P3-7.1；BR-INC-12；AC-INC-50/51）
 *
 * <p>每月一条对账记录；含 run 报告 JSON + 锁定状态 + 解锁理由。
 * <p>锁定月份所有账务写操作（含 Allowance / Bonus / NegativeFeedback / Contribution）须先检查此表。
 *
 * <p>删除走 DeletionRequestService + DeleteAuditService（软删除，del_flag='0'/'1'）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "switching_acceptance", autoResultMap = true)
public class SwitchingAcceptance extends BaseEntity implements SoftDeletable {

    /** 月份 YYYY-MM */
    public static final int MAX_MONTH_LEN = 7;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 月份 YYYY-MM（7 位） */
    private String month;

    /** 运行时间 */
    private Date ranAt;

    /** 运行人 */
    private Long ranBy;

    /** 对账报告 JSON（不解析，直接存原文） */
    private String reportJson;

    /** 总差异率（<0.01 通过；BigDecimal 5,4） */
    private BigDecimal diffRate;

    /** 是否通过 */
    private Boolean passed;

    /** 是否锁定 */
    private Boolean isLocked;

    /** 锁定时间 */
    private Date lockedAt;

    /** 锁定人 */
    private Long lockedBy;

    /** 解锁理由 */
    private String unlockReason;

    /** 解锁时间 */
    private Date unlockedAt;

    /** 解锁人 */
    private Long unlockedBy;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}