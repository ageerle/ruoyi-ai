package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.util.*;
import java.util.regex.Pattern;

/** Speaker identity is separate from the visible cast, including off-screen dialogue. */
public final class ShortDramaVoiceContract {
    private ShortDramaVoiceContract() {}
    public record Speakers(List<ShortDramaCharacter> actors, boolean explicit, List<String> issues) {}

    public static Speakers resolve(ShortDramaStoryboard shot, List<ShortDramaCharacter> cast) {
        try {
            JsonNode continuity = AtlasMediaSupport.OBJECT_MAPPER.readTree(Objects.toString(shot.getContinuityJson(), "{}"));
            if (continuity != null && continuity.has("voice_speakers")) {
                if (!continuity.path("voice_speakers").isArray()) throw new ShortDramaVoiceException("voice_speakers 必须是角色ID数组");
                List<ShortDramaCharacter> speakers = new ArrayList<>();
                for (JsonNode id : continuity.path("voice_speakers")) {
                    var actor = cast.stream().filter(c -> c.getId().toString().equals(id.asText())).findFirst()
                        .orElseThrow(() -> new ShortDramaVoiceException("发言人不属于当前项目：" + id.asText()));
                    if (!speakers.contains(actor)) speakers.add(actor);
                }
                return new Speakers(speakers, true, List.of());
            }
        } catch (java.io.IOException e) { throw new ShortDramaVoiceException("连续性信息无法读取，请先修复本镜", e); }
        String text = Objects.toString(shot.getSourceText(), "");
        if (text.isBlank()) text = Objects.toString(shot.getVideoPrompt(), "");
        LinkedHashSet<ShortDramaCharacter> speakers = new LinkedHashSet<>();
        List<String> issues = new ArrayList<>();
        for(var actor:cast)for(String alias:aliases(actor)) {
            var plain=Pattern.compile("(?m)^\\s*"+Pattern.quote(alias)+"\\s*(?:[（(][^）)\\n]*[）)]\\s*)?[：:]\\s*(?![「“\"])([^\\n]+)").matcher(text);
            while(plain.find()) if(!plain.group(1).stripLeading().matches("^[「“\"].*"))speakers.add(actor);
        }
        // Newly planned shots already require one '角色名：「原句」' source line per utterance.
        var lines = Pattern.compile("(?m)^\\s*([^\n：:]{1,40})[：:]\\s*[「“\"]([^」”\"\n]+)[」”\"]").matcher(text);
        List<int[]> labeledRanges = new ArrayList<>();
        while (lines.find()) {
            String name = lines.group(1).replaceAll("[（(].*?[）)]", "").trim();
            var matches = cast.stream().filter(c -> aliases(c).contains(name)).toList();
            if (matches.size() == 1) { speakers.add(matches.get(0)); labeledRanges.add(new int[]{lines.start(),lines.end()}); }
            else if (Set.of("画面文字", "字幕", "牌匾", "文书文字").contains(name)) labeledRanges.add(new int[]{lines.start(),lines.end()});
            else if (!Set.of("画面文字", "字幕", "牌匾", "文书文字").contains(name)) issues.add("未绑定发言人：" + name);
        }
        // Legacy prose: only infer an unambiguous actor in the immediate speech clause.
        var speech = Pattern.compile("[「“\"]([^」”\"\n]+)[」”\"]").matcher(text);
        while (speech.find()) {
            final int position=speech.start();
            if(labeledRanges.stream().anyMatch(range -> position>=range[0] && position<range[1]))continue;
            int from = Math.max(0, speech.start() - 140);
            String prefix = text.substring(from, speech.start());
            int boundary = Math.max(Math.max(prefix.lastIndexOf('。'), prefix.lastIndexOf('\n')), prefix.lastIndexOf('！'));
            String clause = prefix.substring(boundary + 1);
            if (!clause.matches("(?s).*(说|问|道|喊|念叨|开口|画外音|语气|声音|亲口|接着|继续).*")) continue;
            var matches = cast.stream().filter(c -> aliases(c).stream().anyMatch(clause::contains)).toList();
            if (matches.size() == 1) speakers.add(matches.get(0));
            else issues.add("请核对对白发言人：「" + speech.group(1).substring(0, Math.min(22, speech.group(1).length())) + "」");
        }
        // Unquoted older director prose needs the explicit editor instead of guessing from the visible cast.
        if (speakers.isEmpty() && issues.isEmpty() && text.matches("(?s).*(亲口说|继续说|方言说|画外声|画外音)[：:].*"))
            issues.add("旧版对白未标注角色名，请指定本镜发言人");
        return new Speakers(List.copyOf(speakers), false, List.copyOf(issues));
    }

    static Set<String> aliases(ShortDramaCharacter actor) {
        var names = new LinkedHashSet<String>(); names.add(actor.getName());
        String aliases = Objects.toString(actor.getAliases(), "").replaceAll("[\\[\\]\"]", "");
        for (String name : aliases.split("[,，、;；\\n]")) if (!name.isBlank()) names.add(name.trim());
        return names;
    }

    public static void validateReferences(String model, List<Double> durations) {
        if (durations.isEmpty()) return;
        boolean v25 = model.startsWith("bytedance/seedance-2.5/");
        if (!(v25 || model.startsWith("bytedance/seedance-2.0")) || !model.endsWith("/reference-to-video"))
            throw new ShortDramaVoiceException("角色声音需要已配置的 Seedance 2.0 / 2.5 多参考视频模型");
        int count = v25 ? 10 : 3; double seconds = v25 ? 30 : 15;
        if (durations.size() > count) throw new ShortDramaVoiceException("本镜角色样音与手动参考合计超过" + count + "条，请拆镜或选择支持更多音频的模型");
        if (durations.stream().anyMatch(d -> d == null || !Double.isFinite(d) || d < 2 || d > seconds + .1)
            || durations.stream().mapToDouble(Double::doubleValue).sum() > seconds + .1)
            throw new ShortDramaVoiceException("本镜参考音频须至少2秒，单条与合计不能超过" + seconds + "秒，请选用更短样音");
    }
}
