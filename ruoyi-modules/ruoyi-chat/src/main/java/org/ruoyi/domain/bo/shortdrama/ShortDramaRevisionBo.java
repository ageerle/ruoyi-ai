package org.ruoyi.domain.bo.shortdrama;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ShortDramaRevisionBo {
    @NotNull private Long scriptId;
    @NotBlank private String expectedScriptText;
    @NotBlank private String scriptText;
    private String outlineText;
    @NotEmpty private List<ShortDramaStoryboardBo> storyboards;
    private List<ShortDramaCharacterBo> characters;
    private List<ShortDramaLocationBo> locations;
}
