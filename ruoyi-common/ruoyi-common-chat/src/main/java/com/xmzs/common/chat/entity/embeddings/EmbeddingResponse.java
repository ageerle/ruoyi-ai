package com.xmzs.common.chat.entity.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xmzs.common.chat.entity.common.Usage;
import lombok.Data;


import java.io.Serializable;
import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse implements Serializable {

    private String object;
    private List<Item> data;
    private String model;
    private Usage usage;
}
