package org.ruoyi.workflow.workflow.def;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.workflow.data.NodeIOData;

import java.io.Serializable;

/**
 * 工作流节点输入输出参数定义
 */
@Data
@NoArgsConstructor
public abstract class WfNodeIO implements Serializable {

    protected String uuid;
    protected Integer type;
    protected String name;
    protected String title;
    protected Boolean required;

    /**
     * 检查数据是否合规
     *
     * @param data 节点输入输出数据
     * @return 是否正确
     */
    public abstract boolean checkValue(NodeIOData data);
}
