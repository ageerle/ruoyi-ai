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
    private String aestheticSkillName;
    private String directorSkillName;

    private String aspectRatio;

    /** 兼容旧请求字段；创建接口始终只生成剧本，该值不再控制后续阶段。 */
    @Deprecated
    private Boolean scriptOnly = true;

}
