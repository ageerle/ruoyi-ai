package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 月度津贴台账（BR-INC-02/03）
 * 锁定评级/叠加/封顶/停发机制，多项目叠加最高2倍封顶 capMultiplier
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "allowance_ledgers", autoResultMap = true)
public class AllowanceLedger extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 人员ID
     */
    private Long personId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 台账月份 YYYY-MM
     */
    private String month;

    /**
     * 评级（绑定时锁定）
     */
    private String lockedLevel;

    /**
     * allowance.L1..L5 基准额
     */
    private BigDecimal baseAmount;

    /**
     * 终额（多项目叠加、2 倍封顶 capMultiplier）
     */
    private BigDecimal finalAmount;

    /**
     * 是否触发封顶（1是 0否）
     */
    private String capApplied;

    /**
     * 停发原因（<60 分/无产出 noOutput.days=60）
     */
    private String stopReason;

    /**
     * 停发开始日期
     */
    private Date stopStartDate;

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
}
