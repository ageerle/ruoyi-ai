package org.ruoyi.domain.bo.media;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoGenerationRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String size;

    private Integer seconds;

    private String quality;
}
