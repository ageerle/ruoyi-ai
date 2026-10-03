package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/** Structural shot contracts do not replace visual review of generated media. */
@Slf4j
final class ShortDramaShotDesign {
    private ShortDramaShotDesign() {}
    static final class InvalidDesign extends IllegalArgumentException {
        InvalidDesign(String message) { super(message); }
    }
    /** Expand only an explicit delta contract. Never repair contradictory full-state output. */
    static void expandDeltas(List<StoryboardPanelData> panels) {
        JsonNode previous = null;
        for (int i = 0; i < panels.size(); i++) {
            JsonNode original = panels.get(i).getShotDesign();
            if (original == null || !original.isObject()) { previous = original; continue; }
            var design = (com.fasterxml.jackson.databind.node.ObjectNode) original;
            if (design.has("state_delta")) {
                String prefix = "镜头" + (i + 1) + "专业分镜：";
                boolean continuous = previous != null && "continuous".equals(design.path("transition").asText());
                if (continuous) {
                    if (!design.has("state_in")) design.set("state_in", previous.path("state_out").deepCopy());
                    if (!design.has("cut_in")) design.set("cut_in", previous.path("cut_out").deepCopy());
                }
                JsonNode input = design.path("state_in"), delta = design.path("state_delta");
                if (!input.isObject() || !delta.isObject()) { previous = design; continue; }
                var output = (com.fasterxml.jackson.databind.node.ObjectNode) input.deepCopy();
                delta.fields().forEachRemaining(entry -> {
                    output.set(entry.getKey(), entry.getValue());
                });
                // Preserve explicit states and wording. Missing anchors are review suggestions, not gates.
                if (!design.has("state_out")) design.set("state_out", output);
                design.remove("state_delta"); // Stored/read-back contracts always have complete first and last states.
            }
            previous = design;
        }
    }
    static void validate(List<StoryboardPanelData> panels) {
        for (String issue : issues(panels)) log.info("分镜承接审阅建议（继续保存）：{}", issue);
    }
    static List<String> issues(List<StoryboardPanelData> panels) {
        try { validateContract(panels); return List.of(); }
        catch (InvalidDesign issue) { return List.of(issue.getMessage()); }
    }
    private static void validateContract(List<StoryboardPanelData> panels) {
        JsonNode previous = null;
        for (int i = 0; i < panels.size(); i++) {
            JsonNode design = panels.get(i).getShotDesign();
            String prefix = "镜头" + (i + 1) + "专业分镜：";
            if (design == null || !design.isObject()) throw new InvalidDesign(prefix + "缺少shot_design镜头契约");
            for (String key : List.of("focus", "viewer_gain", "framing", "axis", "movement", "motivation", "cut_in", "cut_out", "transition")) {
                if (!design.path(key).isTextual() || design.path(key).asText().isBlank())
                    throw new InvalidDesign(prefix + "缺少具体的" + key);
            }
            String transition = design.path("transition").asText();
            if (!Set.of("opening", "continuous", "time_change", "space_change").contains(transition))
                throw new InvalidDesign(prefix + "transition类型无效");
            for (String key : List.of("state_in", "state_out")) {
                JsonNode state = design.path(key);
                if (!state.isObject() || state.isEmpty()) throw new InvalidDesign(prefix + key + "须为非空物理状态对象");
                state.fields().forEachRemaining(entry -> {
                    if (entry.getKey().isBlank() || !entry.getValue().isTextual() || entry.getValue().asText().isBlank())
                        throw new InvalidDesign(prefix + key + "须逐项写具体文字状态");
                });
            }
            java.util.Set<String> inputKeys = new java.util.HashSet<>(), outputKeys = new java.util.HashSet<>();
            design.path("state_in").fieldNames().forEachRemaining(inputKeys::add);
            design.path("state_out").fieldNames().forEachRemaining(outputKeys::add);
            if (!inputKeys.equals(outputKeys)) throw new InvalidDesign(prefix + "镜内首末态须保留相同物理锚点，不能因裁幅改变删去世界状态");
            if (previous != null && "opening".equals(transition))
                throw new InvalidDesign(prefix + "同场后续镜不能以opening掩盖承接");
            if (previous != null && "continuous".equals(transition)) {
                if (!previous.path("cut_out").asText().equals(design.path("cut_in").asText()))
                    throw new InvalidDesign(prefix + "cut_in须承接前镜cut_out的同一动作接点");
                if (!previous.path("state_out").equals(design.path("state_in")))
                    throw new InvalidDesign(prefix + "state_in与前镜state_out不一致，存在姿态、持物或物理状态跳变");
            }
            previous = design;
        }
    }
}
