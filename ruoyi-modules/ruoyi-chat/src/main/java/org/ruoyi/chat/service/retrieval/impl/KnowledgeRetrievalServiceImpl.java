package org.ruoyi.chat.service.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.chat.domain.bo.rerank.RerankRequest;
import org.ruoyi.chat.domain.bo.rerank.RerankResult;
import org.ruoyi.chat.domain.bo.vector.QueryVectorBo;
import org.ruoyi.chat.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.chat.factory.RerankModelFactory;
import org.ruoyi.chat.service.rerank.RerankModelService;
import org.ruoyi.chat.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.chat.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现
 * 使用向量检索召回片段，并按配置重排序
 *
 * @author yang
 * @date 2026-04-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final VectorStoreService vectorStoreService;
    private final RerankModelFactory rerankModelFactory;

    /**
     * 粗召回默认扩大倍数
     * 如果启用重排序，粗召回会获取更多结果供重排序筛选
     */
    private static final int RERANK_EXPANSION_FACTOR = 3;
    private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int CACHE_MAX_ENTRIES = 1000;
    private final Map<String, CacheEntry> retrievalCache = new ConcurrentHashMap<>();

    @Override
    public List<String> retrieveTexts(QueryVectorBo queryVectorBo) {
        List<KnowledgeRetrievalVo> results = retrieve(queryVectorBo);
        return results.stream()
                .map(KnowledgeRetrievalVo::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeRetrievalVo> retrieve(QueryVectorBo queryVectorBo) {
        String cacheKey = cacheKey(queryVectorBo);
        CacheEntry cached = retrievalCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.createdAt < CACHE_TTL_MILLIS) {
            return copyResults(cached.results);
        }
        log.info("开始知识库检索, kid={}, query={}", queryVectorBo.getKid(), queryVectorBo.getQuery());

        List<KnowledgeRetrievalVo> finalResults = retrieveUncached(queryVectorBo);
        cache(cacheKey, finalResults);
        return copyResults(finalResults);
    }

    private List<KnowledgeRetrievalVo> retrieveUncached(QueryVectorBo queryVectorBo) {

        // 1. 向量召回
        List<KnowledgeRetrievalVo> coarseResults = performCoarseRetrieval(queryVectorBo);
        log.debug("粗召回返回 {} 条结果", coarseResults.size());

        if (coarseResults.isEmpty()) {
            return coarseResults;
        }

        // 2. 初始化原始索引
        for (int i = 0; i < coarseResults.size(); i++) {
            coarseResults.get(i).setOriginalIndex(i);
        }

        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) &&
                StringUtils.isNotBlank(queryVectorBo.getRerankModelName())) {
            return performRerank(queryVectorBo, coarseResults);
        }
        return coarseResults;
    }

    /**
     * 向量召回，按相似度阈值过滤片段
     */
    private List<KnowledgeRetrievalVo> performCoarseRetrieval(QueryVectorBo queryVectorBo) {
        // 如果启用重排序，适当扩大召回数量
        int originalMaxResults = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
        int targetMaxResults = originalMaxResults;
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) &&
            StringUtils.isNotBlank(queryVectorBo.getRerankModelName())) {
            targetMaxResults = originalMaxResults * RERANK_EXPANSION_FACTOR;
        }

        QueryVectorBo vectorQuery = copyOf(queryVectorBo, targetMaxResults);
        List<KnowledgeRetrievalVo> results = vectorStoreService.search(vectorQuery);
        Double threshold = queryVectorBo.getSimilarityThreshold();
        if (threshold == null) {
            return results;
        }
        return results.stream()
                .filter(result -> result.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 重排序阶段
     */
    private List<KnowledgeRetrievalVo> performRerank(QueryVectorBo queryVectorBo, List<KnowledgeRetrievalVo> coarseResults) {
        int topN = queryVectorBo.getRerankTopN() != null ? queryVectorBo.getRerankTopN() : queryVectorBo.getMaxResults();
        try {
            RerankModelService rerankModel = rerankModelFactory.createModel(queryVectorBo.getRerankModelName());

            List<String> contents = coarseResults.stream()
                    .map(KnowledgeRetrievalVo::getContent)
                    .collect(Collectors.toList());

            // topN 默认为 maxResults
            RerankRequest rerankRequest = RerankRequest.builder()
                    .query(queryVectorBo.getQuery())
                    .documents(contents)
                    .topN(topN)
                    .build();

            RerankResult rerankResult = rerankModel.rerank(rerankRequest);

            Double threshold = queryVectorBo.getRerankScoreThreshold();
            List<KnowledgeRetrievalVo> reranked = new ArrayList<>();
            for (RerankResult.RerankDocument doc : rerankResult.getDocuments()) {
                Integer index = doc.getIndex();
                Double relevance = doc.getRelevanceScore();
                if (index == null || index < 0 || index >= coarseResults.size()
                    || relevance == null || (threshold != null && relevance < threshold)) {
                    continue;
                }

                KnowledgeRetrievalVo vo = coarseResults.get(index);
                vo.setRawScore(vo.getScore());
                vo.setScore(relevance);
                reranked.add(vo);
            }

            // 按新分排序
            reranked.sort((a, b) -> b.getScore().compareTo(a.getScore()));

            // 截断到 topN
            List<KnowledgeRetrievalVo> results = reranked.subList(0, Math.min(topN, reranked.size()));
            return results;

        } catch (Exception e) {
            log.error("重排序流程失败: {}", e.getMessage());
            int limit = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
            List<KnowledgeRetrievalVo> fallback = coarseResults.subList(0, Math.min(limit, coarseResults.size()));
            return fallback;
        }
    }

    private QueryVectorBo copyOf(QueryVectorBo original, int maxResults) {
        QueryVectorBo copy = new QueryVectorBo();
        copy.setQuery(original.getQuery());
        copy.setKid(original.getKid());
        copy.setMaxResults(maxResults);
        copy.setVectorModelName(original.getVectorModelName());
        copy.setEmbeddingModelName(original.getEmbeddingModelName());
        copy.setBaseUrl(original.getBaseUrl());
        return copy;
    }

    @Override
    public void invalidateKnowledge(String kid) {
        if (StringUtils.isBlank(kid)) {
            retrievalCache.clear();
        } else {
            retrievalCache.keySet().removeIf(key -> key.startsWith(kid + "|"));
        }
    }

    private void cache(String key, List<KnowledgeRetrievalVo> results) {
        if (retrievalCache.size() >= CACHE_MAX_ENTRIES) {
            long now = System.currentTimeMillis();
            retrievalCache.entrySet().removeIf(e -> now - e.getValue().createdAt >= CACHE_TTL_MILLIS);
            if (retrievalCache.size() >= CACHE_MAX_ENTRIES) {
                retrievalCache.clear();
            }
        }
        retrievalCache.put(key, new CacheEntry(System.currentTimeMillis(), copyResults(results)));
    }

    private String cacheKey(QueryVectorBo bo) {
        return String.join("|", Objects.toString(bo.getKid(), ""), Objects.toString(bo.getQuery(), ""),
                Objects.toString(bo.getMaxResults(), ""), Objects.toString(bo.getVectorModelName(), ""),
                Objects.toString(bo.getEmbeddingModelName(), ""), Objects.toString(bo.getSimilarityThreshold(), ""),
                Objects.toString(bo.getEnableRerank(), ""), Objects.toString(bo.getRerankModelName(), ""),
                Objects.toString(bo.getRerankTopN(), ""), Objects.toString(bo.getRerankScoreThreshold(), ""));
    }

    private List<KnowledgeRetrievalVo> copyResults(List<KnowledgeRetrievalVo> source) {
        return source.stream().map(vo -> KnowledgeRetrievalVo.builder()
                .id(vo.getId()).docId(vo.getDocId()).knowledgeId(vo.getKnowledgeId()).idx(vo.getIdx())
                .content(vo.getContent()).score(vo.getScore()).originalIndex(vo.getOriginalIndex())
                .rawScore(vo.getRawScore()).sourceName(vo.getSourceName()).build()).collect(Collectors.toList());
    }

    private record CacheEntry(long createdAt, List<KnowledgeRetrievalVo> results) { }
}
