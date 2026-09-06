package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * P2-5.1 Gate 要素逐项判定结果（激活 QA-04-D1 点名的死表 gate_element_results）。
 *
 * <p>每要素一行：PASS / CONDITIONAL / FAIL；CONDITIONAL 必填说明（遗留项深闭环归 P2-5.3）；
 * FAIL（尤其否决项）必填证据附件引用（AC-GATE-02）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gate_element_results", autoResultMap = true)
public class GateElementResult extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** Gate 实例 */
    private Long gateId;

    /** 要素定义（gate_review_elements.id） */
    private Long elementId;

    /** 判定 PASS|CONDITIONAL|FAIL */
    private String result;

    /** 带条件通过说明（CONDITIONAL 必填） */
    private String conditionNote;

    /** 判定证据附件引用（否决/FAIL 必填，AC-GATE-02） */
    private String evidenceRef;

    /** 遗留项跟踪（P2-5.3 深闭环） */
    private String leftoverItem;

    /**
     * 条件遗留责任人（AC-GATE-16：CONDITIONAL 判定必填）
     */
    private Long responsiblePersonId;

    private Date leftoverDueAt;

    /**
     * 遗留关闭凭证（close 时必填写入）
     */
    private String closedEvidence;

    /**
     * OPEN 未关 / CLOSED 已关（遗留查询仅依赖本表，要素停用不消除遗留）
     */
    private String leftoverStatus;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter */
    public void setDelFlag(String flag) { this.delFlag = flag; }

    /** 签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21） */
    private Date signDueAt;

    /** 要素快照（GateElement 的 JSON 备份，便于后续审计回溯） */
    private String elementSnapshot;
}
