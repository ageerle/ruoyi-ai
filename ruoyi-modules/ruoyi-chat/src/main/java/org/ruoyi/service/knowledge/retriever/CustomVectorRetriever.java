package org.ruoyi.service.knowledge.retriever;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自定义检索器：适配 LangChain4j ContentRetriever 接口
 * 桥接统一的 KnowledgeRetrievalService，支持配置化的混合检索、阈值过滤等功能
 *
 * @author RobustH
 */
@Slf4j
@RequiredArgsConstructor
public class CustomVectorRetriever implements ContentRetriever {

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeInfoVo knowledgeInfoVo;
    private final ChatModelVo chatModelVo;

    @Override
    public List<Content> retrieve(Query query) {
        log.info("执行自定义检索，关键字: {}", query.text());

        // 构建增强后的查询参数
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(query.text());
        queryVectorBo.setKid(String.valueOf(knowledgeInfoVo.getId()));
        queryVectorBo.setApiKey(chatModelVo.getApiKey());
        queryVectorBo.setBaseUrl(chatModelVo.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        
        // 应用知识库配置参数
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());
        queryVectorBo.setSimilarityThreshold(knowledgeInfoVo.getSimilarityThreshold());
        queryVectorBo.setEnableHybrid(Objects.equals(knowledgeInfoVo.getEnableHybrid(), 1));
        queryVectorBo.setHybridAlpha(knowledgeInfoVo.getHybridAlpha());

        // 设置重排序参数 (如果 retriever 阶段也想做初步重排，可以在此设置)
        queryVectorBo.setEnableRerank(Objects.equals(knowledgeInfoVo.getEnableRerank(), 1));
        queryVectorBo.setRerankModelName(knowledgeInfoVo.getRerankModel());
        queryVectorBo.setRerankTopN(knowledgeInfoVo.getRerankTopN());
        queryVectorBo.setRerankScoreThreshold(knowledgeInfoVo.getRerankScoreThreshold());

        // 通过统一服务执行检索
        List<String> nearestList = knowledgeRetrievalService.retrieveTexts(queryVectorBo);

        // 将结果包装为标准的 Content 返回
        return nearestList.stream()
                .map(text -> Content.from(TextSegment.from(text)))
                .collect(Collectors.toList());
    }
}
