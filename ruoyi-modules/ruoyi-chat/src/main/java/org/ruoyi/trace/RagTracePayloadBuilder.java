package org.ruoyi.trace;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.trace.util.TracePayloadUtils;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG trace payload 摘要构建器。
 */
public final class RagTracePayloadBuilder {

    private static final int MAX_RESULT_SUMMARY_SIZE = 5;

    private RagTracePayloadBuilder() {
    }

    public static String chatRequestSummary(ChatRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestPresent", request != null);
        if (request == null) {
            return TracePayloadUtils.toJson(payload);
        }

        ChatModelVo model = request.getChatModelVo();
        payload.put("sessionId", request.getSessionId() == null ? null : request.getSessionId().toString());
        payload.put("model", request.getModel());
        payload.put("providerCode", model == null ? null : model.getProviderCode());
        payload.put("knowledgeId", request.getKnowledgeId());
        payload.put("hasKnowledge", request.getKnowledgeId() != null);
        payload.put("contentLength", length(request.getContent()));
        payload.put("contextMessageCount", request.getContextMessages() == null ? null : request.getContextMessages().size());
        payload.put("enableWorkFlow", request.getEnableWorkFlow());
        payload.put("enableThinking", request.getEnableThinking());
        return TracePayloadUtils.toJson(payload);
    }

    public static String retrievalInputSummary(QueryVectorBo query) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("queryPresent", query != null);
        if (query == null) {
            return TracePayloadUtils.toJson(payload);
        }

        payload.put("kid", query.getKid());
        payload.put("queryLength", length(query.getQuery()));
        payload.put("maxResults", query.getMaxResults());
        payload.put("vectorModelName", query.getVectorModelName());
        payload.put("embeddingModelName", query.getEmbeddingModelName());
        payload.put("enableHybrid", query.getEnableHybrid());
        payload.put("hybridAlpha", query.getHybridAlpha());
        payload.put("similarityThreshold", query.getSimilarityThreshold());
        payload.put("enableRerank", query.getEnableRerank());
        payload.put("rerankModel", query.getRerankModelName());
        payload.put("rerankTopN", query.getRerankTopN());
        payload.put("rerankScoreThreshold", query.getRerankScoreThreshold());
        return TracePayloadUtils.toJson(payload);
    }

    public static String retrievalOutputSummary(List<KnowledgeRetrievalVo> results) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resultCount", results == null ? 0 : results.size());
        payload.put("results", summarizeResults(results));
        return TracePayloadUtils.toJson(payload);
    }

    public static String rerankInputSummary(QueryVectorBo query, int candidateCount, Integer topN) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateCount", candidateCount);
        payload.put("rerankModel", query == null ? null : query.getRerankModelName());
        payload.put("topN", topN);
        return TracePayloadUtils.toJson(payload);
    }

    public static String rerankOutputSummary(List<KnowledgeRetrievalVo> results) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resultCount", results == null ? 0 : results.size());
        payload.put("results", summarizeResults(results));
        return TracePayloadUtils.toJson(payload);
    }

    public static String streamInputSummary(ChatRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", request == null || request.getSessionId() == null ? null : request.getSessionId().toString());
        payload.put("model", request == null ? null : request.getModel());
        payload.put("contextMessageCount", request == null || request.getContextMessages() == null ? null : request.getContextMessages().size());
        payload.put("contentLength", request == null ? null : length(request.getContent()));
        return TracePayloadUtils.toJson(payload);
    }

    public static String streamOutputSummary(int responseLength) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("responseLength", responseLength);
        return TracePayloadUtils.toJson(payload);
    }

    private static List<Map<String, Object>> summarizeResults(List<KnowledgeRetrievalVo> results) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return summaries;
        }

        int limit = Math.min(MAX_RESULT_SUMMARY_SIZE, results.size());
        for (int i = 0; i < limit; i++) {
            KnowledgeRetrievalVo result = results.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", i + 1);
            item.put("id", result == null ? null : result.getId());
            item.put("docId", result == null ? null : result.getDocId());
            item.put("knowledgeId", result == null || result.getKnowledgeId() == null ? null : result.getKnowledgeId().toString());
            item.put("idx", result == null ? null : result.getIdx());
            item.put("score", result == null ? null : result.getScore());
            item.put("rawScore", result == null ? null : result.getRawScore());
            item.put("originalIndex", result == null ? null : result.getOriginalIndex());
            item.put("sourceName", result == null ? null : result.getSourceName());
            item.put("contentLength", result == null ? null : length(result.getContent()));
            summaries.add(item);
        }
        return summaries;
    }

    private static Integer length(String value) {
        return value == null ? null : value.length();
    }
}
