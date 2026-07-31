package org.ruoyi.trace;

import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagTracePayloadBuilderTest {

    @Test
    void chatRequestSummaryShouldHandleNullsAndAvoidPromptBody() {
        ChatRequest request = new ChatRequest();
        request.setSessionId(100L);
        request.setModel("qwen-plus");
        request.setKnowledgeId("200");
        request.setContent("secret prompt body");

        ChatModelVo model = new ChatModelVo();
        model.setProviderCode("dashscope");
        request.setChatModelVo(model);

        String payload = RagTracePayloadBuilder.chatRequestSummary(request);

        assertTrue(payload.contains("\"sessionId\":100"));
        assertTrue(payload.contains("\"contentLength\":18"));
        assertTrue(payload.contains("\"providerCode\":\"dashscope\""));
        assertFalse(payload.contains("secret prompt body"));
    }

    @Test
    void retrievalInputSummaryShouldUseSummaryOnly() {
        QueryVectorBo query = new QueryVectorBo();
        query.setKid("200");
        query.setQuery("private retrieval query");
        query.setMaxResults(5);
        query.setEnableRerank(true);
        query.setRerankModelName("gte-rerank");
        query.setRerankTopN(null);

        String payload = RagTracePayloadBuilder.retrievalInputSummary(query);

        assertTrue(payload.contains("\"kid\":\"200\""));
        assertTrue(payload.contains("\"queryLength\":23"));
        assertTrue(payload.contains("\"enableRerank\":true"));
        assertFalse(payload.contains("private retrieval query"));
    }

    @Test
    void retrievalOutputSummaryShouldAvoidFragmentContent() {
        KnowledgeRetrievalVo result = new KnowledgeRetrievalVo();
        result.setId("fragment-1");
        result.setDocId("doc-1");
        result.setIdx(1);
        result.setScore(0.85);
        result.setContent("sensitive knowledge fragment");

        String payload = RagTracePayloadBuilder.retrievalOutputSummary(List.of(result));

        assertTrue(payload.contains("\"resultCount\":1"));
        assertTrue(payload.contains("\"contentLength\":28"));
        assertTrue(payload.contains("\"fragment-1\""));
        assertFalse(payload.contains("sensitive knowledge fragment"));
    }

    @Test
    void summariesShouldAcceptNullValuesWithoutMapOfNpe() {
        assertTrue(RagTracePayloadBuilder.chatRequestSummary(null).contains("\"requestPresent\":false"));
        assertTrue(RagTracePayloadBuilder.retrievalInputSummary(null).contains("\"queryPresent\":false"));
        assertTrue(RagTracePayloadBuilder.retrievalOutputSummary(null).contains("\"resultCount\":0"));
        assertTrue(RagTracePayloadBuilder.rerankInputSummary(null, 0, null).contains("\"candidateCount\":0"));
    }
}
