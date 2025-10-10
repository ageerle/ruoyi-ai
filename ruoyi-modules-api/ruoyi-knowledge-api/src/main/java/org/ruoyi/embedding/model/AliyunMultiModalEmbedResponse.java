package org.ruoyi.embedding.model;

import java.util.List;

/**
 * 阿里云多模态嵌入 API 响应数据模型
 */
public record AliyunMultiModalEmbedResponse(
        Output output,       // 输出结果对象
        String request_id,   // 请求唯一标识
        String code,         // 错误码
        String message,      // 错误消息
        Usage usage          // 用量信息
) {

    /**
     * 输出对象，包含嵌入向量结果
     */
    public record Output(
            List<EmbeddingItem> embeddings // 嵌入向量列表
    ) {
    }

    /**
     * 单个嵌入向量条目
     */
    public record EmbeddingItem(
            int index,             // 输入内容的索引
            List<Double> embedding, // 生成的 1024 维向量
            String type            // 输入的类型 (text/image/video/multi_images)
    ) {
    }

    /**
     * 用量统计信息
     */
    public record Usage(
            int input_tokens,  // 本次请求输入的 Token 数量
            int image_tokens,  // 本次请求输入的图像 Token 数量
            int image_count,   // 本次请求输入的图像数量
            int duration       // 本次请求输入的视频时长（秒）
    ) {
    }
}
