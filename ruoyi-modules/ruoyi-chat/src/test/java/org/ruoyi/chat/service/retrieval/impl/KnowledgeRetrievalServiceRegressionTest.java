package org.ruoyi.chat.service.retrieval.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.chat.domain.bo.rerank.RerankRequest;
import org.ruoyi.chat.domain.bo.rerank.RerankResult;
import org.ruoyi.chat.domain.bo.vector.QueryVectorBo;
import org.ruoyi.chat.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.chat.factory.RerankModelFactory;
import org.ruoyi.chat.service.rerank.RerankModelService;
import org.ruoyi.chat.service.vector.VectorStoreService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class KnowledgeRetrievalServiceRegressionTest {

    @Test
    void keepsVectorScoresWhenFilteringBySimilarity() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(any()))
            .thenReturn(List.of(item("fid-a", "A", 0.9), item("fid-b", "B", 0.3)));
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(vectorStore, mock(RerankModelFactory.class));
        QueryVectorBo query = new QueryVectorBo();
        query.setKid("1");
        query.setQuery("test");
        query.setMaxResults(5);
        query.setSimilarityThreshold(0.5);

        List<KnowledgeRetrievalVo> result = service.retrieve(query);

        assertEquals(List.of("fid-a"), result.stream().map(KnowledgeRetrievalVo::getId).toList());
        assertEquals(0.9, result.get(0).getScore(), 1e-9);
    }

    @Test
    void rerankThresholdDoesNotLeakIntoNonRerankRetrieval() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(any())).thenReturn(List.of(item("fid-a", "A", 0.75)));
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(vectorStore, mock(RerankModelFactory.class));
        QueryVectorBo query = new QueryVectorBo();
        query.setKid("1");
        query.setQuery("test");
        query.setMaxResults(5);
        query.setEnableRerank(false);
        query.setSimilarityThreshold(0.5);
        query.setRerankScoreThreshold(0.9);

        List<KnowledgeRetrievalVo> result = service.retrieve(query);

        assertEquals(1, result.size());
        assertEquals("fid-a", result.get(0).getId());
    }

    @Test
    void usesDistinctThresholdsForVectorAndRerankScores() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(any()))
            .thenReturn(List.of(item("fid-a", "A", 0.9), item("fid-b", "B", 0.3), item("fid-c", "C", 0.8)));
        RerankModelFactory factory = mock(RerankModelFactory.class);
        RerankModelService model = mock(RerankModelService.class);
        when(factory.createModel("rerank")).thenReturn(model);
        when(model.rerank(any())).thenAnswer(invocation -> {
            RerankRequest request = invocation.getArgument(0);
            assertEquals(List.of("A", "C"), request.getDocuments());
            return RerankResult.builder().documents(List.of(
                RerankResult.RerankDocument.builder().index(0).relevanceScore(0.02).build(),
                RerankResult.RerankDocument.builder().index(1).relevanceScore(0.2).build()
            )).build();
        });
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(vectorStore, factory);
        QueryVectorBo query = rerankQuery(0.1);

        List<KnowledgeRetrievalVo> result = service.retrieve(query);

        assertEquals(List.of("fid-c"), result.stream().map(KnowledgeRetrievalVo::getId).toList());
        assertEquals(0.2, result.get(0).getScore(), 1e-9);
        assertEquals(0.8, result.get(0).getRawScore(), 1e-9);
    }

    @Test
    void keepsVectorResultsWhenRerankFails() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.search(any())).thenReturn(List.of(item("fid-a", "A", 0.75)));
        RerankModelFactory factory = mock(RerankModelFactory.class);
        when(factory.createModel("rerank")).thenThrow(new IllegalStateException("rerank unavailable"));
        KnowledgeRetrievalServiceImpl service = new KnowledgeRetrievalServiceImpl(vectorStore, factory);

        List<KnowledgeRetrievalVo> result = service.retrieve(rerankQuery(0.9));

        assertEquals(List.of("fid-a"), result.stream().map(KnowledgeRetrievalVo::getId).toList());
        assertEquals(0.75, result.get(0).getScore(), 1e-9);
    }

    private static QueryVectorBo rerankQuery(double rerankThreshold) {
        QueryVectorBo query = new QueryVectorBo();
        query.setKid("1");
        query.setQuery("test");
        query.setMaxResults(5);
        query.setSimilarityThreshold(0.5);
        query.setEnableRerank(true);
        query.setRerankModelName("rerank");
        query.setRerankScoreThreshold(rerankThreshold);
        return query;
    }

    private static KnowledgeRetrievalVo item(String id, String content, double score) {
        return KnowledgeRetrievalVo.builder().id(id).content(content).score(score).build();
    }
}
