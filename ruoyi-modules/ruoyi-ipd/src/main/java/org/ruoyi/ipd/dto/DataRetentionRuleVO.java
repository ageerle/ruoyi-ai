package org.ruoyi.ipd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据保留规则 VO（P2-5.1 合规卡 AC-COMP-01）。
 * <p>资源类型 → 保留天数/删除策略/法律依据 的一行配置。
 *
 * @param resourceType    资源类型（projects / requirements / persons / audit_logs / …）
 * @param retentionDays   保留天数
 * @param deletionPolicy  删除策略（HARD_DELETE / SOFT_DELETE / ARCHIVE）
 * @param legalBasis      法律依据（DSL-个保法 / GDPR-Art17 / 内部留存）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRetentionRuleVO {
    private String resourceType;
    private int retentionDays;
    private String deletionPolicy;
    private String legalBasis;
}
