package org.ruoyi.workflow.workflow.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class NodeIODataFilesContent extends NodeIODataContent<List<String>> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String title;

    private Integer type = WfIODataTypeEnum.FILES.getValue();

    private List<String> value;
}