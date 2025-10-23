package org.ruoyi.workflow.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.*;
import org.ruoyi.workflow.workflow.data.NodeIOData;

import java.util.ArrayList;
import java.util.List;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;


@Slf4j
public class EndNode extends AbstractWfNode {

    public EndNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    protected NodeProcessResult onProcess() {
        List<NodeIOData> result = new ArrayList<>();
        ObjectNode objectConfig = JsonUtil.toBean(node.getNodeConfig(), ObjectNode.class);
        JsonNode resultNode = objectConfig.get("result");
        String output = "";
        if (null == resultNode) {
            log.warn("EndNode result config is empty, nodeUuid: {}, title: {}", node.getUuid(), node.getTitle());
        } else {
            String resultTemplate = resultNode.asText();
            WfNodeIODataUtil.changeFilesContentToMarkdown(state.getInputs());
            output = WorkflowUtil.renderTemplate(resultTemplate, state.getInputs());
        }
        result.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", output));
        return NodeProcessResult.builder().content(result).build();
    }
}
