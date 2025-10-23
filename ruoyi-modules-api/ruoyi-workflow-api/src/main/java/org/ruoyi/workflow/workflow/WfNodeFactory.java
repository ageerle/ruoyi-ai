package org.ruoyi.workflow.workflow;

import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.ruoyi.workflow.workflow.node.EndNode;
import org.ruoyi.workflow.workflow.node.answer.LLMAnswerNode;
import org.ruoyi.workflow.workflow.node.start.StartNode;

public class WfNodeFactory {
    public static AbstractWfNode create(WorkflowComponent wfComponent, WorkflowNode nodeDefinition,
                                        WfState wfState, WfNodeState nodeState) {
        AbstractWfNode wfNode = null;
        switch (WfComponentNameEnum.getByName(wfComponent.getName())) {
            case START -> wfNode = new StartNode(wfComponent, nodeDefinition, wfState, nodeState);
            case LLM_ANSWER -> wfNode = new LLMAnswerNode(wfComponent, nodeDefinition, wfState, nodeState);
            case END -> wfNode = new EndNode(wfComponent, nodeDefinition, wfState, nodeState);
            default -> {
            }
        }
        return wfNode;
    }
}
