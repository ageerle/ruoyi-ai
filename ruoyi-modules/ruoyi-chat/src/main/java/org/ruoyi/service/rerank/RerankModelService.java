package org.ruoyi.service.rerank;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;

import java.util.List;

/**
 * 重排序模型服务接口
 * 继承 langchain4j 的 ScoringModel 接口
 * 参考设计模式：BaseEmbedModelService
 *
 * @author Yzm
 * @date 2026-04-19
 */
public interface RerankModelService extends ScoringModel {

    /**
     * 根据配置信息配置重排序模型
     *
     * @param config 包含模型配置信息的 ChatModelVo 对象
     */
    void configure(ChatModelVo config);

    /**
     * 执行重排序（批量文档）
     * 这是业务层使用的便捷方法
     *
     * @param rerankRequest 重排序请求，包含查询文本和候选文档列表
     * @return 重排序结果，包含排序后的文档和相关性分数
     */
    RerankResult rerank(RerankRequest rerankRequest);

    /**
     * 实现 ScoringModel 接口的 scoreAll 方法
     * 将 ScoringModel 的调用转换为重排序调用
     */
    @Override
    default Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        // 将 TextSegment 转换为文档字符串列表
        List<String> documents = segments.stream()
                .map(TextSegment::text)
                .toList();

        RerankRequest request = RerankRequest.builder()
                .query(query)
                .documents(documents)
                .topN(documents.size())
                .returnDocuments(false)
                .build();

        RerankResult result = rerank(request);

        // 提取分数列表，按原始顺序排列
        List<Double> scores = new java.util.ArrayList<>(
                java.util.Collections.nCopies(documents.size(), 0.0));

        for (RerankResult.RerankDocument doc : result.getDocuments()) {
            if (doc.getIndex() != null && doc.getIndex() < documents.size()) {
                scores.set(doc.getIndex(), doc.getRelevanceScore());
            }
        }

        return Response.from(scores);
    }
}
