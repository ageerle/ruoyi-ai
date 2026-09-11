package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * SOP 模板主表（P1-3.3）
 * <p>由超管维护；每个 {@link #templateCode} 维护一个版本序列（{@link #version} 自增）。
 * 新版本发布时旧版本自动 ARCHIVED（BR-IPD-SOP-02）；实例化走 {@code SopTemplateInstance} 快照。
 * <p>category 取值 DEEP_MGMT（深管）/LIGHT_MGMT（轻管）/MIXED（混合），与 69 动作深度枚举对齐。
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sop_templates", autoResultMap = true)
public class SopTemplate extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板编码（同编码下版本号自增；template_code + version 联合唯一约束） */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板说明 */
    private String description;

    /**
     * 业务版本号（同 template_code 下从 1 开始自增；非乐观锁；
     * 真正的乐观锁由 MyBatis-Plus {@code @Version} 字段独立承担，本字段为业务字段）
     */
    private Long version;

    /** 生效起点 */
    private Date effectiveFrom;

    /** 生效终点（null=当前生效） */
    private Date effectiveTo;

    /** 状态 DRAFT|PUBLISHED|ARCHIVED */
    private String status;

    /** 分类 DEEP_MGMT|LIGHT_MGMT|MIXED */
    private String category;

    /** 创建人 Person ID（与 BaseEntity.createBy 重复冗余以便模板审计检索） */
    private String createdBy;

    /** 软删除标志（0正常 1已删） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 租户ID */
    private String tenantId;

    /** 显式覆盖 Lombok @Accessors(chain=true) 链式 setter 以匹配 SoftDeletable 接口签名 */
    public void setDelFlag(String flag) { this.delFlag = flag; }

    /** 状态枚举字面量（与 DB 字段对齐，避免散落字面量） */
    public static final class Status {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";
        public static final String ARCHIVED = "ARCHIVED";
        private Status() {}
    }

    /** 分类枚举字面量 */
    public static final class Category {
        public static final String DEEP_MGMT = "DEEP_MGMT";
        public static final String LIGHT_MGMT = "LIGHT_MGMT";
        public static final String MIXED = "MIXED";
        private Category() {}
    }
}