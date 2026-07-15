package org.ruoyi.domain.bo.media;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpeechGenerationRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "输入文本不能为空")
    private String input;

    private String voice;

    private String responseFormat;

    private Double speed;

    private String instructions;
}
