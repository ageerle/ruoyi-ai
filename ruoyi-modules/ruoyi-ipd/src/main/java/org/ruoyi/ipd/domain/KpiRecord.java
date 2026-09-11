package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD KPI 记录（功能+共担；v3 TS-06）
 * 功能KPI归个人，共担KPI归产品组长（kpi.functionalWeight=0.6/0.4）
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kpi_records", autoResultMap = true)
public class KpiRecord extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 人员ID
     */
    private Long personId;

    /**
     * KPI 类型 FUNCTIONAL|SHARED（共担四项归集由产品组长）
     */
    private String kpiType;

    /**
     * 考核周期 YYYY-MM（每月 5 日截止 kpi.monthlyDeadlineDay）
     */
    private String period;

    /**
     * 功能得分
     */
    private BigDecimal functionalScore;

    /**
     * 共担四项明细（JSON）
     */
    private String sharedDetail;

    /**
     * 综合得分（kpi.functionalWeight=0.6/0.4）
     */
    private BigDecimal comprehensiveScore;

    /**
     * 在研分段
     */
    private String segment;

    /**
     * 同一人、项目、周期、KPI 类型的追加归集版本。
     */
    private Integer revision;

    /**
     * 评分人
     */
    private Long scoredBy;

    /**
     * 状态 DRAFT|FINALIZED
     */
    private String status;

    /**
     * 评分日期
     */
    private Date scoredAt;

    /**
     * 软删除标志（0正常 1已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
