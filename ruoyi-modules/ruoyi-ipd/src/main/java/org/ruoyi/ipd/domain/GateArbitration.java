package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * Gate 冲突仲裁与超管终裁意见（P2-5.4；AC-GATE-10 / BR-GATE-06）。
 *
 * <p>双 PM 意见分歧（一轮内先 APPROVE 后 REJECT）⇒ 自动邀请双方产品组长仲裁；
 * 两组长意见仍不一致 ⇒ 升级超管终裁。终裁结果写入项目审计日志永久归档。
 *
 * <p>独立于 gate_reviews 的原因：两位组长的 reviewerType 同为 GROUP_LEADER，
 * 会撞 gate_reviews 的 uk(gate_id, reviewer_type, round)；本表按
 * (gate_id, round, arbitrator_id) 对人去重。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gate_arbitrations", autoResultMap = true)
public class GateArbitration extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Gate 实例ID
     */
    private Long gateId;

    /**
     * 评审轮次（与冲突发生轮对齐）
     */
    private Integer round;

    /**
     * 仲裁人类型 GROUP_LEADER|SUPER_ADMIN
     */
    private String arbitratorType;

    /**
     * 仲裁人/终裁人ID
     */
    private Long arbitratorId;

    /**
     * 意见 APPROVE|REJECT
     */
    private String decision;

    /**
     * 仲裁/终裁说明
     */
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
