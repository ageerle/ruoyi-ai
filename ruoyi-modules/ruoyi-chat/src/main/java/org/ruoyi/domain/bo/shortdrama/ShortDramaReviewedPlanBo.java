package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;

@Data
public class ShortDramaReviewedPlanBo {
    @NotNull private Long scriptId;
    @NotBlank private String expectedScriptText;
    private String model;
    @NotEmpty @Size(max=500) private List<StoryboardPanelData> panels;
}
