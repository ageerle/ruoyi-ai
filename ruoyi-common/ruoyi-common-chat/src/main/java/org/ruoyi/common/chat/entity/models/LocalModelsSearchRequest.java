package org.ruoyi.common.chat.entity.models;

import lombok.Data;

import java.util.List;

/**
 * @program: RUOYIAI
 * @ClassName LocalModelsSearchRequest
 * @description:
 * @author: hejh
 * @create: 2025-03-15 17:22
 * @Version 1.0
 **/
@Data
public class LocalModelsSearchRequest {

    private List<String> text;
    private String model_name;
    private String delimiter;
    private int k;
    private int block_size;
    private int overlap_chars;

    // 构造函数、Getter 和 Setter
    public LocalModelsSearchRequest(List<String> text, String model_name, String delimiter, int k, int block_size, int overlap_chars) {
        this.text = text;
        this.model_name = model_name;
        this.delimiter = delimiter;
        this.k = k;
        this.block_size = block_size;
        this.overlap_chars = overlap_chars;
    }


}


