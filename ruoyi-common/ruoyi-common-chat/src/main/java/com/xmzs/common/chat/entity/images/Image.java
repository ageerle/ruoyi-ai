package com.xmzs.common.chat.entity.images;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Getter
@Slf4j
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class Image implements Serializable {

    /**
     * 提示词：dall-e-2支持1000字符、dall-e-3支持4000字符
     */
    private String prompt;
    /**
     * 支持dall-e-2、dall-e-3
     *
     * @see Model
     */
    @Builder.Default
    private String model = Model.DALL_E_3.getName();

    /**
     * 此参数仅仅dall-e-3,默认值：standard
     *
     * @see Quality
     */
    private String quality;

    /**
     * 为每个提示生成的个数，dall-e-3只能为1。
     */
    private Integer n;
    /**
     * 图片尺寸，默认值：1024x1024
     * dall-e-2支持：256x256, 512x512, or 1024x1024
     * dall-e-3支持：1024x1024, 1792x1024, or 1024x1792
     *
     * @see SizeEnum
     */
    private String size;
    /**
     * 此参数仅仅dall-e-3,取值范围：vivid、natural
     * 默认值：vivid
     *
     * @see Style
     */
    private String style;

    /**
     * 生成图片格式：url、b64_json
     *
     * @see ResponseFormat
     */
    @JsonProperty("response_format")
    private String responseFormat;

    private String user;

    /**
     * 图片生成模型
     */
    @Getter
    @AllArgsConstructor
    public enum Model {
        DALL_E_2("dall-e-2"),
        DALL_E_3("dall-e-3"),
        ;
        private final String name;
    }

    /**
     * 生成图片质量
     */
    @Getter
    @AllArgsConstructor
    public enum Quality {
        STANDARD("standard"),
        HD("hd"),
        ;
        private final String name;
    }

    /**
     * 生成图片风格
     */
    @Getter
    @AllArgsConstructor
    public enum Style {
        VIVID("vivid"),
        NATURAL("natural"),
        ;
        private final String name;
    }
}
