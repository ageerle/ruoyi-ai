package org.ruoyi.workflow.workflow.def;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataFilesContent;

/**
 * 用户输入参数-文件列表类型 参数定义
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WfNodeIOFiles extends WfNodeIO {
    protected Integer type = WfIODataTypeEnum.FILES.getValue();
    private Integer limit;

    @Override
    public boolean checkValue(NodeIOData data) {
        if (!(data.getContent() instanceof NodeIODataFilesContent wfNodeIOFiles)) {
            return false;
        }
        return !required || !CollectionUtils.isEmpty(wfNodeIOFiles.getValue());
    }
}
