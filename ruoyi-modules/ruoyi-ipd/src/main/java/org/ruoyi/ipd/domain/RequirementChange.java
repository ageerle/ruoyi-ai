package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * IPD 需求变更单（双签否决对象 BR-GATE-07）
 * 每个变更单需经双PM 签署批准方可生效，拒绝则退回
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "requirement_changes", autoResultMap = true)
public class RequirementChange extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 需求ID
     */
    private Long requirementId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 变更前快照（JSON）
     */
    private String beforeSnapshot;

    /**
     * 变更后快照（JSON）
     */
    private String afterSnapshot;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 双签签名聚合（P2-6.1；MARKET_PM=APPROVE;RD_PM=APPROVE 形式）
     * 历史版本冻结：提交 PENDING_SIGN 时即不可变（业务层守卫）
     */
    private String signatures;

    /**
     * 状态 DRAFT|PENDING_SIGN|APPROVED|REJECTED
     */
    private String status;

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
