package org.ruoyi.workflow.domain.bo;


import cn.hutool.core.util.ObjectUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.workflow.domain.FlowInstanceBizExt;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 启动流程对象
 *
 * @author may
 */
@Data
public class StartProcessBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务唯一值id
     */
    @NotBlank(message = "业务ID不能为空", groups = {AddGroup.class})
    private String businessId;

    /**
     * 流程定义编码
     */
    @NotBlank(message = "流程定义编码不能为空", groups = {AddGroup.class})
    private String flowCode;

    /**
     * 办理人(可不填 用于覆盖当前节点办理人)
     */
    private String handler;

    /**
     * 流程变量，前端会提交一个元素{'entity': {业务详情数据对象}}
     */
    private Map<String, Object> variables;

    /**
     * 流程业务扩展信息
     */
    private FlowInstanceBizExt bizExt;

    public Map<String, Object> getVariables() {
        if (variables == null) {
            return new HashMap<>(16);
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }

    public FlowInstanceBizExt getBizExt() {
        if (ObjectUtil.isNull(bizExt)) {
            bizExt = new FlowInstanceBizExt();
        }
        return bizExt;
    }
}
