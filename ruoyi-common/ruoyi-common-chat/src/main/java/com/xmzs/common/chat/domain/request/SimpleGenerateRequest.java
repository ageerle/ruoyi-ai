package com.xmzs.common.chat.domain.request;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class SimpleGenerateRequest {

    /**
     * 要使用的模型ID (目前统一为reecho-neural-voice-001)
     */
    private String model = "reecho-neural-voice-001";

    /**
     * 多样性 (0-100，默认为97)
     */
    private Integer randomness;

    /**
     * 稳定性过滤 (0-100，默认为0)
     */
    private Integer stability_boost;

    /**
     * 角色ID
     */
    private String voiceId;

    /**
     * 要生成的文本内容
     */
    private String text;
}
