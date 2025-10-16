package org.ruoyi.workflow.workflow.def;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataNumberContent;

/**
 * 用户输入参数-数字类型 参数定义
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WfNodeIONumber extends WfNodeIO {
    protected Integer type = WfIODataTypeEnum.NUMBER.getValue();

    @Override
    public boolean checkValue(NodeIOData data) {
        if (!(data.getContent() instanceof NodeIODataNumberContent nodeIONumber)) {
            return false;
        }
        return !required || null != nodeIONumber.getValue();
    }
}
