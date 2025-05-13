package org.ruoyi.common.chat.entity.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.ruoyi.common.chat.entity.common.Usage;

import java.io.Serializable;
import java.util.List;

/**
 *  
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
