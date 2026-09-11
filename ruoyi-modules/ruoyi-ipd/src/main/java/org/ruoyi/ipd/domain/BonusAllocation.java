package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 奖金分配台账（AC-INC-35；P3-4.4）。
 *
 * <p>DDL：batch_missing_tables.sql L4-24。
 * <p>字段映射：
 * <ul>
 *   <li>id (bigint, PK)</li>
 *   <li>bonusPoolId (bonus_pool_id bigint) — 关联 bonus_pools.id</li>
 *   <li>personId (person_id bigint) — 被分配人</li>
 *   <li>roleInProject (role_in_project varchar(16)) — MARKET_PM|RD_PM</li>
 *   <li>contributionRate (contribution_rate decimal(5,4)) — 贡献度（0.4000-0.6500）</li>
 *   <li>performanceCoefficient (performance_coefficient decimal(5,2)) — 绩效系数</li>
 *   <li>allocatedAmount (allocated_amount decimal(18,2)) — 分配金额</li>
 *   <li>status (varchar(16)) — DRAFT|CONFIRMED|ARCHIVED</li>
 * </ul>
 *
 * <p>删除走软删除（del_flag='0'/'1'）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "bonus_allocations", autoResultMap = true)
public class BonusAllocation extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 bonus_pools.id */
    private Long bonusPoolId;

    /** 被分配人 */
    private Long personId;

    /** MARKET_PM|RD_PM */
    private String roleInProject;

    /** 贡献度（0.4000-0.6500） */
    private BigDecimal contributionRate;

    /** 绩效系数 */
    private BigDecimal performanceCoefficient;

    /** 分配金额 */
    private BigDecimal allocatedAmount;

    /** DRAFT|CONFIRMED|ARCHIVED */
    private String status;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
