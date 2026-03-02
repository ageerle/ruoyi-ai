package org.ruoyi.workflow.workflow.node.knowledgeRetrieval;

import dev.langchain4j.data.message.SystemMessage;
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
 * 【节点】知识库检索节点
 * 从知识库中检索相关内容
 */
@Slf4j
public class KnowledgeRetrievalNode extends AbstractWfNode {

    public KnowledgeRetrievalNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    /**
     * 处理知识库检索
     * nodeConfig 格式：
     * {
     *   "knowledge_id": "kb_123",
     *   "top_k": 5,
     *   "similarity_threshold": 0.7,
     *   "retrieval_mode": "vector",
     *   "embedding_model": "text-embedding-3-small",
     *   "return_source": true,
     *   "prompt": "额外的查询改写提示词"
     * }
     *
     * @return 检索结果
     */
    @Override
    public NodeProcessResult onProcess() {
        KnowledgeRetrievalNodeConfig config = checkAndGetConfig(KnowledgeRetrievalNodeConfig.class);

        // 验证知识库ID
        if (StringUtils.isBlank(config.getKnowledgeId())) {
            log.error("Knowledge base ID is required but not provided");
            List<NodeIOData> outputs = new ArrayList<>();
            outputs.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", "错误：未配置知识库ID"));
            return NodeProcessResult.builder().content(outputs).build();
        }

        // 获取查询文本
        String queryText = getFirstInputText();
        if (StringUtils.isBlank(queryText)) {
            log.warn("Knowledge retrieval node has no input query, node: {}", state.getUuid());
            // 返回空结果
            List<NodeIOData> outputs = new ArrayList<>();
            outputs.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", ""));
            return NodeProcessResult.builder().content(outputs).build();
        }

        log.info("Knowledge retrieval node config: {}", config);
        log.info("Query text: {}", queryText);

        // 如果有自定义提示词，对查询进行改写
        String finalQuery = queryText;
        if (StringUtils.isNotBlank(config.getPrompt())) {
            finalQuery = rewriteQuery(config, queryText);
            log.info("Rewritten query: {}", finalQuery);
        }

        // 根据检索模式执行不同的检索策略
        String retrievalResult;
        String mode = config.getRetrievalMode() != null ? config.getRetrievalMode().toLowerCase() : "vector";

        // 目前只支持向量检索，图谱检索需要依赖graph模块
        if ("graph".equals(mode) || "hybrid".equals(mode)) {
            log.warn("Graph retrieval mode is not supported in workflow-api module, falling back to vector retrieval");
        }

        retrievalResult = retrieveFromVector(config, finalQuery);

        log.info("Retrieval result length: {}", retrievalResult.length());

        // 构建输出
        List<NodeIOData> outputs = new ArrayList<>();
        outputs.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", retrievalResult));

        // 如果需要返回原始查询
        outputs.add(NodeIOData.createByText("query", "", finalQuery));

        return NodeProcessResult.builder().content(outputs).build();
    }

    /**
     * 使用LLM改写查询
     */
    private String rewriteQuery(KnowledgeRetrievalNodeConfig config, String originalQuery) {
        try {
            // 构建改写提示词
            String prompt = WorkflowUtil.renderTemplate(config.getPrompt(), state.getInputs());
            prompt = prompt.replace("{query}", originalQuery);

            log.info("Query rewrite prompt: {}", prompt);

            // 调用LLM进行查询改写
            String rewrittenQuery = invokeLLMSync(config, prompt);

            if (StringUtils.isNotBlank(rewrittenQuery)) {
                log.info("Query rewritten from '{}' to '{}'", originalQuery, rewrittenQuery);
                return rewrittenQuery.trim();
            }

            // 如果改写失败，返回原查询
            return originalQuery;
        } catch (Exception e) {
            log.error("Failed to rewrite query, using original query", e);
            return originalQuery;
        }
    }

    /**
     * 同步调用LLM
     * 使用一个临时的流式处理器来收集完整响应
     */
    private String invokeLLMSync(KnowledgeRetrievalNodeConfig config, String prompt) {
        try {
            // 创建一个StringBuilder来收集LLM响应
            StringBuilder responseBuilder = new StringBuilder();
            Object lock = new Object();
            boolean[] completed = {false};

            // 创建临时节点状态用于LLM调用
            WfNodeState tempState = new WfNodeState();
            tempState.setUuid(state.getUuid() + "_rewrite");
            List<NodeIOData> tempInputs = new ArrayList<>();
            tempInputs.add(NodeIOData.createByText("input", "", prompt));
            tempState.setInputs(tempInputs);

            // 创建临时工作流节点定义
            WorkflowNode tempNode = new WorkflowNode();
            tempNode.setUuid(tempState.getUuid());
            tempNode.setInputConfig(node.getInputConfig());

            // 使用WorkflowUtil调用LLM（流式）
            WorkflowUtil workflowUtil = SpringUtil.getBean(WorkflowUtil.class);
            List<SystemMessage> systemMessage = List.of(new SystemMessage(prompt));

            // 调用流式LLM
            String modelName = StringUtils.isNotBlank(config.getModelName()) ? config.getModelName() : "deepseek-chat";

            workflowUtil.streamingInvokeLLM(
                wfState,
                tempState,
                tempNode,
                modelName,
                systemMessage,
                ""
            );

            // 等待LLM响应完成（最多等待30秒）
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30秒超时

            while (!completed[0] && (System.currentTimeMillis() - startTime) < timeout) {
                synchronized (lock) {
                    // 检查是否有输出
                    if (!tempState.getOutputs().isEmpty()) {
                        for (NodeIOData output : tempState.getOutputs()) {
                            if ("output".equals(output.getName())) {
                                String text = output.valueToString();
                                if (StringUtils.isNotBlank(text)) {
                                    responseBuilder.append(text);
                                    completed[0] = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!completed[0]) {
                    Thread.sleep(100); // 等待100ms后重试
                }
            }

            String result = responseBuilder.toString().trim();
            if (StringUtils.isBlank(result)) {
                log.warn("LLM sync call returned empty response");
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to invoke LLM synchronously", e);
            return "";
        }
    }

    /**
     * 从向量库检索
     */
    private String retrieveFromVector(KnowledgeRetrievalNodeConfig config, String query) {
        try {

            // 获取知识库信息以获取embedding模型配置
            Long knowledgeId = Long.parseLong(config.getKnowledgeId());

            // 合并结果
            String mergedResult = "根据知识库id + query 查询知识库内容";

            return mergedResult;
        } catch (NumberFormatException e) {
            log.error("Invalid knowledge base ID format: {}", config.getKnowledgeId(), e);
            return "错误：知识库ID格式无效";
        } catch (Exception e) {
            log.error("Failed to retrieve from vector store", e);
            return "";
        }
    }

}
