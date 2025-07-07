package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastGPTAnswerResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<FastGPTChatChoice> choices;
}
