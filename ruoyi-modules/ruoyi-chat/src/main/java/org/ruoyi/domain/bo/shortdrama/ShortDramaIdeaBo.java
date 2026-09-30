package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShortDramaIdeaBo {

    @NotBlank(message = "创意想法不能为空")
    private String idea;

    @NotBlank(message = "模型不能为空")
    private String model;

    private String projectName;

    private String artStyle;

    private String aspectRatio;

    /** Stop after script so the author can review before asset/shot planning. */
    private Boolean scriptOnly = false;
}
