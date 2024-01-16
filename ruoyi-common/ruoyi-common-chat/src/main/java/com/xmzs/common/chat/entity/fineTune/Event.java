package com.xmzs.common.chat.entity.fineTune;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event implements Serializable {
    private String object;
    @JsonProperty("created_at")
    private long createdAt;
    private String level;
    private String message;
}
