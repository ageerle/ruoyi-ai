package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 应标记录（研发PM 应标；不留痕的拒绝不记录）
 * 研发PM对招标单提交应标，支持 ACCEPTED/REJECTED/WITHDRAWN 状态
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "bid_responses", autoResultMap = true)
public class BidResponse extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 招标单ID
     */
    private Long invitationId;

    /**
     * 研发PM ID
     */
    private Long rdPmId;

    /**
     * 应标说明
     */
    private String responseNote;

    /**
     * 应标决定（accept|reject）；仅请求携带不入库：BR-TEAM-03 拒绝不留痕，accept 落库 PENDING
     */
    @TableField(exist = false)
    private String decision;

    /**
     * 状态 PENDING|ACCEPTED|REJECTED|WITHDRAWN
     */
    private String status;

    /**
     * 应标时间
     */
    private Date respondedAt;

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
