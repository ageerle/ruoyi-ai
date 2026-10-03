package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;

import java.util.*;
import java.util.regex.Pattern;

/** Timing repair keeps actual speech capacity while allowing concurrent actions and motivated shot merges. */
final class ShortDramaSceneBudget {
    private ShortDramaSceneBudget() {}
    private static final Pattern BUDGET = Pattern.compile("预计\\s*(\\d+(?:\\.\\d+)?)\\s*秒");

    static String scaffold(String scene) {
        return scaffold(scene, 1);
    }
    static String scaffold(String scene, int minimum) {
        var match = BUDGET.matcher(scene.lines().findFirst().orElse(""));
        if (!match.find()) return "";
        int seconds = (int)Math.round(Double.parseDouble(match.group(1)));
        if (seconds < 1) return "";
        return "\n【本场时长参考】剧本预计" + seconds
            + "秒，仅作制作参考。按原对白、连续动作与叙事节奏安排自然分段，允许估算差异；不强制镜数、最短整数秒或总和，不靠删对白、增加事件或空等凑预算。\n";
    }

    static String repairPrompt(String scene, List<StoryboardPanelData> panels, String error) {
        return repairPrompt(scene, panels, error, 1);
    }
    static String repairPrompt(String scene, List<StoryboardPanelData> panels, String error, int minimum) {
        ArrayNode compact = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < panels.size(); i++) {
            var p = panels.get(i); var n = compact.addObject(); n.put("from_panel", i + 1);
            n.put("source_text", Objects.toString(p.getSourceText(), "")); n.put("duration", p.getDuration());
            n.put("location", Objects.toString(p.getLocation(), ""));
            n.put("start_state", Objects.toString(p.getStartState(), "")); n.put("end_state", Objects.toString(p.getEndState(), ""));
            if (p.getBackgroundExtras() != null) n.put("background_extras", p.getBackgroundExtras());
            if (p.getTiming() != null) n.set("timing", p.getTiming());
            if (p.getShotDesign() != null) n.set("shot_design", p.getShotDesign());
        }
        return ShortDramaDirectorSkills.load("emotional-dialogue", "cinematic-storyboard", "director-blocking", "video-prompt") + scaffold(scene, minimum)
            + "只修本场可拍节拍表，返回JSON数组。每项必须含：from_panel（原镜序号，拆镜可重复；合并同空间连续镜头时改用from_panels整数数组）、source_text、description、start_state、end_state、performance_beats、duration（明确分配的整数秒）、timing。\n"
            + "原镜含shot_design时修订项也必须含完整shot_design；拆镜或合镜后重写实际焦点与切点，连续镜state_in复制前镜state_out、cut_in复制前镜cut_out，不复制同一个动作从头重复。\n"
            + "shot_design须明确包含focus、viewer_gain、framing、axis、movement、motivation、transition、cut_in、cut_out；连续组首项另含非空state_in，每项含state_delta。状态是对象，不得只返回拍法而遗漏状态。首态锚点覆盖下方原规划全组已有属性，初态据原文明确填写；state_delta仅列本项真实末态变化，不新增键。\n"
            + "本场每个JSON项就是一次视频生成，段内允许有动机的摄影切点。先按原事件的因果与真实容量安排连贯表演，再决定自然分段；不要逐句、逐反应列项，也不按总秒数除以段长硬算镜数。合并项重写segment_goal、segment_result、bridge_in、bridge_out、beat_type、narrative_cause、character_goal、next_hook，结尾直接收束。\n"
            + "先逐句抄回原话并注明原发言人、画内/画外/心念等原声源，再安排节拍。保留字句、先后和声源，不把别人的台词交给另一角色，不重复整句来凑镜头。长句可在自然句界跨镜拆开。\n"
            + "source_text保留段落换行；对白逐句独占一行，统一为角色名：「原句」。动作、字幕与文书文字必须另起行，不得跟在对白冒号后；叙述性冒号后换行，不能把动作文字误当发音延长镜头。\n"
            + "timing完整填写spoken_text（只含本镜发音）、speech_rate（2至5字/秒）、action_seconds、pause_seconds、action_note；较长非对白时段补pacing_note，说明逐段具体事件与叙事必要性，不为达到总分钟数虚填动作/停顿。"
            + "duration填写正整数制作估算，不强制最短整数秒或固定段长。结合对白、并行动作与必要反应自然安排，不因公式估算差异强制拆镜；不要删改原对白或增添事件来凑数。\n"
            + "说话时转头、指向物件、听者回应眼神属于并行动作，不额外累计action_seconds；只有必须停止说话的交接、落笔等动作才独占。"
            + "倾听对方正在说话不再记pause_seconds，停顿按真实表演需要逐项决定，静默镜speech_rate仍填4，不能0。\n"
            + "超总预算时先把同一空间同一因果的对白与同时进行动作合并；同一段可让不同人物依次说话，口型与各自声源对应，不同时抢声。"
            + "from_panels不能合并不同场景或同角色不同服装形象；合并后start_state承接首项、end_state落实最后项，并为每名发言/倾听者保留可见反应。\n"
            + "输出前保留全部原话及先后关系，核对真实表演节奏；总时长可以随内容调整，不要求凑齐剧本估时。\n"
            + "世界观与全角色库不能增加本场未安排的演员、精灵或投影；background_extras保持匿名群演原有身份/服饰，不升级为角色ID；若拆镜改变0秒人数/位置，按该镜start_state明确重写该字段。地点精确绑定不合并新名字。\n"
            + "background_extras无人时写空字符串；有人时严格写成“0秒：可见人数=6-8；身份服饰=匿名守卒，粗布军服；位置=门板内侧；姿态动作=肩抵门板”，人数使用整数或整数范围，不写“无”“若干”或数组。\n"
            + "不伸长无事件的静默来填预算，允许上述预算容差，动作与反应必须推进原事件。\n"
            + "原文：\n" + scene + "\n失败原因：" + error + "\n当前节拍：\n" + compact;
    }

    static List<StoryboardPanelData> applyRepair(List<StoryboardPanelData> originals, String response) {
        return applyRepair(originals, response, 1);
    }
    static List<StoryboardPanelData> applyRepair(List<StoryboardPanelData> originals, String response, int minimumShotSeconds) {
        var mapper = AtlasMediaSupport.OBJECT_MAPPER;
        try {
            JsonNode edits = mapper.readTree(response.strip().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", ""));
            if (!edits.isArray() || edits.isEmpty()) throw new IllegalArgumentException("时间表须为非空数组");
            List<StoryboardPanelData> repaired = new ArrayList<>();
            for (JsonNode edit : edits) {
                List<Integer> indices = new ArrayList<>();
                if (edit.has("from_panels")) {
                    if (!edit.path("from_panels").isArray() || edit.path("from_panels").isEmpty()) throw new IllegalArgumentException("合并原镜序号须为非空数组");
                    for (JsonNode index : edit.path("from_panels")) {
                        if (!index.isIntegralNumber()) throw new IllegalArgumentException("合并原镜序号须为整数");
                        indices.add(index.asInt());
                    }
                    if (new HashSet<>(indices).size() != indices.size()) throw new IllegalArgumentException("合并原镜序号不能重复");
                } else {
                    if (!edit.path("from_panel").isIntegralNumber()) throw new IllegalArgumentException("时间表原镜序号须为整数");
                    indices.add(edit.path("from_panel").asInt());
                }
                if (indices.stream().anyMatch(n -> n < 1 || n > originals.size())) throw new IllegalArgumentException("时间表原镜序号无效");
                var panel = mapper.convertValue(mapper.valueToTree(originals.get(indices.get(0) - 1)), StoryboardPanelData.class);
                mergeReferences(panel, indices.stream().map(n -> originals.get(n - 1)).toList());
                if (!edit.hasNonNull("source_text") || !edit.path("timing").isObject()) throw new IllegalArgumentException("时间表缺少原话或时间预算");
                panel.setSourceText(edit.path("source_text").asText()); panel.setTiming(edit.path("timing"));
                panel.setDescription(edit.path("description").asText(panel.getDescription()));
                if (edit.hasNonNull("segment_goal")) panel.setSegmentGoal(edit.path("segment_goal").asText());
                if (edit.hasNonNull("segment_result")) panel.setSegmentResult(edit.path("segment_result").asText());
                if (edit.hasNonNull("bridge_in")) panel.setBridgeIn(edit.path("bridge_in").asText());
                if (edit.hasNonNull("bridge_out")) panel.setBridgeOut(edit.path("bridge_out").asText());
                if (edit.hasNonNull("beat_type")) panel.setBeatType(edit.path("beat_type").asText());
                if (edit.hasNonNull("narrative_cause")) panel.setNarrativeCause(edit.path("narrative_cause").asText());
                if (edit.hasNonNull("character_goal")) panel.setCharacterGoal(edit.path("character_goal").asText());
                if (edit.hasNonNull("next_hook")) panel.setNextHook(edit.path("next_hook").asText());
                panel.setStartState(edit.path("start_state").asText(panel.getStartState()));
                panel.setEndState(edit.path("end_state").asText(panel.getEndState()));
                if (panel.getShotDesign() != null && !panel.getShotDesign().isNull() && !edit.path("shot_design").isObject())
                    throw new IllegalArgumentException("专业镜头时间修订须保留shot_design并重新核对跨镜接点");
                if (edit.path("shot_design").isObject()) {
                    var design = edit.path("shot_design").deepCopy();
                    retainKnownStates(design, indices.stream().map(n -> originals.get(n - 1)).toList());
                    panel.setShotDesign(design);
                }
                if (edit.has("background_extras")) {
                    if (!edit.path("background_extras").isTextual()) throw new IllegalArgumentException("background_extras须为0秒匿名群演文字");
                    ShortDramaBackgroundExtras.parse(edit.path("background_extras").asText());
                    panel.setBackgroundExtras(edit.path("background_extras").asText());
                }
                panel.setStoryAction(panel.getDescription()); panel.setStoryResult(panel.getEndState());
                panel.setContinuityAction("从" + panel.getStartState() + "连续执行至" + panel.getEndState());
                if (edit.path("performance_beats").isArray()) panel.setPerformanceBeats(edit.path("performance_beats"));
                var continuity = JsonNodeFactory.instance.objectNode(); continuity.set("timing", panel.getTiming());
                int minimum = Math.max(minimumShotSeconds, (int)Math.ceil(ShortDramaTiming.requiredSeconds(panel.getSourceText(), continuity.toString())));
                int duration = minimum;
                if (edit.has("duration")) {
                    if (!edit.path("duration").isIntegralNumber()) throw new IllegalArgumentException("duration须为整数秒");
                    duration = edit.path("duration").asInt();
                }
                if (duration <= 0) throw new IllegalArgumentException("duration须为正整数秒");
                panel.setDuration(duration); panel.setPanelNumber(repaired.size() + 1); panel.setSegmentNumber(repaired.size() + 1);
                repaired.add(panel);
            }
            return repaired;
        } catch (Exception e) { throw new IllegalArgumentException("时间表修订无效：" + e.getMessage(), e); }
    }

    private static void mergeReferences(StoryboardPanelData panel, List<StoryboardPanelData> originals) {
        Map<String, ShortDramaServiceImpl.CharacterRef> refs = new LinkedHashMap<>();
        Set<String> present = new LinkedHashSet<>();
        for (var original : originals) {
            if (!Objects.equals(panel.getLocation(), original.getLocation())) throw new IllegalArgumentException("不同场景不能合并为一个镜头");
            for (var ref : original.getCharacters() == null ? List.<ShortDramaServiceImpl.CharacterRef>of() : original.getCharacters()) {
                var before = refs.putIfAbsent(ref.getName(), ref);
                if (before != null && !Objects.equals(before.getAppearance(), ref.getAppearance())) throw new IllegalArgumentException("同角色不同服装形象不能合并");
            }
            if (original.getPresentCharacters() != null) present.addAll(original.getPresentCharacters());
        }
        panel.setCharacters(new ArrayList<>(refs.values())); panel.setPresentCharacters(new ArrayList<>(present));
    }

    /** Missing contract fields retain only explicitly recorded source states; unknown initial values stay unknown. */
    private static void retainKnownStates(JsonNode revised, List<StoryboardPanelData> originals) {
        if (!(revised instanceof com.fasterxml.jackson.databind.node.ObjectNode design)) return;
        var first = originals.get(0).getShotDesign();
        if (!design.has("state_in") && first != null && first.path("state_in").isObject())
            design.set("state_in", first.path("state_in").deepCopy());
        if (design.has("state_delta") || design.has("state_out")) return;
        var delta = design.putObject("state_delta");
        for (var original : originals) {
            var recorded = original.getShotDesign();
            if (recorded == null) continue;
            if (recorded.path("state_delta").isObject())
                recorded.path("state_delta").fields().forEachRemaining(entry -> delta.set(entry.getKey(), entry.getValue().deepCopy()));
            else if (recorded.path("state_out").isObject())
                recorded.path("state_out").fields().forEachRemaining(entry -> {
                    if (!entry.getValue().equals(design.path("state_in").path(entry.getKey())))
                        delta.set(entry.getKey(), entry.getValue().deepCopy());
                });
        }
    }
}
