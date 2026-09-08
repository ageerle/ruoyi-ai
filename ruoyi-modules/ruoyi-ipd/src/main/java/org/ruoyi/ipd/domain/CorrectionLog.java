package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 字段更正留痕（P3-2.2 更正留痕子模块）。
 *
 * <p>任何对 ProjectScore / KpiRecord 等核心实体的字段更正操作都必须落一条本表记录，
 * 含旧值/新值/原因/操作人/操作时间，确保审计追溯。删除走软删除。
 *
 * <p>R-P3-2.2-POSTREVIEW：{@link EntityType} 枚举约束实体类型取值范围（白名单），
 * 防止自由字符串写入绕过入参校验。{@code entityType} DB 字段仍为字符串（DDL 兼容），
 * 枚举仅作 Service 层校验用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "correction_logs", autoResultMap = true)
public class CorrectionLog extends BaseEntity implements SoftDeletable {

    /**
     * 更正留痕实体类型白名单（P3-2.2 + R-P3-2.2-POSTREVIEW）。
     * 命名一律大写下划线，与现存 {@code entity_type} 列字符串对齐；
     * Service 层校验「必须在该枚举内」，DB 列保留 varchar 兼容既有 DDL。
     */
    public enum EntityType {
        PROJECT_SCORE,
        KPI_RECORD,
        SOP_TEMPLATE,
        REQUIREMENT,
        OTHER
    }

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 实体类型 e.g. ProjectScore/KpiRecord。 */
    @TableField("entity_type")
    private String entityType;

    /** 实体 ID。 */
    @TableField("entity_id")
    private Long entityId;

    /** 更正字段名。 */
    @TableField("field_name")
    private String fieldName;

    /** 旧值（字符串表示，避免数字/日期精度问题）。 */
    @TableField("old_value")
    private String oldValue;

    /** 新值。 */
    @TableField("new_value")
    private String newValue;

    /** 更正原因（业务文案）。 */
    @TableField("reason")
    private String reason;

    /** 操作人 ID。 */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名（冗余存储，便于审计反查不联表）。 */
    @TableField("operator_name")
    private String operatorName;

    /** 操作时间。 */
    @TableField("operated_at")
    private Date operatedAt;

    /** 多租户隔离。 */
    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
