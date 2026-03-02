package org.ruoyi.workflow.workflow.node.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class ImageNodeConfig {

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 模型名称
     */
    @NotNull
    @JsonProperty("model_name")
    private String modelName;

    /**
     * 图片尺寸大小
     */
    private String size;

    /**
     * 随机数种子
     */
    @Min(value = 0, message = "随机数种子不能小于0")
    @Max(value = 2147483647, message = "随机数种子不能大于2147483647")
    private Integer seed;
}
