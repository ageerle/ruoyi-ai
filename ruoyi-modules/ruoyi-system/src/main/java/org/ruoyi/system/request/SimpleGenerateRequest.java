package org.ruoyi.system.request;

import lombok.Data;

import java.util.List;

/**
 * @author WangLe
 */
@Data
public class SimpleGenerateRequest {

    /**
     * 角色ID
     */
    private String voiceId;

    /**
     * 要生成的文本内容
     */
    private String text;

    /**
     * 要生成的文本内容 优先级高于text
     */
    private List<String> texts;

    /**
     * 角色风格 ID （默认为default)
     */
    private String promptId = "default";

    /**
     * 要使用的模型ID (目前统一为reecho-neural-voice-001)
     */
    private String model = "reecho-neural-voice-001";

    /**
     * 多样性 (0-100，默认为97)
     */
    private Integer randomness;

    /**
     * 稳定性过滤 (0-100，默认为100)
     */
    private Integer stability_boost;

    /**
     * 概率优选（0-100，默认为99）
     */
    private Integer probability_optimization;

    /**
     * 是否直接返回生成音频的Base64 DataURL，而不是Url链接（默认为false)
     */
    private Boolean origin_audio;

    /**
     * 是否启用流式生成
     */
    private Boolean stream;

}
