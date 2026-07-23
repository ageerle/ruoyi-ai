package org.ruoyi.service.retrieval.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.RerankModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.vector.VectorStoreService;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class KnowledgeRetrievalServiceRegressionTest {

    @Test
    void rrfMergesSameStableIdAndKeepsDistinctItems() throws Exception {
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(
            mock(VectorStoreService.class), mock(RerankModelFactory.class), mock(KnowledgeFragmentMapper.class));
        KnowledgeRetrievalVo vectorA = item("fid-a", "A", 0.9);
        KnowledgeRetrievalVo vectorB = item("fid-b", "B", 0.8);
        KnowledgeRetrievalVo keywordA = item("fid-a", "A", 10.0);
        KnowledgeRetrievalVo keywordC = item("fid-c", "C", 10.0);

        Method method = KnowledgeRetrievalServiceImpl.class.getDeclaredMethod(
            "calculateRRF", List.class, List.class, double.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<KnowledgeRetrievalVo> result = (List<KnowledgeRetrievalVo>) method.invoke(
            service, List.of(vectorA, vectorB), List.of(keywordA, keywordC), 0.5);

        assertEquals(3, result.size());
        assertEquals("fid-a", result.get(0).getId(), "an item present in both rankings must win");
        assertEquals(3, result.stream().map(KnowledgeRetrievalVo::getId).distinct().count());
    }

    @Test
    void rerankThresholdDoesNotLeakIntoNonRerankRetrieval() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(item("fid-a", "A", 0.75)));
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(
            vectorStore, mock(RerankModelFactory.class), mock(KnowledgeFragmentMapper.class));
        QueryVectorBo query = new QueryVectorBo();
        query.setKid("1");
        query.setQuery("test");
        query.setMaxResults(5);
        query.setEnableHybrid(false);
        query.setEnableRerank(false);
        query.setSimilarityThreshold(0.5);
        query.setRerankScoreThreshold(0.9);

        List<KnowledgeRetrievalVo> result = service.retrieve(query);

        assertEquals(1, result.size());
        assertEquals("fid-a", result.get(0).getId());
    }

    private static KnowledgeRetrievalVo item(String id, String content, double score) {
        return KnowledgeRetrievalVo.builder().id(id).content(content).score(score).build();
    }
}
