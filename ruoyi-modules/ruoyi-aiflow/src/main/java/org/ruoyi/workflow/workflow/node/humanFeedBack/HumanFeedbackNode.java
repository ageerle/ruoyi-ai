package org.ruoyi.workflow.workflow.node.humanFeedBack;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.*;

/**
 * 人机交互节点实现类
 */
@Slf4j
public class HumanFeedbackNode extends AbstractWfNode {

    public HumanFeedbackNode(WorkflowComponent component, WorkflowNode nodeDefinition, WfState wfState, WfNodeState nodeState) {
        super(component, nodeDefinition, wfState, nodeState);
    }

    // 人机交互节点的处理逻辑
    @Override
    public NodeProcessResult onProcess() {
        log.info("Processing HumanFeedback node: {}", node.getTitle());
        // 从状态中获取用户输入数据
        Object humanFeedbackState = state.data().get(HUMAN_FEEDBACK_KEY);
        if (null != humanFeedbackState) {
            String userInput = humanFeedbackState.toString();
            if (StringUtils.isNotBlank(userInput)) {
                // 用户已提供输入，将用户输入添加到节点输入和输出中
                NodeIOData feedbackData = NodeIOData.createByText("output", "default", userInput);
                // 添加到输出列表，这样后续节点可以使用
                state.getOutputs().add(feedbackData);
                // 设置为成功状态
                state.setProcessStatus(NODE_PROCESS_STATUS_SUCCESS);
                log.info("Human feedback processed for node: {}, content: {}", node.getTitle(), userInput);
            } else {
                // 用户输入为空，设置等待状态
                state.setProcessStatus(NODE_PROCESS_STATUS_DOING);
                log.info("Human feedback is empty for node: {}", node.getTitle());
            }
        } else {
            // 没有用户输入，这可能是正常情况（等待用户输入）
            // 但为了确保流程可以继续，我们仍然标记为成功
            state.setProcessStatus(NODE_PROCESS_STATUS_SUCCESS);
            log.info("No human feedback found for node: {}, continuing workflow", node.getTitle());
        }
        return new NodeProcessResult();
    }
}

