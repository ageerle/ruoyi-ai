package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 移交记录（项目移交+超管移交 BR-HAND/BR-ADM）
 * 先移交后禁用，支持 PROJECT/SUPER_ADMIN/BATCH 三种移交类型
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "handover_records", autoResultMap = true)
public class HandoverRecord extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 移交类型 PROJECT|SUPER_ADMIN|BATCH
     */
    private String handoverType;

    /**
     * 移交人ID
     */
    private Long fromPersonId;

    /**
     * 承接人ID
     */
    private Long toPersonId;

    /**
     * 项目ID（项目移交时填写）
     */
    private Long projectId;

    /**
     * 移交范围（角色独立移交：市场PM/研发PM 各自数据跟随，JSON）
     */
    private String scope;

    /**
     * 状态 DRAFT|CONFIRMED|COMPLETED
     */
    private String status;

    /**
     * 确认时间
     */
    private Date confirmedAt;

    /**
     * 完成时间
     */
    private Date completedAt;

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

    /** 移交角色维度 RD_PM|MARKET_PM（AC-HAND-06 仅目标角色变更） */
    private String handoverRole;

    /** 移交说明（≤1000字符，spec 字段模型） */
    private String note;
}
