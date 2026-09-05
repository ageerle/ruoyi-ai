package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;

/** IPD 交付物（附件关联，深管完成校验 BR-IPD-03：至少 1 条未删记录方可标已完成） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "deliverables", autoResultMap = true)
public class Deliverable extends BaseEntity {

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
}