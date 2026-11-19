package org.ruoyi.workflow.workflow.node.knowledgeRetrieval;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.SpringUtil;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
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
     *   "embedding_model": "text-embedding-3-small",
     *   "return_source": true
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

        if ("graph".equalsIgnoreCase(config.getRetrievalMode())) {
            log.warn("Graph retrieval mode is not supported");
            throw new UnsupportedOperationException("GraphRAG retrieval is not supported");
        }

        String retrievalResult = retrieveFromVector(config, queryText);

        // 严格模式处理：检索无结果时返回默认回复
        if (StringUtils.isBlank(retrievalResult) && Boolean.TRUE.equals(config.getIsStrict())) {
            String defaultResp = config.getDefaultResponse();
            if (StringUtils.isNotBlank(defaultResp)) {
                log.info("Knowledge retrieval returned no results, using default response (strict mode)");
                retrievalResult = defaultResp;
            }
        }

        log.info("Retrieval result length: {}", retrievalResult.length());

        // 构建输出
        List<NodeIOData> outputs = new ArrayList<>();
        outputs.add(NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", retrievalResult));

        outputs.add(NodeIOData.createByText("query", "", queryText));

        return NodeProcessResult.builder().content(outputs).build();
    }

    /**
     * 从向量库检索，重排序设置继承自知识库
     */
    private String retrieveFromVector(KnowledgeRetrievalNodeConfig config, String query) {
        try {
            Long knowledgeId = Long.parseLong(config.getKnowledgeId());

            org.ruoyi.chat.service.knowledge.IKnowledgeInfoService knowledgeInfoService =
                SpringUtil.getBean(org.ruoyi.chat.service.knowledge.IKnowledgeInfoService.class);
            org.ruoyi.chat.domain.vo.knowledge.KnowledgeInfoVo kb = knowledgeInfoService.queryById(knowledgeId);
            if (kb == null) {
                log.error("Knowledge base not found: {}", knowledgeId);
                return "错误：知识库不存在, id=" + knowledgeId;
            }

            org.ruoyi.common.chat.service.chat.IChatModelService chatModelService =
                SpringUtil.getBean(org.ruoyi.common.chat.service.chat.IChatModelService.class);
            org.ruoyi.common.chat.domain.vo.chat.ChatModelVo embModel =
                chatModelService.selectModelByName(kb.getEmbeddingModel());
            if (embModel == null) {
                log.error("Embedding model not found: {}", kb.getEmbeddingModel());
                return "错误：知识库未配置有效的向量模型";
            }

            // 组装检索参数，优先使用节点的返回数量和相似度阈值
            org.ruoyi.chat.domain.bo.vector.QueryVectorBo bo = new org.ruoyi.chat.domain.bo.vector.QueryVectorBo();
            bo.setQuery(query);
            bo.setKid(String.valueOf(knowledgeId));
            bo.setMaxResults(config.getTopK() != null ? config.getTopK() : kb.getRetrieveLimit());
            bo.setSimilarityThreshold(config.getSimilarityThreshold() != null
                ? config.getSimilarityThreshold() : kb.getSimilarityThreshold());
            bo.setEmbeddingModelName(kb.getEmbeddingModel());
            bo.setVectorModelName(kb.getVectorModel());
            bo.setBaseUrl(embModel.getApiHost());

            bo.setEnableRerank(kb.getEnableRerank() != null && kb.getEnableRerank() == 1);
            bo.setRerankModelName(kb.getRerankModel());
            bo.setRerankTopN(kb.getRerankTopN());
            bo.setRerankScoreThreshold(kb.getRerankScoreThreshold());

            org.ruoyi.chat.service.retrieval.KnowledgeRetrievalService retrievalService =
                SpringUtil.getBean(org.ruoyi.chat.service.retrieval.KnowledgeRetrievalService.class);
            java.util.List<org.ruoyi.chat.domain.vo.knowledge.KnowledgeRetrievalVo> results = retrievalService.retrieve(bo);
            if (results == null || results.isEmpty()) {
                log.info("Knowledge retrieval returned no results, kid={}, query={}", knowledgeId, query);
                return "";
            }

            // 合并结果
            boolean returnSource = config.getReturnSource() == null || config.getReturnSource();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                org.ruoyi.chat.domain.vo.knowledge.KnowledgeRetrievalVo vo = results.get(i);
                sb.append(i + 1).append(". ").append(vo.getContent());
                if (returnSource && StringUtils.isNotBlank(vo.getSourceName())) {
                    sb.append("（来源: ").append(vo.getSourceName());
                    if (vo.getScore() != null) {
                        sb.append(String.format(", 相关度: %.3f", vo.getScore()));
                    }
                    sb.append("）");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (NumberFormatException e) {
            log.error("Invalid knowledge base ID format: {}", config.getKnowledgeId(), e);
            return "错误：知识库ID格式无效";
        } catch (Exception e) {
            log.error("Failed to retrieve from vector store", e);
            return "";
        }
    }

}
