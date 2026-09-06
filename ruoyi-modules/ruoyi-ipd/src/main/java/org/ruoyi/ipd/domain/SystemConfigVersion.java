package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * IPD 参数版本链（P0-3.3：参数版本持久化、不可变快照与时点解析；AC-GLB-09/10）
 *
 * <p>valid-time 生效期模型，形状以 ipd_dev 真库 system_config_versions 为准（batch_missing_tables.sql §7）：
 * 每次 update 闭合当前开区间行（effective_to=now）并追加新行（effective_from=now, effective_to=NULL）。
 * 不可变承诺 = config_value 与历史行永不改写/删除，仅允许 effective_to 一次闭合；
 * 服务层不提供任何 update config_value / delete 通道（行为级 append-only）。
 *
 * <p>⚠️ 不继承 BaseEntity：本表无 create_by/update_by/update_time/create_dept
 * （对齐 AuditLog 先例），仅 create_time + tenant_id + del_flag。
 * 表在 tenant.excludes 登记过 → 租户插件跳过，tenant_id 落库默认 '000000'，本域不写该列。
 */
@TableName(value = "system_config_versions", autoResultMap = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置键（对齐 system_configs.config_key） */
    @TableField("config_key")
    private String configKey;

    /** 该生效期内的参数值（NOT NULL；不可变承诺的载体） */
    @TableField("config_value")
    private String configValue;

    /** 键内单调递增版本号（uk_config_version 保证唯一） */
    @TableField("version")
    private Integer version;

    /** 生效起始时间（含） */
    @TableField("effective_from")
    private Date effectiveFrom;

    /** 生效截止时间（不含；NULL=当前有效开区间） */
    @TableField("effective_to")
    private Date effectiveTo;

    /** 不可变快照标记（DDL 默认 1；语义锚 AC-GLB-09） */
    @TableField("is_immutable")
    private Boolean isImmutable;

    /** 变更人（会话绑定；遗留 2 参通道落 0=系统通道） */
    @TableField("changed_by")
    private Long changedBy;

    @TableField("change_reason")
    private String changeReason;

    @TableField("create_time")
    private Date createTime;

    @TableField("tenant_id")
    private String tenantId;

    /** 只映射不写入：append-only，本域永不置 1 */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
