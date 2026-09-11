package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 共担 KPI 双组长确认行（P3-1.2-BACKEND；ZK 原型页 20「共担KPI双组长确认」）。
 *
 * <p>collect 归集成功后按 K01-K04 各生成一条 PENDING；两位 GROUP_LEADER 依次
 * first/second 签署，第二签落库后 CONFIRMED；OVERDUE 为读时派生态（PENDING 且已过
 * deadlineAt），不落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kpi_shared_confirms", autoResultMap = true)
public class KpiSharedConfirm extends BaseEntity implements SoftDeletable {

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
     * 考核周期 YYYY-MM
     */
    private String period;

    /**
     * 归集责任人（产品组长 personId）
     */
    private Long personId;

    /**
     * 指标编码 K01~K04
     */
    private String metricCode;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标权重（K01=0.15/K02=0.10/K03=0.10/K04=0.05）
     */
    private BigDecimal weight;

    /**
     * 归集截止时间（次月第 N 个工作日 18:00）
     */
    private Date deadlineAt;

    /**
     * 状态 PENDING|CONFIRMED（OVERDUE 读时派生，不落库）
     */
    private String status;

    /**
     * 第一组长签署人
     */
    private Long firstConfirmedBy;

    /**
     * 第一组长签署时间
     */
    private Date firstConfirmedAt;

    /**
     * 第二组长签署人
     */
    private Long secondConfirmedBy;

    /**
     * 第二组长签署时间
     */
    private Date secondConfirmedAt;

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
