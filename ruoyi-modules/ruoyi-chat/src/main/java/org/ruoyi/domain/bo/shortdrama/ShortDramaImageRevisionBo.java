package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Optional request-only edit intent; omission preserves approved identity/location continuity. */
@Data
public class ShortDramaImageRevisionBo {
    @Pattern(regexp = "identity|identity_revision|location|location_revision")
    private String referencePurpose;

    @Size(max = 4000)
    private String revisionRequirements;

    /** Separate look references; the primary reference still owns identity or geometry. */
    @Size(max = 3)
    private java.util.List<@Pattern(regexp = "https?://.+") String> styleReferenceImageUrls;
}
