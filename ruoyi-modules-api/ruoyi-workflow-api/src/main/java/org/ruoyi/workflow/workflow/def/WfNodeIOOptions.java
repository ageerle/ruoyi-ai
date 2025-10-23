package org.ruoyi.workflow.workflow.def;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataOptionsContent;

import java.util.Map;

/**
 * 用户输入参数-下拉选项类型 参数定义
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WfNodeIOOptions extends WfNodeIO {
    protected Integer type = WfIODataTypeEnum.OPTIONS.getValue();
    private Boolean multiple;

    @Override
    public boolean checkValue(NodeIOData data) {
        if (!(data.getContent() instanceof NodeIODataOptionsContent optionsData)) {
            return false;
        }
        Map<String, Object> value = optionsData.getValue();
        if (required && null == value) {
            return false;
        }
        //如果设置了单选，传过来的值是多项，则检查不通过
        return multiple || null == value || value.size() <= 1;
    }
}
