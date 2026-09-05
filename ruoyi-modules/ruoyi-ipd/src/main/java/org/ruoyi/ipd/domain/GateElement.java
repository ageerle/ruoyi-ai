package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

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
}