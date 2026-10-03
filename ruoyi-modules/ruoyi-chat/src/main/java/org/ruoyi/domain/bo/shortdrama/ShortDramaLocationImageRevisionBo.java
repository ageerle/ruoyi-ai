package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request-only image changes. Never persisted into the frozen location description. */
@Data
public class ShortDramaLocationImageRevisionBo {
    @Size(max = 4000)
    private String revisionRequirements;
}
