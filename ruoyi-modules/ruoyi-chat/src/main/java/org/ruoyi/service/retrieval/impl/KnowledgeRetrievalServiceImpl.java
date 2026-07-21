package org.ruoyi.service.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.RerankModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.rerank.RerankModelService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现
 * 整合粗召回（向量检索/关键词检索）、RRF融合和重排序流程
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
    private final KnowledgeFragmentMapper fragmentMapper;

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

        // 1. 粗召回阶段 (向量检索 + 关键词搜索)
        List<KnowledgeRetrievalVo> coarseResults = performCoarseRetrieval(queryVectorBo);
        log.debug("粗召回返回 {} 条结果", coarseResults.size());

        if (coarseResults.isEmpty()) {
            return coarseResults;
        }

        // 2. 初始化原始索引
        for (int i = 0; i < coarseResults.size(); i++) {
            coarseResults.get(i).setOriginalIndex(i);
        }

        // 3. 重排序阶段 (可选)
        List<KnowledgeRetrievalVo> finalResults = coarseResults;
        boolean rerankApplied = Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) &&
                StringUtils.isNotBlank(queryVectorBo.getRerankModelName());
        if (rerankApplied) {
            finalResults = performRerank(queryVectorBo, coarseResults);
        }

        // 4. 应用分值阈值过滤 (重排分值或 RRF 分值)
        if (rerankApplied && queryVectorBo.getRerankScoreThreshold() != null) {
            double threshold = queryVectorBo.getRerankScoreThreshold();
            finalResults = finalResults.stream()
                    .filter(res -> res.getScore() != null && res.getScore() >= threshold)
                    .collect(Collectors.toList());
        }
        cache(cacheKey, finalResults);
        return copyResults(finalResults);
    }

    /**
     * 粗召回阶段：根据配置执行向量搜索或混合搜索
     */
    private List<KnowledgeRetrievalVo> performCoarseRetrieval(QueryVectorBo queryVectorBo) {
        // 如果启用重排序，适当扩大召回数量
        int originalMaxResults = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
        int targetMaxResults = originalMaxResults;
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) && 
            StringUtils.isNotBlank(queryVectorBo.getRerankModelName())) {
            targetMaxResults = originalMaxResults * RERANK_EXPANSION_FACTOR;
        }

        // 如果未启用混合检索，直接走向量搜索
        if (!Boolean.TRUE.equals(queryVectorBo.getEnableHybrid())) {
            QueryVectorBo vectorQuery = copyOf(queryVectorBo, targetMaxResults);
            List<KnowledgeRetrievalVo> results = vectorStoreService.search(vectorQuery);
            
            // 应用基础相似度阈值过滤(如果有)
            if (queryVectorBo.getSimilarityThreshold() != null) {
                results = results.stream()
                        .filter(r -> r.getScore() >= queryVectorBo.getSimilarityThreshold())
                        .collect(Collectors.toList());
            }
            return results;
        }

        // 混合检索逻辑
        log.info("执行混合检索: kid={}, query={}", queryVectorBo.getKid(), queryVectorBo.getQuery());
        try {
            // A. 并行执行向量搜索
            int finalTargetMaxResults = targetMaxResults;
            CompletableFuture<List<KnowledgeRetrievalVo>> vectorFuture = CompletableFuture.supplyAsync(() -> {
                QueryVectorBo vectorQuery = copyOf(queryVectorBo, finalTargetMaxResults);
                List<KnowledgeRetrievalVo> results = vectorStoreService.search(vectorQuery);
                // 向量层初步过滤
                if (queryVectorBo.getSimilarityThreshold() != null) {
                    return results.stream()
                            .filter(r -> r.getScore() >= queryVectorBo.getSimilarityThreshold())
                            .collect(Collectors.toList());
                }
                return results;
            });

            // B. 并行执行关键词搜索 (MySQL Fulltext)
            CompletableFuture<List<KnowledgeRetrievalVo>> keywordFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    Long kid = Long.valueOf(queryVectorBo.getKid());
                    List<KnowledgeFragmentVo> fragments = fragmentMapper.searchByKeyword(kid, queryVectorBo.getQuery(), finalTargetMaxResults);
                    return fragments.stream().map(f -> {
                        KnowledgeRetrievalVo vo = new KnowledgeRetrievalVo();
                        // 优先使用 fid 作为融合标识（与向量侧一致），历史数据无 fid 时回退主键
                        vo.setId(StringUtils.isNotBlank(f.getFid()) ? f.getFid() : f.getId().toString());
                        vo.setContent(f.getContent());
                        vo.setDocId(f.getDocId());
                        vo.setIdx(f.getIdx());
                        vo.setKnowledgeId(f.getKnowledgeId());
                        vo.setScore(10.0); // RRF 初始占位分
                        return vo;
                    }).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("关键词检索失败: {}", e.getMessage());
                    return new ArrayList<>();
                }
            });

            List<KnowledgeRetrievalVo> vectorResults = vectorFuture.get();
            List<KnowledgeRetrievalVo> keywordResults = keywordFuture.get();

            // C. RRF 融合
            double alpha = queryVectorBo.getHybridAlpha() != null ? queryVectorBo.getHybridAlpha() : 0.5;
            return calculateRRF(vectorResults, keywordResults, alpha);

        } catch (Exception e) {
            log.error("混合检索执行失败，回退到纯向量检索: {}", e.getMessage(), e);
            try {
                return vectorStoreService.search(copyOf(queryVectorBo, targetMaxResults));
            } catch (Exception vectorError) {
                throw new ServiceException("知识库检索不可用：向量与混合检索均失败");
            }
        }
    }

    /**
     * 重排序阶段
     */
    private List<KnowledgeRetrievalVo> performRerank(QueryVectorBo queryVectorBo, List<KnowledgeRetrievalVo> coarseResults) {
        try {
            RerankModelService rerankModel = rerankModelFactory.createModel(queryVectorBo.getRerankModelName());
            
            List<String> contents = coarseResults.stream()
                    .map(KnowledgeRetrievalVo::getContent)
                    .collect(Collectors.toList());

            // topN 默认为 maxResults
            int topN = queryVectorBo.getRerankTopN() != null ? queryVectorBo.getRerankTopN() : queryVectorBo.getMaxResults();

            RerankRequest rerankRequest = RerankRequest.builder()
                    .query(queryVectorBo.getQuery())
                    .documents(contents)
                    .topN(topN)
                    .build();

            RerankResult rerankResult = rerankModel.rerank(rerankRequest);

            // 写回分数并记录原始分
            List<KnowledgeRetrievalVo> reranked = new ArrayList<>();
            for (RerankResult.RerankDocument doc : rerankResult.getDocuments()) {
                if (doc.getIndex() != null && doc.getIndex() < coarseResults.size()) {
                    KnowledgeRetrievalVo vo = coarseResults.get(doc.getIndex());
                    vo.setRawScore(vo.getScore());
                    vo.setScore(doc.getRelevanceScore());
                    reranked.add(vo);
                }
            }

            // 按新分排序
            reranked.sort((a, b) -> b.getScore().compareTo(a.getScore()));
            
            // 截断到 topN
            return reranked.subList(0, Math.min(topN, reranked.size()));

        } catch (Exception e) {
            log.error("重排序流程失败: {}", e.getMessage());
            int limit = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
            return coarseResults.subList(0, Math.min(limit, coarseResults.size()));
        }
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合计算
     */
    private List<KnowledgeRetrievalVo> calculateRRF(List<KnowledgeRetrievalVo> vectorList, List<KnowledgeRetrievalVo> keywordList, double alpha) {
        Map<String, KnowledgeRetrievalVo> allMap = new LinkedHashMap<>();
        Map<String, Double> vectorScores = new HashMap<>();
        Map<String, Double> keywordScores = new HashMap<>();

        int k = 60; // RRF 常数

        for (int i = 0; i < vectorList.size(); i++) {
            KnowledgeRetrievalVo vo = vectorList.get(i);
            allMap.put(vo.getId(), vo);
            vectorScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        for (int i = 0; i < keywordList.size(); i++) {
            KnowledgeRetrievalVo vo = keywordList.get(i);
            if (!allMap.containsKey(vo.getId())) {
                allMap.put(vo.getId(), vo);
            }
            keywordScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        List<KnowledgeRetrievalVo> fusedResults = new ArrayList<>();
        for (Map.Entry<String, KnowledgeRetrievalVo> entry : allMap.entrySet()) {
            String id = entry.getKey();
            double finalScore = (1 - alpha) * vectorScores.getOrDefault(id, 0.0) + 
                               alpha * keywordScores.getOrDefault(id, 0.0);
            
            KnowledgeRetrievalVo vo = entry.getValue();
            vo.setScore(finalScore * 60.0); // 归一化缩放
            fusedResults.add(vo);
        }

        fusedResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        return fusedResults;
    }

    private QueryVectorBo copyOf(QueryVectorBo original, int maxResults) {
        QueryVectorBo copy = new QueryVectorBo();
        copy.setQuery(original.getQuery());
        copy.setKid(original.getKid());
        copy.setMaxResults(maxResults);
        copy.setVectorModelName(original.getVectorModelName());
        copy.setEmbeddingModelName(original.getEmbeddingModelName());
        copy.setApiKey(original.getApiKey());
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
                Objects.toString(bo.getEnableHybrid(), ""), Objects.toString(bo.getHybridAlpha(), ""),
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
