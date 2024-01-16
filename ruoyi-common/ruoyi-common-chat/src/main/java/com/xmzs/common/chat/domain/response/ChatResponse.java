package com.xmzs.common.chat.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @sine 2023-04-08
 */
@Data
public class ChatResponse {
    /**
     * 问题消耗tokens
     */
    @JsonProperty("question_tokens")
    private long questionTokens = 0;
}
