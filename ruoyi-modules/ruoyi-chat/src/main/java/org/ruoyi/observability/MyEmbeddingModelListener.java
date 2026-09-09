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
        log.info("embedding_model_requested status=STARTED segmentCount={} modelType={}",
            requestContext.textSegments() == null ? 0 : requestContext.textSegments().size(),
            modelType(requestContext.embeddingModel()));
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext responseContext) {
        Response<List<Embedding>> response = responseContext.response();
        List<Embedding> embeddings = response.content();
        log.info("embedding_model_completed status=COMPLETED segmentCount={} embeddingCount={} "
                + "dimension={} modelType={}",
            responseContext.textSegments() == null ? 0 : responseContext.textSegments().size(),
            embeddings == null ? 0 : embeddings.size(),
            embeddings == null || embeddings.isEmpty() ? 0 : embeddings.get(0).dimension(),
            modelType(responseContext.embeddingModel()));
    }

    @Override
    public void onError(EmbeddingModelErrorContext errorContext) {
        log.error("embedding_model_failed status=FAILED segmentCount={} modelType={} errorType={}",
            errorContext.textSegments() == null ? 0 : errorContext.textSegments().size(),
            modelType(errorContext.embeddingModel()),
            errorContext.error() == null ? "unknown" : errorContext.error().getClass().getName());
    }

    private static String modelType(Object model) {
        return model == null ? "unknown" : model.getClass().getName();
    }
}
