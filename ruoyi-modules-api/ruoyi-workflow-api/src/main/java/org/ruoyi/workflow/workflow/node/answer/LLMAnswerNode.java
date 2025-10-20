package org.ruoyi.workflow.workflow.node.answer;

import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.SpringUtil;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import java.util.List;

/**
 * 【节点】LLM生成回答 <br/>
 * 节点内容固定格式：LLMAnswerNodeConfig
 */
@Slf4j
public class LLMAnswerNode extends AbstractWfNode {

    public LLMAnswerNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    /**
     * nodeConfig格式：<br/>
     * {"prompt": "将以下内容翻译成英文：{input}","model_platform":"deepseek","model_name":"deepseek-chat"}<br/>
     *
     * @return LLM的返回内容
     */
    @Override
    public NodeProcessResult onProcess() {
        LLMAnswerNodeConfig nodeConfigObj = checkAndGetConfig(LLMAnswerNodeConfig.class);
        String inputText = getFirstInputText();
        log.info("LLM answer node config:{}", nodeConfigObj);
        String prompt = inputText;
        if (StringUtils.isNotBlank(nodeConfigObj.getPrompt())) {
            prompt = WorkflowUtil.renderTemplate(nodeConfigObj.getPrompt(), state.getInputs());
        }
        log.info("LLM prompt:{}", prompt);
        // 调用LLM
        WorkflowUtil workflowUtil = SpringUtil.getBean(WorkflowUtil.class);
        String modelName = nodeConfigObj.getModelName();
        String category = nodeConfigObj.getCategory();
        List<UserMessage> systemMessage = List.of(UserMessage.from(prompt));
        workflowUtil.streamingInvokeLLM(wfState, state, node, category, modelName, systemMessage);
        return new NodeProcessResult();
    }
}
