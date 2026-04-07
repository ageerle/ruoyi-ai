package org.ruoyi.observability;

import dev.langchain4j.Experimental;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelRequestContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 自定义的 EmbeddingModelListener 的监听器。
 * 它监听 EmbeddingModel 的请求、响应和错误事件。
 *
 * @author evo
 */
@Slf4j
@Experimental
public class MyEmbeddingModelListener implements EmbeddingModelListener {

    @Override
    public void onRequest(EmbeddingModelRequestContext requestContext) {
        log.info("【EmbeddingModel请求】输入文本段落数量: {}", requestContext.textSegments().size());
        log.info("【EmbeddingModel请求】嵌入模型: {}", requestContext.embeddingModel());
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext responseContext) {
        Response<List<Embedding>> response = responseContext.response();
        List<Embedding> embeddings = response.content();
        log.info("【EmbeddingModel响应】嵌入向量数量: {}", embeddings.size());
        log.info("【EmbeddingModel响应】嵌入维度: {}", embeddings.isEmpty() ? 0 : embeddings.get(0).dimension());
        log.info("【EmbeddingModel响应】嵌入模型: {}", responseContext.embeddingModel());
        log.info("【EmbeddingModel响应】输入文本段落: {}", responseContext.textSegments());
    }

    @Override
    public void onError(EmbeddingModelErrorContext errorContext) {
        log.error("【EmbeddingModel错误】错误类型: {}", errorContext.error().getClass().getName());
        log.error("【EmbeddingModel错误】错误信息: {}", errorContext.error().getMessage());
        log.error("【EmbeddingModel错误】输入文本段落数量: {}", errorContext.textSegments().size());
        log.error("【EmbeddingModel错误】嵌入模型: {}", errorContext.embeddingModel());
    }
}
