package org.ruoyi.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 流程变量参数
 *
 * @author may
 */
@Data
public class FlowUrgeTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务id
     */
    @NotNull(message = "任务id为空", groups = AddGroup.class)
    private List<Long> taskIdList;

    /**
     * 消息类型
     */
    private List<String> messageType;

    /**
     * 催办内容
     */
    @NotNull(message = "催办内容为空", groups = AddGroup.class)
    private String message;
}
