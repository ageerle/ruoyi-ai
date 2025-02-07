package org.ruoyi.common.chat.entity.whisper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Translations {
    /**
     * 模型目前只支持这一种：WHISPER_1
     */
    @Builder.Default
    private String model = Whisper.Model.WHISPER_1.getName();
    /**
     * 提示语，需要与语音语言匹配
     */
    private String prompt;
    /**
     * 输出的格式，采用以下选项之一：json、text、srt、verbose_json 或 vtt。
     * 默认值：json
     */
    @JsonProperty("response_format")
    @Builder.Default
    private String responseFormat = Whisper.ResponseFormat.JSON.getName();
    /**
     * 温度控制随机效果：0-1，值越大输出更加随机
     * 默认值：0
     */
    @Builder.Default
    private double temperature = 0;

}
