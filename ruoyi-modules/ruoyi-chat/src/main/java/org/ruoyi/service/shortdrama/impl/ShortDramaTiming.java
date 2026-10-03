package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

/** Editable production estimate. Recorded speech duration should replace estimates once available. */
@Slf4j
public final class ShortDramaTiming {
    private ShortDramaTiming() { }
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Only locally generated timing diagnostics are safe for the production controller to display. */
    public static final class ReviewException extends IllegalArgumentException {
        private ReviewException(String message) { super(message); }
    }

    private record Budget(double speech, double action, double pause, String pacingNote, boolean strictFill) {
        double required() { return Math.ceil((speech + action + pause) * 10) / 10; }
    }

    public static double requiredSeconds(String source, String continuity) {
        return budget(source, continuity).required();
    }

    private static Budget budget(String source, String continuity) {
        JsonNode root;
        try { root = JSON.readTree(continuity == null || continuity.isBlank() ? "{}" : continuity); }
        catch (Exception e) { throw new IllegalArgumentException("镜头承接信息须为有效JSON", e); }
        if (root == null || !root.isObject()) throw new IllegalArgumentException("镜头承接信息须为JSON对象");
        JsonNode t = root.path("timing");
        String fallback = String.join(" ", ShortDramaVideoPromptReview.sourceDialogue(source));
        String spoken = t.has("spoken_text") ? t.path("spoken_text").asText() : fallback;
        double units = Math.max(spokenUnits(spoken), spokenUnits(fallback));
        double rate = t.path("speech_rate").asDouble(4);
        if (!Double.isFinite(rate) || rate <= 0) throw new IllegalArgumentException("对白语速须为正的有限数值");
        double measured = t.path("measured_audio_seconds").asDouble(0);
        double action = t.path("action_seconds").asDouble(0);
        double pause = t.path("pause_seconds").asDouble(spoken.isBlank() ? 0 : 2);
        if (!Double.isFinite(action) || !Double.isFinite(pause) || !Double.isFinite(measured)
            || action < 0 || pause < 0 || measured < 0) throw new IllegalArgumentException("时间预算必须是非负有限数值");
        return new Budget(measured > 0 ? measured : units / rate, action, pause,
            t.path("pacing_note").isTextual() ? t.path("pacing_note").asText().strip() : "",
            t.path("strict_fill").asBoolean(false));
    }

    private static double spokenUnits(String text) {
        return text.codePoints().mapToDouble(cp -> cp >= 0x3400 && cp <= 0x9fff ? 1
            : cp >= '0' && cp <= '9' ? 1.5 : (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') ? 1 : 0).sum();
    }

    public static void validate(Integer number, Integer duration, String source, String continuity) {
        if (duration == null || duration <= 0) throw new IllegalArgumentException("制作估算时长须为正整数秒");
        // Only data shape is mandatory. Creative estimates never reject saving or production.
        for (String issue : issues(number, duration, source, continuity))
            log.info("时长审阅建议（不影响保存或生成）：{}", issue);
    }

    public static List<String> issues(Integer number, Integer duration, String source, String continuity) {
        Budget budget = budget(source, continuity);
        List<String> issues = new ArrayList<>();
        if (duration == null || duration <= 0) return List.of("制作估算时长尚未填写");
        double required = budget.required();
        if (required > duration + 0.05) issues.add("镜头" + number + "内容估算约" + required + "秒，当前安排" + duration + "秒，可结合实际表演调整");
        // Review trigger, not an automatic maximum: suspense and physical action can justify time.
        // Compare against speech alone so invented action/pause budgets cannot hide a padded shot.
        if (duration >= 8 && budget.speech > 0
            && duration - budget.speech > Math.max(3, budget.speech * 0.75) + 0.05
            && (budget.pacingNote.isBlank() || budget.pacingNote.matches("(?i)^(待补充|同上|略|TODO|暂无)[。；;\\s]*$")))
            issues.add("镜头" + number + "对白估算约" + Math.round(budget.speech * 10) / 10.0
                + "秒，当前安排" + duration + "秒，可留意动作与停顿的节奏");
        int shortestIntegerDuration = Math.max(1, (int) Math.ceil(required - 0.000001));
        if (budget.strictFill && duration > shortestIntegerDuration)
            issues.add("镜头" + number + "有效内容估算约" + required + "秒，当前安排" + duration
                + "秒，可根据动作结果与反应决定结束点");
        return List.copyOf(issues);
    }

    /** API production gates must expose actionable review errors instead of an unknown-error toast. */
    public static void validateSubmission(Integer number, Integer duration, String source, String continuity) {
        try { validate(number, duration, source, continuity); }
        catch (IllegalArgumentException e) { throw new ReviewException(e.getMessage()); }
    }
}
