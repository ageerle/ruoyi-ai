package org.ruoyi.embedding.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.embedding.MultiModalEmbedModelService;
import org.ruoyi.embedding.model.AliyunMultiModalEmbedRequest;
import org.ruoyi.embedding.model.AliyunMultiModalEmbedResponse;
import org.ruoyi.embedding.model.ModalityType;
import org.ruoyi.embedding.model.MultiModalInput;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云百炼多模态嵌入模型服务实现类
 * 实现了MultiModalEmbedModelService接口，提供文本、图像和视频的嵌入向量生成服务
 */
@Component("bailianMultiModel")
@Slf4j
public class AliBaiLianMultiEmbeddingProvider implements MultiModalEmbedModelService {
    private ChatModelVo chatModelVo;

    private final OkHttpClient okHttpClient;

    /**
     * 构造函数，初始化HTTP客户端
     * 设置连接超时、读取超时和写入超时时间
     */
    public AliBaiLianMultiEmbeddingProvider() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 图像嵌入向量生成
     * @param imageDataUrl 图像数据的URL
     * @return 包含图像嵌入向量的Response对象
     */
    @Override
    public Response<Embedding> embedImage(String imageDataUrl) {
        return embedSingleModality("image", imageDataUrl);
    }

    /**
     * 视频嵌入向量生成
     * @param videoDataUrl 视频数据的URL
     * @return 包含视频嵌入向量的Response对象
     */
    @Override
    public Response<Embedding> embedVideo(String videoDataUrl) {
        return embedSingleModality("video", videoDataUrl);
    }

    /**
     * 多模态嵌入向量生成
     * 支持同时处理文本、图像和视频等多种模态的数据
     * @param input 包含多种模态输入的对象
     * @return 包含多模态嵌入向量的Response对象
     */
    @Override
    public Response<Embedding> embedMultiModal(MultiModalInput input) {
        try {
            // 构建请求内容
            List<Map<String, Object>> contents = buildContentMap(input);
            if (contents.isEmpty()) {
                throw new IllegalArgumentException("至少提供一种模态的内容");
            }

            // 构建请求
            AliyunMultiModalEmbedRequest request = buildRequest(contents, chatModelVo);
            AliyunMultiModalEmbedResponse resp = executeRequest(request, chatModelVo);

            // 转换为 embeddings
            Response<List<Embedding>> response = toEmbeddings(resp);
            List<Embedding> embeddings = response.content();

            if (embeddings.isEmpty()) {
                log.warn("阿里云混合模态嵌入返回为空");
                return Response.from(Embedding.from(new float[0]), response.tokenUsage());
            }

            // 多模态通常取第一个向量作为代表，也可以根据业务场景返回多个
            return Response.from(embeddings.get(0), response.tokenUsage());

        } catch (Exception e) {
            log.error("阿里云混合模态嵌入失败", e);
            throw new IllegalArgumentException("阿里云混合模态嵌入失败", e);
        }
    }

    /**
     * 配置模型参数
     * @param config 模型配置信息
     */
    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    /**
     * 获取支持的模态类型
     * @return 支持的模态类型集合
     */
    @Override
    public Set<ModalityType> getSupportedModalities() {
        return Set.of(ModalityType.TEXT, ModalityType.VIDEO, ModalityType.IMAGE);
    }

    /**
     * 批量文本嵌入向量生成
     * @param textSegments 文本段列表
     * @return 包含所有文本嵌入向量的Response对象
     */
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        if (textSegments.isEmpty()) return Response.from(Collections.emptyList());

        try {
            List<Map<String, Object>> contents = new ArrayList<>();
            for (TextSegment segment : textSegments) {
                contents.add(Map.of("text", segment.text()));
            }

            AliyunMultiModalEmbedRequest request = buildRequest(contents, chatModelVo);
            AliyunMultiModalEmbedResponse resp = executeRequest(request, chatModelVo);

            return toEmbeddings(resp);
        } catch (Exception e) {
            log.error("阿里云文本嵌入失败", e);
            throw new IllegalArgumentException("阿里云文本嵌入失败", e);
        }
    }

    /**
     * 单模态嵌入（图片/视频/单条文本）复用方法
     * @param key 模态类型（image/video/text）
     * @param dataUrl 数据URL
     * @return 包含嵌入向量的Response对象
     */

    public Response<Embedding> embedSingleModality(String key, String dataUrl) {
        try {
            AliyunMultiModalEmbedRequest request = buildRequest(List.of(Map.of(key, dataUrl)), chatModelVo);
            AliyunMultiModalEmbedResponse resp = executeRequest(request, chatModelVo);

            Response<List<Embedding>> response = toEmbeddings(resp);
            List<Embedding> embeddings = response.content();

            if (embeddings.isEmpty()) {
                log.warn("阿里云 {} 嵌入返回为空", key);
                return Response.from(Embedding.from(new float[0]), response.tokenUsage());
            }

            return Response.from(embeddings.get(0), response.tokenUsage());
        } catch (Exception e) {
            log.error("阿里云 {} 嵌入失败", key, e);
            throw new IllegalArgumentException("阿里云 " + key + " 嵌入失败", e);
        }
    }

    /**
     * 构建请求对象
     * @param contents 请求内容列表
     * @param chatModelVo 模型配置信息
     * @return 构建好的请求对象
     */
    private AliyunMultiModalEmbedRequest buildRequest(List<Map<String, Object>> contents, ChatModelVo chatModelVo) {
        if (contents.isEmpty()) throw new IllegalArgumentException("请求内容不能为空");
        return AliyunMultiModalEmbedRequest.create(chatModelVo.getModelName(), contents);
    }

    /**
     * 执行 HTTP 请求并解析响应
     * @param request 请求对象
     * @param chatModelVo 模型配置信息
     * @return API响应对象
     * @throws IOException IO异常
     */
    private AliyunMultiModalEmbedResponse executeRequest(AliyunMultiModalEmbedRequest request, ChatModelVo chatModelVo) throws IOException {
        String jsonBody = request.toJson();
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

        Request httpRequest = new Request.Builder()
                .url(chatModelVo.getApiHost())
                .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
                .post(body)
                .build();

        try (okhttp3.Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "无错误信息";
                throw new IllegalArgumentException("API调用失败: " + response.code() + " - " + err, null);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) throw new IllegalArgumentException("响应体为空", null);

            return parseEmbeddingsFromResponse(responseBody.string());
        }
    }

    /**
     * 解析嵌入向量列表
     * @param responseBody API响应的JSON字符串
     * @return 嵌入向量响应对象
     * @throws IOException IO异常
     */
    private AliyunMultiModalEmbedResponse parseEmbeddingsFromResponse(String responseBody) throws IOException {
        ObjectMapper objectMapper1 = new ObjectMapper();
        return objectMapper1.readValue(responseBody, AliyunMultiModalEmbedResponse.class);
    }

    /**
     * 构建 API 请求内容 Map
     * @param input 多模态输入对象
     * @return 包含各种模态内容的Map列表
     */
    private List<Map<String, Object>> buildContentMap(MultiModalInput input) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (input.getText() != null && !input.getText().isBlank()) {
            contents.add(Map.of("text", input.getText()));
        }
        if (input.getImageUrl() != null && !input.getImageUrl().isBlank()) {
            contents.add(Map.of("image", input.getImageUrl()));
        }
        if (input.getVideoUrl() != null && !input.getVideoUrl().isBlank()) {
            contents.add(Map.of("video", input.getVideoUrl()));
        }
        if (input.getMultiImageUrls() != null && input.getMultiImageUrls().length > 0) {
            contents.add(Map.of("multi_images", Arrays.asList(input.getMultiImageUrls())));
        }

        return contents;
    }

    /**
     * 将 API 原始响应解析为 LangChain4j 的 Response<Embedding>
     * @param resp API原始响应对象
     * @return 包含嵌入向量和token使用情况的Response对象
     */
    private Response<List<Embedding>> toEmbeddings(AliyunMultiModalEmbedResponse resp) {
        if (resp == null || resp.output() == null || resp.output().embeddings() == null) {
            return Response.from(Collections.emptyList());
        }

        // 转换 double -> float
        List<Embedding> embeddings = resp.output().embeddings().stream()
                .map(item -> {
                    float[] vector = new float[item.embedding().size()];
                    for (int i = 0; i < item.embedding().size(); i++) {
                        vector[i] = item.embedding().get(i).floatValue();
                    }
                    return Embedding.from(vector);
                })
                .toList();

        // 构建 TokenUsage
        TokenUsage tokenUsage = null;
        if (resp.usage() != null) {
            tokenUsage = new TokenUsage(
                    resp.usage().input_tokens(),
                    resp.usage().image_tokens(),
                    resp.usage().input_tokens() +resp.usage().image_tokens()
            );
        }

        return Response.from(embeddings, tokenUsage);
    }
}
