package org.ruoyi.workflow.workflow.def;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataTextContent;

/**
 * 用户输入参数-文本类型 参数定义
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WfNodeIOText extends WfNodeIO {

    protected Integer type = WfIODataTypeEnum.TEXT.getValue();

    @JsonProperty("max_length")
    private Integer maxLength;

    @Override
    public boolean checkValue(NodeIOData data) {
        if (!(data.getContent() instanceof NodeIODataTextContent optionsData)) {
            return false;
        }
        String value = optionsData.getValue();
        if (required && null == value) {
            return false;
        }
        return null == maxLength || value.length() <= maxLength;
    }
}
