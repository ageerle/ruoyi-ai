package org.ruoyi.workflow.workflow.node.googleSearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.util.SpringUtil;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.ruoyi.workflow.workflow.node.enmus.NodeMessageTemplateEnum;

import java.util.List;
import java.util.UUID;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

/**
 * 【扩展节点】网络搜索
 * 通过智谱 Web Search API 返回适合大模型消费的结构化网页结果。
 */
@Slf4j
public class GoogleSearchNode extends AbstractWfNode {

    public GoogleSearchNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    /**
     * 处理搜索请求
     * nodeConfig 格式：
     * {
     *   "query": "搜索关键词",
     *   "search_engine": "search_std",
     *   "result_count": 10,
     *   "search_domain_filter": "",
     *   "search_recency_filter": "noLimit",
     *   "content_size": "medium",
     *   "include_image": false
     * }
     *
     * @return 搜索结果
     */
    @Override
    public NodeProcessResult onProcess() {
        GoogleSearchNodeConfig config = checkAndGetConfig(GoogleSearchNodeConfig.class);

        // 获取搜索关键词
        String searchQuery = WorkflowUtil.renderTemplate(config.getQuery(), state.getInputs());
        if (StringUtils.isBlank(searchQuery)) {
            searchQuery = getFirstInputText();
        }

        if (StringUtils.isBlank(searchQuery)) {
            throw new IllegalArgumentException("未提供搜索关键词");
        }
        searchQuery = searchQuery.trim();
        if (searchQuery.length() > 70) {
            throw new IllegalArgumentException("搜索关键词不能超过 70 个字符");
        }

        log.info("Web search node processing, engine: {}, result_count: {}",
            config.getSearchEngine(), config.getResultCount());

        String nodeMessageTemplate = getNodeMessageTemplate(NodeMessageTemplateEnum.GOOGLE_SEARCH.getValue());
        notifyAndStoreMessage(wfState, nodeMessageTemplate);

        ZhipuWebSearchClient searchClient = SpringUtil.getBean(ZhipuWebSearchClient.class);
        ZhipuWebSearchClient.SearchResponse response = searchClient.search(
            searchQuery,
            config,
            UUID.randomUUID().toString()
        );
        String searchResult = JsonUtil.toJson(response);
        if (searchResult == null) {
            throw new IllegalStateException("搜索结果序列化失败");
        }

        log.info("Web search completed, result count: {}", response.count());
        notifyAndStoreMessage(wfState, nodeMessageTemplate + "返回 " + response.count() + " 条结果");

        List<NodeIOData> outputs = List.of(
            NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "智谱网络搜索结果", searchResult)
        );
        return NodeProcessResult.builder().content(outputs).build();
    }
}
