package org.ruoyi.workflow.workflow.node.start;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeIODataUtil;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import java.util.List;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;
import static org.ruoyi.workflow.enums.ErrorEnum.A_WF_NODE_CONFIG_ERROR;
import static org.ruoyi.workflow.enums.ErrorEnum.A_WF_NODE_CONFIG_NOT_FOUND;

@Slf4j
public class StartNode extends AbstractWfNode {

    public StartNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    public NodeProcessResult onProcess() {
        String objectConfig = node.getNodeConfig();
        if (null == objectConfig) {
            throw new BaseException(A_WF_NODE_CONFIG_NOT_FOUND.getInfo());
        }
        List<NodeIOData> result;
        StartNodeConfig nodeConfigObj = JsonUtil.fromJson(objectConfig, StartNodeConfig.class);
        if (null == nodeConfigObj) {
            log.warn("找不到开始节点的配置");
            throw new BaseException(A_WF_NODE_CONFIG_ERROR.getInfo());
        }
        if (StringUtils.isNotBlank(nodeConfigObj.getPrologue())) {
            result = List.of(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "default", nodeConfigObj.getPrologue()));
        } else {
            result = WfNodeIODataUtil.changeInputsToOutputs(state.getInputs());
        }
        return NodeProcessResult.builder().content(result).build();
    }

}
