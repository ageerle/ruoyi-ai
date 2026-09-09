package org.ruoyi.service.coding.harness.app;

import java.util.Locale;
import java.util.Set;

/** One local image supplied with the immutable initial run requirement. */
public record HarnessImageInput(String dataUrl, String detail) {

    /** DeepSeek 既有语义：auto/low/original（original 按高精度发送）。 */
    private static final Set<String> DEEPSEEK_DETAILS = Set.of("auto", "low", "original");
    /** Doubao-Seed-Evolving 图片精度：low/high/xhigh，默认 high。 */
    private static final Set<String> DOUBAO_DETAILS = Set.of("low", "high", "xhigh");

    public HarnessImageInput {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("image dataUrl is required");
        }
        detail = detail == null || detail.isBlank()
            ? "auto" : detail.strip().toLowerCase(Locale.ROOT);
        if (!DEEPSEEK_DETAILS.contains(detail) && !DOUBAO_DETAILS.contains(detail)) {
            throw new IllegalArgumentException(
                "image detail must be one of auto/low/original (DeepSeek) or low/high/xhigh (Doubao)");
        }
    }
}
