package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShortDramaComposeVideoBo {

    @NotBlank(message = "转场类型不能为空")
    @Pattern(regexp = "none|dissolve|fade|slide", message = "不支持的转场类型")
    private String transitionType = "dissolve";

    @NotNull(message = "转场时长不能为空")
    @DecimalMin(value = "0.0", message = "转场时长不能小于0秒")
    private BigDecimal transitionDurationSeconds = new BigDecimal("0.5");

    @NotBlank(message = "成片画幅不能为空")
    @Pattern(regexp = "9:16|16:9|1:1", message = "不支持的成片画幅")
    private String aspectRatio = "9:16";
    @Size(min = 2, message = "至少选择2个分镜视频")
    private List<Long> storyboardIds;
}
