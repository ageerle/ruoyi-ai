package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * G5 上市 90 天复盘待办（AC-GATE-13；P2-5.6）。
 *
 * <p>G5 通过 + launchDate 入参 ⇒ 生成 scheduled_at = launchDate + 90d 的 PENDING 待办；
 * assigneeId 默认取项目当前主 MARKET_PM（移交后跟随），状态机 PENDING → COMPLETED/OVERDUE。
 *
 * <p>删除走 DeletionRequestService + DeleteAuditService（暂未启用，与其他业务表保持同款）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "post_launch_reviews", autoResultMap = true)
public class PostLaunchReview extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 复盘待办截止日 = launchDate + 90d */
    private Date scheduledAt;

    /** 状态 PENDING/COMPLETED/OVERDUE */
    private String status;

    /** 当前负责 PM（移交后跟 ProjectMember 取主 MARKET_PM） */
    private Long assigneeId;

    /** 实际营收（复盘完成时填写） */
    private BigDecimal actualRevenue;

    /** 客户反馈（≤2000 字符） */
    private String customerFeedback;

    /** KPI 达成（≤2000 字符） */
    private String kpiAchievement;

    /** 经验教训（≤4000 字符） */
    private String lessons;

    /** 完成时间 */
    private Date completedAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter */
    public void setDelFlag(String flag) { this.delFlag = flag; }

    /** 租户 ID（多租户隔离） */
    private String tenantId;
}