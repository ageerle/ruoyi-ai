package org.ruoyi.common.chat.entity.images;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 *  
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {
    private String url;
    @JsonProperty("b64_json")
    private String b64Json;
}
