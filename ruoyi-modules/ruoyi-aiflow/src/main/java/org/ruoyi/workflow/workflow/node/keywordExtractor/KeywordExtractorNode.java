package org.ruoyi.workflow.workflow.node.keywordExtractor;

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
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import java.util.ArrayList;
import java.util.List;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

/**
 * 【节点】关键词提取节点
 * 使用 LLM 从文本中提取关键词
 */
@Slf4j
public class KeywordExtractorNode extends AbstractWfNode {

    public KeywordExtractorNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    /**
     * 处理关键词提取
     * nodeConfig 格式：
     * {
     * "model_name": "deepseek-chat",
     * "category": "llm",
     * "top_n": 5,
     * "prompt": "额外的提示词"
     * }
     *
     * @return 提取的关键词列表
     */
    @Override
    public NodeProcessResult onProcess() {
        KeywordExtractorNodeConfig config = checkAndGetConfig(KeywordExtractorNodeConfig.class);

        // 获取输入文本
        String inputText = getFirstInputText();
        if (StringUtils.isBlank(inputText)) {
            log.warn("Keyword extractor node has no input text, node: {}", state.getUuid());
            // 返回空结果
            List<NodeIOData> outputs = new ArrayList<>();
            outputs.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", ""));
            return NodeProcessResult.builder().content(outputs).build();
        }

        log.info("Keyword extractor node config: {}", config);
        log.info("Input text length: {}", inputText.length());

        // 构建提示词
        String prompt = buildPrompt(config, inputText);
        log.info("Keyword extraction prompt: {}", prompt);

        // 调用 LLM 进行关键词提取
        WorkflowUtil workflowUtil = SpringUtil.getBean(WorkflowUtil.class);
        String modelName = config.getModelName();
        String category = config.getCategory();
        List<UserMessage> systemMessage = List.of(UserMessage.from(prompt));

        // 使用流式调用
        workflowUtil.streamingInvokeLLM(wfState, state, node, category, modelName, systemMessage);

        return new NodeProcessResult();
    }

    /**
     * 构建关键词提取的提示词
     */
    private String buildPrompt(KeywordExtractorNodeConfig config, String inputText) {
        StringBuilder promptBuilder = new StringBuilder();

        // 基础提示词
        promptBuilder.append("请从以下文本中提取 ").append(config.getTopN()).append(" 个最重要的关键词。\n\n");

        // 添加自定义提示词（如果有）
        if (StringUtils.isNotBlank(config.getPrompt())) {
            promptBuilder.append(config.getPrompt()).append("\n\n");
        }

        // 输出格式要求
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 只返回关键词，每个关键词用逗号分隔\n");
        promptBuilder.append("2. 关键词应该是名词或名词短语\n");
        promptBuilder.append("3. 按重要性从高到低排序\n");
        promptBuilder.append("4. 不要添加任何解释或额外的文字\n\n");

        // 原始文本
        promptBuilder.append("文本内容：\n");
        promptBuilder.append(inputText);

        return promptBuilder.toString();
    }
}
