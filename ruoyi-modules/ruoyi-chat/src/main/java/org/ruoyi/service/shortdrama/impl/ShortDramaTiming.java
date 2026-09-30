package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

/** Editable production estimate. Recorded speech duration should replace estimates once available. */
public final class ShortDramaTiming {
    private ShortDramaTiming() { }
    private static final Pattern QUOTE = Pattern.compile("「([^」]+)」");
    private static final ObjectMapper JSON = new ObjectMapper();

    public static double requiredSeconds(String source, String continuity) {
        JsonNode root;
        try { root = JSON.readTree(continuity == null || continuity.isBlank() ? "{}" : continuity); }
        catch (Exception e) { throw new IllegalArgumentException("镜头承接信息须为有效JSON", e); }
        if (root == null || !root.isObject()) throw new IllegalArgumentException("镜头承接信息须为JSON对象");
        JsonNode t = root.path("timing");
        StringBuilder fallback = new StringBuilder();
        var matcher = QUOTE.matcher(source == null ? "" : source);
        while (matcher.find()) fallback.append(matcher.group(1)).append(' ');
        String spoken = t.has("spoken_text") ? t.path("spoken_text").asText() : fallback.toString();
        double units = Math.max(spokenUnits(spoken), spokenUnits(fallback.toString()));
        double rate = t.path("speech_rate").asDouble(4);
        if (!Double.isFinite(rate) || rate < 2 || rate > 5) throw new IllegalArgumentException("对白语速须在2至5字/秒之间");
        double measured = t.path("measured_audio_seconds").asDouble(0);
        double action = t.path("action_seconds").asDouble(0);
        double pause = t.path("pause_seconds").asDouble(spoken.isBlank() ? 0 : 2);
        if (!Double.isFinite(action) || !Double.isFinite(pause) || !Double.isFinite(measured)
            || action < 0 || pause < 0 || measured < 0) throw new IllegalArgumentException("时间预算必须是非负有限数值");
        return Math.ceil(((measured > 0 ? measured : units / rate) + action + pause) * 10) / 10;
    }

    private static double spokenUnits(String text) {
        return text.codePoints().mapToDouble(cp -> cp >= 0x3400 && cp <= 0x9fff ? 1
            : cp >= '0' && cp <= '9' ? 1.5 : (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') ? 1 : 0).sum();
    }

    public static void validate(Integer number, Integer duration, String source, String continuity) {
        if (duration == null || duration <= 0 || duration > 60) throw new IllegalArgumentException("镜头时长须在1至60秒之间");
        double required = requiredSeconds(source, continuity);
        if (required > duration + 0.05) throw new IllegalArgumentException("镜头" + number + "内容至少约" + required + "秒，分配仅" + duration + "秒，请缩短台词、拆镜或延长时长");
    }
}
