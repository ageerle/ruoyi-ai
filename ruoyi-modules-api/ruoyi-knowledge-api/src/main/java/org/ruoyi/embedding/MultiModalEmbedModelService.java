package org.ruoyi.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.ruoyi.embedding.model.MultiModalInput;


/**
 * 多模态嵌入模型服务接口，继承自基础嵌入模型服务
 * 该接口提供了处理图像、视频以及多模态数据并转换为嵌入向量的功能
 */
public interface MultiModalEmbedModelService extends BaseEmbedModelService {
    /**
     * 将图像数据转换为嵌入向量
     *
     * @param imageDataUrl 图像的地址，必须是公开可访问的URL
     * @return 包含嵌入向量的响应对象，可能包含状态信息和嵌入结果
     */
    Response<Embedding> embedImage(String imageDataUrl);

    /**
     * 将视频数据转换为嵌入向量
     *
     * @param videoDataUrl 视频的地址，必须是公开可访问的URL
     * @return 包含嵌入向量的响应对象，可能包含状态信息和嵌入结果
     */
    Response<Embedding> embedVideo(String videoDataUrl);


    /**
     * 处理多模态输入并返回嵌入向量的方法
     *
     * @param input 包含多种模态信息（如图像、文本等）的输入对象
     * @return Response<Embedding> 包含嵌入向量的响应对象，Embedding通常表示输入数据的向量表示
     */
    Response<Embedding> embedMultiModal(MultiModalInput input);
}
