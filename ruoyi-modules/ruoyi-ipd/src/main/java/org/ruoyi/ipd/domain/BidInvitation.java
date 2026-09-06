package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 招标单（BR-TEAM）
 * 支持一对一指定研发PM或公开征集，支持状态机 OPEN→SELECTED/EXPIRED→CLOSED
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "bid_invitations", autoResultMap = true)
public class BidInvitation extends BaseEntity implements SoftDeletable {

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
     * 招标方式 ONE_TO_ONE|PUBLIC
     */
    private String mode;

    /**
     * 一对一指定研发PM ID
     */
    private Long targetPersonId;

    /**
     * 招标标题
     */
    private String title;

    /**
     * 招标内容
     */
    private String content;

    /**
     * 有效期截止时间（入参/出参格式 yyyy-MM-dd HH:mm:ss，与前端页21一致；
     * R8-P0-11 先例：/api/v1 链路全局 date-format 未生效，Date 字段就地标注）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireAt;

    /**
     * 状态 OPEN|SELECTED|EXPIRED|CLOSED
     */
    private String status;

    /**
     * 选定应标记录ID
     */
    private Long selectedResponseId;

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
