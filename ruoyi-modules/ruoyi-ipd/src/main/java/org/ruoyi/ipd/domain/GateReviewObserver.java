package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD Gate 评审列席人员（MEDIUM-1.3；销售/供应/售后/品质/合规 5 类角色）。
 *
 * <p>列席人仅提交观察意见（opinion 字段），不进入主审投票（不写入 gate_reviews 表）。
 * 由超管/组长邀请产生，每位列席人对应一行；uk(gate_id, observer_id) 兜底幂等邀请。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gate_review_observers", autoResultMap = true)
public class GateReviewObserver extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** Gate 实例ID */
    private Long gateId;

    /** 列席人 personId */
    private Long observerId;

    /** 列席角色：SALES/SUPPLY/AFTERSALES/QUALITY/COMPLIANCE */
    private String role;

    /** 邀请人 personId */
    private Long invitedBy;

    /** 邀请时间 */
    private Date invitedAt;

    /** 是否已出席 0否 1是 */
    private Integer attended;

    /** 列席意见（仅 observer 本人可写） */
    private String opinion;

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
