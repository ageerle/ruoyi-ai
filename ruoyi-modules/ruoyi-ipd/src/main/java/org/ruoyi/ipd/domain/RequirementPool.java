package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 需求池（AC-REQ-09；P2-6.x）。
 *
 * <p>DDL：batch_missing_tables.sql L94-115。
 * <p>字段映射：
 * <ul>
 *   <li>id (bigint, PK)</li>
 *   <li>projectId (project_id bigint) — 关联项目（可空=未立项需求）</li>
 *   <li>title (varchar(256))</li>
 *   <li>description (text)</li>
 *   <li>source (varchar(32)) — CUSTOMER|MARKET|INTERNAL</li>
 *   <li>priority (varchar(16)) — LOW|MEDIUM|HIGH|CRITICAL</li>
 *   <li>status (varchar(16)) — SUBMITTED|REVIEWING|APPROVED|REJECTED|IMPLEMENTED</li>
 *   <li>submitterId (submitter_id bigint)</li>
 *   <li>reviewerId (reviewer_id bigint)</li>
 * </ul>
 *
 * <p>删除走软删除（del_flag='0'/'1'）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "requirement_pool", autoResultMap = true)
public class RequirementPool extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联项目（可空=未立项需求） */
    private Long projectId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** CUSTOMER|MARKET|INTERNAL */
    private String source;

    /** LOW|MEDIUM|HIGH|CRITICAL */
    private String priority;

    /** SUBMITTED|REVIEWING|APPROVED|REJECTED|IMPLEMENTED */
    private String status;

    /** 提交人 */
    private Long submitterId;

    /** 评审人 */
    private Long reviewerId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
