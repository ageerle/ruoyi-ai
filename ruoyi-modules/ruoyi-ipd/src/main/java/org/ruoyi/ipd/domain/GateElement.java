package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * Gate 评审要素定义（33 项/14 否决位，超管可增删改——BR-IPD-06 配套）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "gate_review_elements", autoResultMap = true)
public class GateElement extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 适用 Gate G1..G5 */
    private String gateCode;
    private String elementCode;
    private String elementName;
    private String passStandard;
    /** 1=否决项（命中无法提交通过） */
    private String isVeto;
    private Integer sortOrder;
    private String enabled;

    /** 生命周期：draft/published/archived（页47）；draft 与 archived 对业务不可见 */
    private String status;

    /** 发布版本号：新建草稿=0，每次 publish 递增 */
    private Integer version;

    /** 双否决位：1=该否决项命中需双签确认（定义层标记，评审侧 P2-5.2 消费） */
    private String vetoDualRequired;

    /** 阈值配置 JSON 对象（键非空、值均为整数），如 {"minCustomerVerifications":3} */
    private String thresholdJson;

    /** 签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21） */
    private Date signDueAt;

    /** 签署期限已延长次数（AC-GATE-21 上限 3） */
    private Integer signExtensionCount;
}