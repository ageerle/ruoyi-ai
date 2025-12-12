package org.ruoyi.common.chat.entity.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Permission implements Serializable {

    private String id;
    @JsonProperty("object")
    private String object;
    @JsonProperty("created")
    private long created;
    @JsonProperty("allow_create_engine")
    private boolean allowCreateEngine;
    @JsonProperty("allow_sampling")
    private boolean allowSampling;
    @JsonProperty("allow_logprobs")
    private boolean allowLogprobs;
    @JsonProperty("allow_search_indices")
    private boolean allowSearchIndices;
    @JsonProperty("allow_view")
    private boolean allowView;
    @JsonProperty("allow_fine_tuning")
    private boolean allowFineTuning;
    @JsonProperty("organization")
    private String organization;
    /**
     * 不知道是什么类型的数据
     */
    @JsonProperty("group")
    private Object group;
    @JsonProperty("is_blocking")
    private boolean isBlocking;
}
