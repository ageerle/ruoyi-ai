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
    @NotNull @Min(1) @Max(15) private Integer minimumShotSeconds = 1;
    /** Absent means a full reviewed plan; present imports only explicitly reviewed scenes. */
    @Size(min=1, max=100) private List<@NotNull @Min(1) Integer> sceneNumbers;
    /** Returned by checkpoint-status. Required for partial imports, ignores temporary image URLs. */
    @Pattern(regexp="[a-f0-9]{64}") private String expectedAssetSignature;
    @NotEmpty @Size(max=500) private List<StoryboardPanelData> panels;
}
