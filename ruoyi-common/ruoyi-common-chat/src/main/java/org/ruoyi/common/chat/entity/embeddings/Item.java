package org.ruoyi.common.chat.entity.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {
    private String object;
    private List<BigDecimal> embedding;
    private Integer index;
}
