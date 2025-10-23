package org.ruoyi.embedding.model;

import org.ruoyi.common.json.utils.JsonUtils;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author: Robust_H
 * @Date: 2025-10-1-上午10:00
 * @Description: 阿里云多模态嵌入请求
 */
@Data
public class AliyunMultiModalEmbedRequest {
    private String model;
    private Input input;

    /**
     * 表示输入数据的记录类(Record)
     * 该类用于封装一个包含多个映射关系列表的输入数据结构
     *
     * @param contents 包含多个Map的列表，每个Map中存储String类型的键和Object类型的值
     */
    public record Input(List<Map<String, Object>> contents) { }

    /**
     * 创建请求对象
     */
    public static AliyunMultiModalEmbedRequest create(String modelName, List<Map<String, Object>> contents) {
        AliyunMultiModalEmbedRequest request = new AliyunMultiModalEmbedRequest();
        request.setModel(modelName);
        Input input = new Input(contents);
        request.setInput(input);
        return request;
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        return JsonUtils.toJsonString(this);
    }
}
