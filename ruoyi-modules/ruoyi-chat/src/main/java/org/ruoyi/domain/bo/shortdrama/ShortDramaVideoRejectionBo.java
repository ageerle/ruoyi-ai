package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Owner attestation for a legacy HTTP rejection whose body was retained in runtime logs. */
public record ShortDramaVideoRejectionBo(@NotNull Integer httpStatus,
                                        @NotBlank @Size(max = 2000) String responseBody,
                                        @NotBlank @Size(min = 20, max = 2000) String evidence) { }
