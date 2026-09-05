package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 项目成员（双PM 绑定 + 评级快照；v3 TS-06）
 * 市场PM和研发PM各一条记录，锁定评级用于津贴计算 BR-INC-02
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_members", autoResultMap = true)
public class ProjectMember extends BaseEntity implements SoftDeletable {

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
     * 角色 MARKET_PM|RD_PM（固定不可跨 B7）
     */
    private String role;

    /**
     * 绑定时评级快照（BR-INC-02）
     */
    private String lockedLevel;

    /**
     * 锁定月度津贴额
     */
    private BigDecimal lockedAmount;

    /**
     * 加入日期
     */
    private Date joinDate;

    /**
     * 退出日期
     */
    private Date exitDate;

    /**
     * 退出原因 TRANSFER|VOLUNTARY|LOW_PERF
     */
    private String exitReason;

    /**
     * 奖金资格（放弃置 0，BR-INC-09）
     */
    private String bonusEligible;

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
