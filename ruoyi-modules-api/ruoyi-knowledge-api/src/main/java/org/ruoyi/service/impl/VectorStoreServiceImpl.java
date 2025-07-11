package org.ruoyi.service.impl;

import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量库管理
 *
 * @author ageer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final ConfigService configService;

    private EmbeddingStore<TextSegment> embeddingStore;


    @Override
    public void createSchema(String kid, String modelName) {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname");
        embeddingStore = WeaviateEmbeddingStore.builder()
                .scheme(protocol)
                .host(host)
                .objectClass(className+kid)
                .scheme(protocol)
                .avoidDups(true)
                .consistencyLevel("ALL")
                .build();
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo bo) {
        createSchema(bo.getKid(), bo.getVectorModelName());

        EmbeddingConfig config = new EmbeddingConfig(bo.getEmbeddingModelName(), bo.getApiKey(), bo.getBaseUrl());
        EmbeddingModel embeddingModel = getEmbeddingModelFromConfig(config, false);

        for (String chunk : bo.getChunkList()) {
            Embedding embedding = safeEmbed(embeddingModel, chunk, config);
            embeddingStore.add(embedding, TextSegment.from(chunk));
        }
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo bo) {
        createSchema(bo.getKid(), bo.getVectorModelName());

        EmbeddingConfig config = new EmbeddingConfig(bo.getEmbeddingModelName(), bo.getApiKey(), bo.getBaseUrl());
        EmbeddingModel embeddingModel = getEmbeddingModelFromConfig(config, false);
        Embedding queryEmbedding = safeEmbed(embeddingModel, bo.getQuery(), config);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(bo.getMaxResults())
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName)  {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname");
        String finalClassName = className + id;
        WeaviateClient client = new WeaviateClient(new Config(protocol, host));
        Result<Boolean> result = client.schema().classDeleter().withClassName(finalClassName).run();
        if (result.hasErrors()) {
            log.error("失败删除向量: " + result.getError());
            throw new ServiceException("失败删除向量数据!");
        } else {
            log.info("成功删除向量数据: " + result.getResult());
        }
    }

    private Embedding safeEmbed(EmbeddingModel model, String input, EmbeddingConfig config) {
        try {
            return model.embed(input).content();
        } catch (Exception e) {
            log.warn("Embedding 请求失败，尝试降级为 HTTP/1.1，modelName: {}", config.modelName());
            EmbeddingModel fallbackModel = getEmbeddingModelFromConfig(config, true);
            return fallbackModel.embed(input).content();
        }
    }

    private EmbeddingModel getEmbeddingModelFromConfig(EmbeddingConfig config, boolean forceHttp1) {
        return getEmbeddingModel(config.modelName(), config.apiKey(), config.baseUrl(), forceHttp1);
    }

    @SneakyThrows
    public EmbeddingModel getEmbeddingModel(String modelName, String apiKey, String baseUrl, boolean forceHttp1) {
        JdkHttpClientBuilder jdkHttpClientBuilder = null;

        if (forceHttp1) {
            java.net.http.HttpClient.Builder httpClientBuilder = java.net.http.HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1);
            jdkHttpClientBuilder = new JdkHttpClientBuilder().httpClientBuilder(httpClientBuilder);
        }

        if ("quentinz/bge-large-zh-v1.5".equals(modelName)) {
            var builder = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName);
            if (jdkHttpClientBuilder != null) builder.httpClientBuilder(jdkHttpClientBuilder);
            return builder.build();
        } else {
            var builder = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName);
            if (jdkHttpClientBuilder != null) builder.httpClientBuilder(jdkHttpClientBuilder);
            return builder.build();
        }
    }

    /**
     * 封装 embedding 模型配置
     */
    private record EmbeddingConfig(String modelName, String apiKey, String baseUrl) {}

}
