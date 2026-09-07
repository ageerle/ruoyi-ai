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
 * KPI 规则版本快照（P3-2.2 规则版本快照子模块）。
 *
 * <p>每次规则调整（self/market/rd 权重等）落一条不可变快照，
 * 评分回算只读历史规则，不重新解析当前规则。删除走软删除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kpi_rule_snapshots", autoResultMap = true)
public class KpiRuleSnapshot extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 规则版本号（单调递增，1,2,3...）。
     * 与 id 解耦：版本号是面向业务的语义版本；id 是物理主键。
     */
    @TableField("version")
    private Long version;

    /** 规则生效起点（含）。 */
    @TableField("effective_from")
    private Date effectiveFrom;

    /** 规则生效终点（不含，null 表示至今有效）。 */
    @TableField("effective_to")
    private Date effectiveTo;

    /** 当时生效规则的不可变 JSON 快照。 */
    @TableField("rule_json")
    private String ruleJson;

    /** 创建人 ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间（冗余 BaseEntity.createTime，便于显式查询）。 */
    @TableField("created_at")
    private Date createdAt;

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
