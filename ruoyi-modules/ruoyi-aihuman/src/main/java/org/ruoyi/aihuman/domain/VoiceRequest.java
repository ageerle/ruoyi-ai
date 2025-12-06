package org.ruoyi.aihuman.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * 语音请求参数实体类
 */
@Data
public class VoiceRequest {

    @JsonProperty("ENDPOINT")
    private String endpoint;
    private String appId;
    private String accessToken;
    private String resourceId;
    private String voice;
    private String text;
    private String encoding;

}