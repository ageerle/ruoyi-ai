package org.ruoyi.common.core.domain.dto;


import cn.hutool.core.util.ObjectUtil;
import lombok.Data;

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
public class StartProcessDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务唯一值id
     */
    private String businessId;

    /**
     * 流程定义编码
     */
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
    private FlowInstanceBizExtDTO bizExt;

    public Map<String, Object> getVariables() {
        if (variables == null) {
            return new HashMap<>(16);
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }

    public FlowInstanceBizExtDTO getBizExt() {
        if (ObjectUtil.isNull(bizExt)) {
            bizExt = new FlowInstanceBizExtDTO();
        }
        return bizExt;
    }
}
