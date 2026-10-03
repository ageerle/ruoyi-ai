package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;
import java.util.*;

/** Repairs only physical state contracts; the approved scene plan is never regenerated. */
final class ShortDramaShotContractRepair {
    private ShortDramaShotContractRepair() {}

    static String prompt(String scene, List<StoryboardPanelData> panels, String issue) {
        var context = AtlasMediaSupport.OBJECT_MAPPER.createArrayNode();
        for (var panel : panels) {
            var node = context.addObject();
            node.put("panel_number", panel.getPanelNumber());
            node.put("description", panel.getDescription());
            node.put("start_state", panel.getStartState());
            node.put("end_state", panel.getEndState());
            node.set("shot_design", panel.getShotDesign());
        }
        var required = segmentKeys(panels);
        var template = AtlasMediaSupport.OBJECT_MAPPER.createArrayNode();
        for (var panel : panels) {
            var patch = template.addObject();
            patch.put("panel_number", panel.getPanelNumber());
            if (required.containsKey(panel.getPanelNumber())) {
                var initial = patch.putObject("state_in");
                for (String key : required.get(panel.getPanelNumber()))
                    initial.put(key, panel.getShotDesign().path("state_in").path(key).asText(""));
            }
            patch.putObject("state_delta");
        }
        return """
            仅校对现有专业分镜的物理状态契约，不重写剧情、镜号、时长、构图、轴线、运镜或交镜动作。
            输出JSON数组，每个镜号恰好一次：[{"panel_number":1,"state_in":{...},"state_delta":{...}},
            {"panel_number":2,"state_delta":{...}}]。只允许这三个字段。
            opening、time_change、space_change镜必须明确state_in；continuous镜禁止state_in，后台逐字继承前镜末态与交镜接点。
            每个连续段的初始state_in必须预先包含全段所有state_delta所用的键，并给出原文支持的真实初态。
            在提交前扫描后续所有delta键；若如人物视线、手部动作、器物受力、气流、声音会发生变化，须在该段首镜初态写明。
            下方已计算每段首镜必须覆盖的准确锚点清单。首镜state_in必须逐字包含该段清单所有键，不能遗漏、改名或添加新键。
            下方给出可直接填写的JSON骨架。保留所有镜号与键名；state_in的空字符串须根据原文补为具体初态，不得删除这一项。已有文字只有与原文矛盾时才更正。state_delta仍须填写本镜真实变化，不能把所有变化改成空对象。
            每镜state_delta只能选用该段清单中的准确键，禁止把神态改写为表情等近义键。没有变化写{}。
            为清单每个键从原文和原规划写出实际初态，不能只处理错误提示中的一个键。不新增原文没有的动作、姿态或器物。
            每镜delta只写该镜结束时真实改变的已有属性；不写state_out，后台按继承状态加delta展开后仍严格校验。
            当前问题：
            """ + issue + "\n每段首镜的完整准确锚点清单：\n" + required + "\n完整JSON填写骨架：\n" + template + "\n本场原文：\n" + scene + "\n待校对的原规划（全部非状态字段将原样保留）：\n" + context;
    }

    static Map<Integer, Set<String>> segmentKeys(List<StoryboardPanelData> panels) {
        var segments = new LinkedHashMap<Integer, Set<String>>();
        Set<String> current = null;
        for (var panel : panels) {
            var design = panel.getShotDesign();
            if (design == null || !design.isObject()) throw new ShortDramaShotDesign.InvalidDesign("状态校对缺少原镜头契约");
            if (current == null || !"continuous".equals(design.path("transition").asText())) {
                current = new LinkedHashSet<>(); segments.put(panel.getPanelNumber(), current);
            }
            for (String field : List.of("state_in", "state_out", "state_delta")) {
                var state = design.path(field);
                if (state.isObject()) state.fieldNames().forEachRemaining(current::add);
            }
        }
        return segments;
    }

    static List<StoryboardPanelData> apply(List<StoryboardPanelData> panels, JsonNode patches) {
        if (patches == null || !patches.isArray() || patches.size() != panels.size())
            throw new ShortDramaShotDesign.InvalidDesign("状态校对须覆盖原镜号且不得增减镜头");
        var byNumber = new HashMap<Integer, JsonNode>();
        for (var patch : patches) {
            if (!patch.isObject() || !patch.path("panel_number").isIntegralNumber())
                throw new ShortDramaShotDesign.InvalidDesign("状态校对缺少原镜号");
            patch.fieldNames().forEachRemaining(key -> {
                if (!Set.of("panel_number", "state_in", "state_delta").contains(key))
                    throw new ShortDramaShotDesign.InvalidDesign("状态校对不能修改其他字段：" + key);
            });
            if (byNumber.put(patch.path("panel_number").asInt(), patch) != null)
                throw new ShortDramaShotDesign.InvalidDesign("状态校对镜号重复");
        }
        var required = segmentKeys(panels);
        var repaired = new ArrayList<StoryboardPanelData>();
        for (var panel : panels) {
            JsonNode patch = byNumber.remove(panel.getPanelNumber());
            if (patch == null || !patch.path("state_delta").isObject())
                throw new ShortDramaShotDesign.InvalidDesign("状态校对缺少原镜号或state_delta");
            var copy = AtlasMediaSupport.OBJECT_MAPPER.convertValue(panel, StoryboardPanelData.class);
            if (!(copy.getShotDesign() instanceof ObjectNode design))
                throw new ShortDramaShotDesign.InvalidDesign("状态校对缺少原镜头契约");
            boolean continuous = "continuous".equals(design.path("transition").asText());
            if (continuous && patch.has("state_in"))
                throw new ShortDramaShotDesign.InvalidDesign("连续镜状态校对须继承前镜末态");
            if (!continuous && !patch.path("state_in").isObject())
                throw new ShortDramaShotDesign.InvalidDesign("新连续段状态校对缺少明确初态");
            if (required.containsKey(panel.getPanelNumber())) {
                var returnedKeys = new HashSet<String>(); patch.path("state_in").fieldNames().forEachRemaining(returnedKeys::add);
                if (!returnedKeys.equals(required.get(panel.getPanelNumber()))) {
                    var missing = new TreeSet<>(required.get(panel.getPanelNumber())); missing.removeAll(returnedKeys);
                    var extra = new TreeSet<>(returnedKeys); extra.removeAll(required.get(panel.getPanelNumber()));
                    throw new ShortDramaShotDesign.InvalidDesign("镜头" + panel.getPanelNumber() + "连续状态校对未完成：缺少锚点" + missing + "；多出锚点" + extra);
                }
            }
            design.remove(List.of("state_in", "state_out", "state_delta"));
            if (continuous) design.remove("cut_in");
            else design.set("state_in", patch.get("state_in").deepCopy());
            design.set("state_delta", patch.get("state_delta").deepCopy());
            repaired.add(copy);
        }
        if (!byNumber.isEmpty()) throw new ShortDramaShotDesign.InvalidDesign("状态校对包含未知镜号");
        ShortDramaShotDesign.expandDeltas(repaired);
        ShortDramaShotDesign.validate(repaired);
        return repaired;
    }
}
