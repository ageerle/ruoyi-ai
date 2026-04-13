package org.ruoyi.service.knowledge.rerank;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;

/**
 * DashScope 重排模型实现 (GTE-Rerank)
 * 包装了阿里云 DashScope 的 TextReRank API，使其符合 LangChain4j 的 ScoringModel 标准。
 */
@Slf4j
public class DashScopeScoringModel implements ScoringModel {

    private final String apiKey;
    private final String modelName;
    private final TextReRank rerank;

    @Builder
    public DashScopeScoringModel(String apiKey, String modelName) {
        if (isNullOrEmpty(apiKey)) {
            throw new IllegalArgumentException("DashScope API Key 不能为空");
        }
        this.apiKey = apiKey;
        this.modelName = isNullOrEmpty(modelName) ? "gte-rerank" : modelName;
        this.rerank = new TextReRank();
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        if (isNullOrEmpty(segments)) {
            return Response.from(new ArrayList<>());
        }

        // 提取文本列表供阿里 SDK 使用
        List<String> texts = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());

        try {
            TextReRankParam param = TextReRankParam.builder()
                    .apiKey(apiKey)
                    .model(modelName)
                    .query(query)
                    .documents(texts)
                    .topN(texts.size())
                    .returnDocuments(false)
                    .build();

            TextReRankResult result = rerank.call(param);

            // 初始化分数组，默认值为 0.0
            Double[] scores = new Double[texts.size()];
            for (int i = 0; i < texts.size(); i++) {
                scores[i] = 0.0;
            }

            // 根据返回结果填充对应的分数值（返回结果中包含原文索引）
            result.getOutput().getResults().forEach(item -> {
                if (item.getIndex() != null && item.getIndex() < texts.size()) {
                    scores[item.getIndex()] = item.getRelevanceScore();
                }
            });

            List<Double> scoreList = new ArrayList<>();
            for (Double s : scores) {
                scoreList.add(s);
            }

            return Response.from(scoreList);

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("DashScope 重排处理出错: {}", e.getMessage(), e);
            throw new RuntimeException("调用 DashScope 重排服务失败", e);
        }
    }

    @Override
    public Response<Double> score(TextSegment segment, String query) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(segment);
        Response<List<Double>> response = scoreAll(segments, query);
        return Response.from(response.content().get(0), response.tokenUsage(), response.finishReason());
    }
}
