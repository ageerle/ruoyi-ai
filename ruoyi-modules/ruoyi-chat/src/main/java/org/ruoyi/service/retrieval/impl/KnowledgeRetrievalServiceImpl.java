package org.ruoyi.service.retrieval.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.factory.RerankModelFactory;
import org.ruoyi.service.rerank.RerankModelService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库检索服务实现
 * 整合粗召回（向量检索）和重排序流程
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

    @Override
    public List<String> retrieveTexts(QueryVectorBo queryVectorBo) {
        log.info("开始知识库检索, kid={}, query={}", queryVectorBo.getKid(), queryVectorBo.getQuery());

        // 1. 粗召回阶段 - 向量检索
        List<String> coarseResults = coarseRetrieval(queryVectorBo);
        log.debug("粗召回返回 {} 条结果", coarseResults.size());

        if (coarseResults.isEmpty()) {
            return coarseResults;
        }

        // 2. 重排序阶段（可选）
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) &&
                queryVectorBo.getRerankModelName() != null) {
            return rerank(queryVectorBo, coarseResults);
        }

        return coarseResults;
    }

    /**
     * 粗召回阶段 - 向量检索
     */
    private List<String> coarseRetrieval(QueryVectorBo queryVectorBo) {
        // 如果启用重排序，扩大粗召回数量
        int originalMaxResults = queryVectorBo.getMaxResults();
        int expandedResults = originalMaxResults;
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank()) &&
                queryVectorBo.getRerankModelName() != null) {
            expandedResults = originalMaxResults * RERANK_EXPANSION_FACTOR;
            log.debug("启用重排序，粗召回数量从 {} 扩大到 {}", originalMaxResults, expandedResults);
        }

        // 临时修改查询数量
        queryVectorBo.setMaxResults(expandedResults);
        try {
            return vectorStoreService.getQueryVector(queryVectorBo);
        } finally {
            // 恢复原始值
            queryVectorBo.setMaxResults(originalMaxResults);
        }
    }

    /**
     * 重排序阶段
     */
    private List<String> rerank(QueryVectorBo queryVectorBo, List<String> coarseResults) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 通过工厂获取重排序模型
            RerankModelService rerankModel = rerankModelFactory.createModel(queryVectorBo.getRerankModelName());

            // 2. 构建重排序请求
            int topN = queryVectorBo.getRerankTopN() != null ?
                    queryVectorBo.getRerankTopN() : queryVectorBo.getMaxResults();

            RerankRequest rerankRequest = RerankRequest.builder()
                    .query(queryVectorBo.getQuery())
                    .documents(coarseResults)
                    .topN(topN)
                    .returnDocuments(true)
                    .build();

            log.info("执行重排序, model={}, documents={}, topN={}",
                    queryVectorBo.getRerankModelName(), coarseResults.size(), topN);

            // 3. 执行重排序
            RerankResult rerankResult = rerankModel.rerank(rerankRequest);

            // 4. 转换重排序结果
            List<String> finalResults = new ArrayList<>();
            for (RerankResult.RerankDocument doc : rerankResult.getDocuments()) {
                // 应用分数阈值过滤
                if (queryVectorBo.getRerankScoreThreshold() != null &&
                        doc.getRelevanceScore() < queryVectorBo.getRerankScoreThreshold()) {
                    continue;
                }

                if (doc.getDocument() != null) {
                    finalResults.add(doc.getDocument());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("重排序完成, 返回 {} 条结果, 耗时 {}ms", finalResults.size(), duration);

            return finalResults;

        } catch (Exception e) {
            log.error("重排序失败: {}", e.getMessage(), e);
            // 重排序失败时返回原始粗召回结果（截取到期望数量）
            int limit = Math.min(queryVectorBo.getMaxResults(), coarseResults.size());
            return new ArrayList<>(coarseResults.subList(0, limit));
        }
    }
}
