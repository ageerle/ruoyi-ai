package org.ruoyi.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 作废请求对象
 *
 * @author may
 */
@Data
public class FlowInvalidBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    @NotNull(message = "流程实例id为空", groups = AddGroup.class)
    private Long id;

    /**
     * 审批意见
     */
    private String comment;
}
