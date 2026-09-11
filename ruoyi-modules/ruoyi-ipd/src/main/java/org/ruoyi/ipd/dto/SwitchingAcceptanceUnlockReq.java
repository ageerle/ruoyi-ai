package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 月度解锁请求（P3-7.1；事故恢复用；仅超管）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwitchingAcceptanceUnlockReq(
    @NotBlank @Size(min = 5, max = 500, message = "解锁理由 5-500 字符") String reason
) {
}