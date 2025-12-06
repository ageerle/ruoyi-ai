package org.ruoyi.common.chat.entity.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalModelsSearchResponse {
    @JsonProperty("topKEmbeddings")

    private List<List<List<Double>>> topKEmbeddings;  // 处理三层嵌套数组

    // 默认构造函数
    public LocalModelsSearchResponse() {
    }


}
