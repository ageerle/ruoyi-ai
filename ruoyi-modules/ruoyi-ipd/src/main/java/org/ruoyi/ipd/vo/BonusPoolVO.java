package org.ruoyi.ipd.vo;

import org.ruoyi.ipd.domain.BonusPool;

import java.math.BigDecimal;
import java.util.Date;

/**
 * View: BonusPool 的对外暴露视图，移除内部字段（delFlag / createBy / updateBy / createDept / params）。
 * 对应接口：/api/v1/bonus-pool
 */
public record BonusPoolVO(
    Long id,
    Long projectId,
    BigDecimal targetSales,
    BigDecimal poolRate,
    BigDecimal basePool,
    BigDecimal coefficient,
    BigDecimal achievementRate,
    BigDecimal tierCoefficient,
    BigDecimal finalPool,
    String distributions,
    String status,
    Date calculatedAt,
    Date distributedAt,
    Date createTime,
    Date updateTime
) {
    public static BonusPoolVO from(BonusPool p) {
        return new BonusPoolVO(
            p.getId(),
            p.getProjectId(),
            p.getTargetSales(),
            p.getPoolRate(),
            p.getBasePool(),
            p.getCoefficient(),
            p.getAchievementRate(),
            p.getTierCoefficient(),
            p.getFinalPool(),
            p.getDistributions(),
            p.getStatus(),
            p.getCalculatedAt(),
            p.getDistributedAt(),
            p.getCreateTime(),
            p.getUpdateTime()
        );
    }
}