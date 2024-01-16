package com.xmzs.common.chat.entity.completions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xmzs.common.chat.entity.common.Choice;
import com.xmzs.common.chat.entity.common.OpenAiResponse;
import com.xmzs.common.chat.entity.common.Usage;
import lombok.Data;


import java.io.Serializable;

/**
 * 描述： 答案类
 *
 * @author https:www.unfbx.com
 *  2023-02-11
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompletionResponse extends OpenAiResponse implements Serializable {
    private String id;
    private String object;
    private long created;
    private String model;
    private Choice[] choices;
    private Usage usage;
}
