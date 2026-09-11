package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.Version;
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
    /**
     * P1-9.1 / BR-PROD-03：HISTORICAL_MISSING=历史缺失（不伪造 DONE，门禁视为已满足）。
     */
    private String historyMark;
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
    /** 备注（存量替代佐证等） */
    private String remark;

    /**
     * 软删除标志（0正常 1已删）—— 补 entity 与 DDL 不一致（[SEC-FIX] 2026-09-06）。
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** MyBatis-Plus 乐观锁：transit() 并发由 OptimisticLockerInnerInterceptor 拦截；DDL 见 2026-09-05-ipd-p143-optimistic-lock.sql。 */
    @Version
    @TableField(value = "version", insertStrategy = FieldStrategy.NOT_NULL)
    private Integer version;
}