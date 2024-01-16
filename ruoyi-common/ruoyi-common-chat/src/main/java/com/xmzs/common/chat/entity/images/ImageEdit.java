package com.xmzs.common.chat.entity.images;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xmzs.common.chat.openai.exception.CommonError;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import com.xmzs.common.core.exception.base.BaseException;


import java.io.Serializable;
import java.util.Objects;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Getter
@Slf4j
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ImageEdit implements Serializable {
    /**
     * 必选项：描述文字，最多1000字符
     */
    @NonNull
    private String prompt;
    /**
     * 为每个提示生成的完成次数。
     */
    @Builder.Default
    private Integer n = 1;
    /**
     * 256x256
     * 512x512
     * 1024x1024
     */
    @Builder.Default
    private String size = SizeEnum.size_512.getName();

    @JsonProperty("response_format")
    @Builder.Default
    private String responseFormat = ResponseFormat.URL.getName();

    private String user;

    public ImageEdit setN(Integer n) {
        if(n < 1){
            log.warn("n最小值1");
            n = 1;
        }
        if(n > 10){
            log.warn("n最大值10");
            n = 10;
        }
        this.n = n;
        return this;
    }

    public ImageEdit setPrompt(String prompt) {
        if(Objects.isNull(prompt) || "".equals(prompt)){
            log.error("参数异常");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
        if(prompt.length() > 1000){
            log.error("长度超过1000");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
        this.prompt = prompt;
        return this;
    }

    public ImageEdit setSize(SizeEnum size) {
        if(Objects.isNull(size)){
            size = SizeEnum.size_512;
        }
        this.size = size.getName();
        return this;
    }

    public ImageEdit setResponseFormat(ResponseFormat responseFormat) {
        if(Objects.isNull(responseFormat)){
            responseFormat = ResponseFormat.URL;
        }
        this.responseFormat = responseFormat.getName();
        return this;
    }

    public ImageEdit setUser(String user) {
        this.user = user;
        return this;
    }


}
