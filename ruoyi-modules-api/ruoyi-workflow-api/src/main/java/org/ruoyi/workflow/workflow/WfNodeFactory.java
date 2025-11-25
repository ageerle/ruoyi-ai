package org.ruoyi.workflow.workflow;

import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.ruoyi.workflow.workflow.node.EndNode;
import org.ruoyi.workflow.workflow.node.answer.LLMAnswerNode;
import org.ruoyi.workflow.workflow.node.httpRequest.HttpRequestNode;
import org.ruoyi.workflow.workflow.node.keywordExtractor.KeywordExtractorNode;
import org.ruoyi.workflow.workflow.node.knowledgeRetrieval.KnowledgeRetrievalNode;
import org.ruoyi.workflow.workflow.node.mailSend.MailSendNode;
import org.ruoyi.workflow.workflow.node.start.StartNode;
import org.ruoyi.workflow.workflow.node.switcher.SwitcherNode;

public class WfNodeFactory {
    public static AbstractWfNode create(WorkflowComponent wfComponent, WorkflowNode nodeDefinition,
                                        WfState wfState, WfNodeState nodeState) {
        AbstractWfNode wfNode = null;
        switch (WfComponentNameEnum.getByName(wfComponent.getName())) {
            case START -> wfNode = new StartNode(wfComponent, nodeDefinition, wfState, nodeState);
            case LLM_ANSWER -> wfNode = new LLMAnswerNode(wfComponent, nodeDefinition, wfState, nodeState);
            case KEYWORD_EXTRACTOR -> wfNode = new KeywordExtractorNode(wfComponent, nodeDefinition, wfState, nodeState);
            case KNOWLEDGE_RETRIEVER -> wfNode = new KnowledgeRetrievalNode(wfComponent, nodeDefinition, wfState, nodeState);
            case END -> wfNode = new EndNode(wfComponent, nodeDefinition, wfState, nodeState);
            case MAIL_SEND -> wfNode = new MailSendNode(wfComponent, nodeDefinition, wfState, nodeState);
            case HTTP_REQUEST -> wfNode = new HttpRequestNode(wfComponent, nodeDefinition, wfState, nodeState);
            case SWITCHER -> wfNode = new SwitcherNode(wfComponent, nodeDefinition, wfState, nodeState);
            default -> {
            }
        }
        return wfNode;
    }
}
