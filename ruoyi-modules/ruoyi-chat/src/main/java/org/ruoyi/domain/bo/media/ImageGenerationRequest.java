package org.ruoyi.domain.bo.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ImageGenerationRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String size;

    @Min(value = 0, message = "随机种子不能小于0")
    @Max(value = 2147483647, message = "随机种子不能大于2147483647")
    private Integer seed;
}
