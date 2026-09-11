package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;

/** IPD 交付物（附件关联，深管完成校验 BR-IPD-03：至少 1 条未删记录方可标已完成）
 *  删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除；del_flag 列已存在于 P0 schema）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "deliverables", autoResultMap = true)
public class Deliverable extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long actionId;
    private Long projectId;
    private String fileName;
    private Long ossId;
    private String fileUrl;
    private Long fileSize;
    private Long uploadedBy;
    private Date uploadedAt;
    /** 软删除标志（0正常 1已删；P1-4.3 已删附件不计 DONE 计数；删除走两级审核，禁物理 DELETE） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}