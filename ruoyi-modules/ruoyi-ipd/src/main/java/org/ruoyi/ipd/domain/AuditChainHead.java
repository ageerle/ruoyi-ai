package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 审计链分配器锚行（audit_log_chain_heads，单行 GLOBAL；owner 2026-09-05 拍板 P 悲观锁变体）。
 * <p>表本身可变（transaction-locked allocator），audit_logs 仍只追加（G-02 不变）：
 * append 事务内 SELECT ... FOR UPDATE 锚行 → 分配 seq/prevHash → 前移 last_seq/last_hash/next_seq。
 * <p>无 tenant_id 列、无多租户语义，已登记 tenant.excludes（未登记则多租户插件追加过滤 → 锚行读不到）。
 */
@Data
@TableName("audit_log_chain_heads")
public class AuditChainHead implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "chain_key", type = IdType.INPUT)
    private String chainKey;

    @TableField("last_seq")
    private Long lastSeq;

    @TableField("last_hash")
    private String lastHash;

    @TableField("next_seq")
    private Long nextSeq;

    @TableField("initialized_at")
    private Date initializedAt;
}
