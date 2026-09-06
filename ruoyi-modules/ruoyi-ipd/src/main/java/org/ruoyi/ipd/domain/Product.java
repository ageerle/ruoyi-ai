package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * IPD 产品——TS-05 products；BR-PROD-01 三路来源（超管导入在售型号 / PM 新增 / 游客「其他」占位）
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "products", autoResultMap = true)
public class Product extends BaseEntity implements SoftDeletable {

    public static final String SRC_ADMIN_IMPORT = "ADMIN_IMPORT";
    public static final String SRC_PM_NEW = "PM_NEW";
    public static final String SRC_GUEST_OTHER = "GUEST_OTHER";

    /** 在售（超管导入） */
    public static final String ST_ON_SALE = "ON_SALE";
    /** 在研（PM 新增） */
    public static final String ST_IN_RD = "IN_RD";
    /** 停用 */
    public static final String ST_INACTIVE = "INACTIVE";
    /** 兼容旧数据 ACTIVE */
    public static final String ST_ACTIVE = "ACTIVE";

    public static final java.util.Set<String> STATUSES =
        java.util.Set.of(ST_ON_SALE, ST_IN_RD, ST_INACTIVE, ST_ACTIVE);

    @TableId
    private Long id;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 在售型号编码（超管导入） */
    private String modelCode;

    /** 来源 ADMIN_IMPORT|PM_NEW|GUEST_OTHER */
    private String source;

    /** 关联项目（1:1 唯一，uk_products_project）；软删对端时需可写 null */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long projectId;

    /** 归属产品组 */
    private Long groupId;

    /** 状态 ACTIVE|INACTIVE */
    private String status;

    /** 租户ID */
    private String tenantId;

    /** 删除标志（0正常 1删除；删除走两级审核引擎，禁物理删除 G-02） */
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}