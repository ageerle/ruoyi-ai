package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * IPD 审计日志（只追加 + SHA256 hash 链，v3 TS-06/TS-08）
 * ⚠️ 不继承 BaseEntity：本表无 update_time/del_flag（只追加表，AC-AUD-01 防篡改）
 */
@TableName(value = "audit_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 全局递增序号（hash 链顺序锚，DB 自增） */
    @TableField(value = "seq", insertStrategy = FieldStrategy.NEVER)
    private Long seq;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("operator_name")
    private String operatorName;
    @TableField("operator_role")
    private String operatorRole;
    /** CREATE|UPDATE|DELETE|APPROVE|REJECT|HANDOVER|LOGIN... */
    @TableField("action")
    private String action;
    @TableField("entity_type")
    private String entityType;
    @TableField("entity_id")
    private Long entityId;
    @TableField("before_data")
    private String beforeData;
    @TableField("after_data")
    private String afterData;
    @TableField("reason")
    private String reason;
    /** 前条 hash（链首 64 个 0） */
    @TableField("prev_hash")
    private String prevHash;
    /** SHA256(prevHash+本条内容) */
    @TableField("curr_hash")
    private String currHash;
    @TableField("ip_address")
    private String ipAddress;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("create_time")
    private Date createTime;
}