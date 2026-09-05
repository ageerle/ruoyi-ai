package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 需求池（免登录提交+查询码；v3 TS-06）
 * 游客通过 PORTAL_GUEST 来源提交，内部用户通过 INTERNAL 来源提交
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "requirements", autoResultMap = true)
public class Requirement extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 产品ID（游客「其他」可空路由）
     */
    private Long productId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 来源 PORTAL_GUEST|INTERNAL
     */
    private String source;

    /**
     * 提交人姓名
     */
    private String submitterName;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 需求标题
     */
    private String title;

    /**
     * 需求内容
     */
    private String content;

    /**
     * 查询码（游客凭码查进度）
     */
    private String queryCode;

    /**
     * 状态 SUBMITTED|ACCEPTED|EVALUATING|SCHEDULED|PROCESSING|CLOSED|ARCHIVED
     */
    private String status;

    /**
     * 市场PM ID
     */
    private Long marketPmId;

    /**
     * 研发PM ID
     */
    private Long rdPmId;

    /**
     * 按产品路由双PM时间
     */
    private Date routedAt;

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
