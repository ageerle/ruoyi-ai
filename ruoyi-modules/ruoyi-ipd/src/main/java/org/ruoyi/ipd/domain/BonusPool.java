package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 项目奖金池（BR-INC-04~09，涉钱必须 TDD）
 * 目标销售额×5%×系数→6档阶梯→分配，回款口径 salesSource=RECEIPT
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "bonus_pools", autoResultMap = true)
public class BonusPool extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目ID（1:1）
     */
    private Long projectId;

    /**
     * 目标销售额（回款口径 salesSource=RECEIPT）
     */
    private BigDecimal targetSales;

    /**
     * 奖金池比例（bonus.poolRate，默认0.05）
     */
    private BigDecimal poolRate;

    /**
     * 基础奖金池=目标销售额×5%
     */
    private BigDecimal basePool;

    /**
     * 项目系数（S/A/B，G1 双签决定）
     */
    private BigDecimal coefficient;

    /**
     * 回款达成率%
     */
    private BigDecimal achievementRate;

    /**
     * 达成率 6 档阶梯系数
     */
    private BigDecimal tierCoefficient;

    /**
     * 最终奖金池
     */
    private BigDecimal finalPool;

    /**
     * 个人分配结果（五维贡献+绩效系数，JSON）
     */
    private String distributions;

    /**
     * 状态 DRAFT|CONFIRMED|DISTRIBUTED
     */
    private String status;

    /**
     * 计算完成时间
     */
    private Date calculatedAt;

    /**
     * 发放时间
     */
    private Date distributedAt;

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
