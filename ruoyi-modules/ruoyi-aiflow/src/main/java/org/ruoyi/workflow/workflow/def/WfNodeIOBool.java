package org.ruoyi.workflow.workflow.def;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataBoolContent;

/**
 * 用户输入参数-布尔类型 参数定义
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WfNodeIOBool extends WfNodeIO {

    protected Integer type = WfIODataTypeEnum.BOOL.getValue();

    @Override
    public boolean checkValue(NodeIOData data) {
        if (!(data.getContent() instanceof NodeIODataBoolContent)) {
            return false;
        }
        return !required || null != data.getContent().getValue();
    }
}
