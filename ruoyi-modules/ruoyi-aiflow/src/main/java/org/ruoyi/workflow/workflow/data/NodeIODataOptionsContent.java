package org.ruoyi.workflow.workflow.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class NodeIODataOptionsContent extends NodeIODataContent<Map<String, Object>> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String title;

    private Integer type = WfIODataTypeEnum.OPTIONS.getValue();

    private Map<String, Object> value;
}