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
import org.ruoyi.service.vector.VectorStoreService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义向量检索器：适配 LangChain4j ContentRetriever 接口
 * 桥接现有的 VectorStoreService 获取检索结果
 *
 * @author RobustH
 */
@Slf4j
@RequiredArgsConstructor
public class CustomVectorRetriever implements ContentRetriever {

    private final VectorStoreService vectorStoreService;
    private final KnowledgeInfoVo knowledgeInfoVo;
    private final ChatModelVo chatModelVo;

    @Override
    public List<Content> retrieve(Query query) {
        log.info("执行自定义向量检索，关键字: {}", query.text());

        // 构建内部查询参数
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(query.text());
        queryVectorBo.setKid(String.valueOf(knowledgeInfoVo.getId()));
        queryVectorBo.setApiKey(chatModelVo.getApiKey());
        queryVectorBo.setBaseUrl(chatModelVo.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        // 如果接入了重排，这里的 retrieveLimit 也就是 MaxResults 应当被放大，后续留给 Aggregator 截断
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());

        // 执行底层的多种向量库策略检索
        List<String> nearestList = vectorStoreService.getQueryVector(queryVectorBo);

        // 将结果包装为标准的 Content 返回
        return nearestList.stream()
                .map(text -> Content.from(TextSegment.from(text)))
                .collect(Collectors.toList());
    }
}
