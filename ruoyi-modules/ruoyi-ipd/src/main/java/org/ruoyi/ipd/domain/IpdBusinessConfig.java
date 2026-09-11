package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * IPD 业务参数（ROOT-R1 业务参数配置化根治，P0-5）
 * <p>与 system_configs 区别：本表专管"影响业务结果计算"的参数
 * （奖金池基数/KPI 门槛/冷静期/双签人数/阶梯系数），与系统参数（治理/审计/通知）解耦。
 * <p>作用域 GLOBAL|GROUP|PROJECT；缓存 TTL 默认 60s（0=不缓存）；版本号用于订阅与历史链。
 */
@TableName(value = "ipd_business_config", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IpdBusinessConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 参数键（如 bonus.poolRate / kpi.stopThreshold / deletion.cooldownDays / gate.dualSignCount） */
    @TableField("config_key")
    private String configKey;

    /** 参数值（字符串持久化，读取方按 value_type 解析） */
    @TableField("config_value")
    private String configValue;

    /** STRING|NUMBER|JSON|BOOL */
    @TableField("value_type")
    private String valueType;

    /** GLOBAL|GROUP|PROJECT */
    @TableField("scope")
    private String scope;

    /** 是否启用（0=禁用读取 fallback 默认值） */
    @TableField("enabled")
    private Integer enabled;

    /** 当前版本号（每次更新自增） */
    @TableField("version")
    private Integer version;

    /** 缓存 TTL（秒），0=不缓存 */
    @TableField("cache_ttl")
    private Integer cacheTtl;

    @TableField("description")
    private String description;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic @TableField("del_flag")
    private String delFlag;
}
