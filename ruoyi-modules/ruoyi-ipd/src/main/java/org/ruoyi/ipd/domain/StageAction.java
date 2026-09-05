package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 阶段动作实例（69 动作目录经 ActionCatalog 实例化，深管/轻管 BR-IPD-03/04/05）
 * 完成校验由 StageActionService 按 depth + valueFields 强制，不信任前端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "stage_actions", autoResultMap = true)
public class StageAction extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String actionCode;
    private String actionName;
    private String ownerRole;
    private String depth;
    private String status;
    private String isBlocking;
    private Date actualDoneAt;
    private BigDecimal farValue;
    private BigDecimal frrValue;
    private String certNo;
    private Date certPassedAt;
    private String algoType;
    private String isBioFeature;
    private Date dueDate;
    private Long sopId;
}