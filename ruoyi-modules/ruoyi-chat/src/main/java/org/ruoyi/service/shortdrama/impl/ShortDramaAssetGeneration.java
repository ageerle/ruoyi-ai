package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.util.*;
import java.util.function.BiConsumer;

/** One structured completion. Previews are never authoritative database assets. */
final class ShortDramaAssetGeneration {
    record Result(List<JsonNode> characters, List<JsonNode> locations, List<JsonNode> props) {
        int size() { return characters.size() + locations.size() + props.size(); }
    }
    static String prompt(String script, String tone, String direction) {
        return ShortDramaDirectorSkills.load("visual-world", "character-art-direction")
            + ShortDramaAssetAesthetics.characterReference() + ShortDramaAssetAesthetics.locationReference()
            + direction + "\n剧本风格：" + Objects.toString(tone, "") + "\n" + """
            你是短剧资产制作负责人。一次完成角色、场景、道具提取及它们的出图描述，仅输出一个JSON对象。
            只提取正文中实际出现、需要保持一致的资产，不补写剧情、分镜、摄影规则、运镜或视频指令。
            同名角色和同一地点只列一次，场景描述不能带人物。不要把云、风、光线当成道具。
            角色visualDescription须写清时代身份、年龄体态、五官发型、服装层次及颜色材质，便于独立出图。
            场景descriptions写空间结构、环境、材料和光色；availableSlots是可用站位名。
            道具description写形制、尺寸比例、材料、颜色、连接结构和识别特征，不写持有人或摄影指导。
            组合器物保留完整主体，剧本明确提及且需要单独保持一致的部件也单列，名称不能与主体混淆。
            逐项核对正文中的实体器物，不因已在主体描述中提到就省略独立道具。
            具有操作、固定、承重或连接作用的绳索、绑带、扣件也是道具，不能归入普通衣饰而漏掉。
            所有名称、描述使用中文，描述简洁具体，不复制整段剧本。没有道具时props为空数组。
            字段格式如下（值按剧本填写，不把示例当实际资产）：
            {"characters":[{"name":"角色名","aliases":"","introduction":"身份与作用","roleLevel":"S",
              "gender":"男","ageRange":"成年","personalityTags":"","costumeTier":2,"visualDescription":"独立角色出图描述"}],
             "locations":[{"name":"场景名","summary":"场景用途","hasCrowd":false,"crowdDescription":"",
              "availableSlots":["站位名"],"descriptions":["独立空场景出图描述"]}],
             "props":[{"name":"道具名","description":"独立道具出图描述"}]}
            剧本正文：
            """ + script;
    }
    static Result parse(String raw) {
        if (raw == null) throw new IllegalStateException("资产分析没有返回正文");
        int start = raw.indexOf('{'), end = raw.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalStateException("资产分析没有返回有效JSON，原资产保留");
        try {
            JsonNode root = AtlasMediaSupport.OBJECT_MAPPER.readTree(raw.substring(start, end + 1));
            var characters = list(root, "characters", true, 30);
            var locations = list(root, "locations", true, 40);
            var props = list(root, "props", false, 30);
            for (var item : characters) text(item, "visualDescription", 12000);
            for (var item : locations) {
                JsonNode descriptions = item.path("descriptions");
                if (!descriptions.isArray() || descriptions.isEmpty()) throw new IllegalStateException("场景缺少出图描述");
                for (var description : descriptions)
                    if (!description.isTextual() || description.asText().isBlank() || description.asText().length() > 12000)
                        throw new IllegalStateException("场景出图描述无效");
            }
            for (var item : props) text(item, "description", 12000);
            return new Result(characters, locations, props);
        } catch (java.io.IOException e) { throw new IllegalStateException("资产清单JSON不完整，原资产保留", e); }
    }
    private static List<JsonNode> list(JsonNode root, String field, boolean required, int maximum) {
        JsonNode array = root.path(field);
        if (!array.isArray() || (required && array.isEmpty()) || array.size() > maximum)
            throw new IllegalStateException(field + "清单无效，原资产保留");
        var unique = new LinkedHashMap<String, JsonNode>();
        for (var item : array) {
            String name = text(item, "name", 80);
            // Validate every original item before de-duplicating, including repeated names.
            if ("characters".equals(field)) text(item, "visualDescription", 12000);
            if ("props".equals(field)) text(item, "description", 12000);
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), item);
        }
        return List.copyOf(unique.values());
    }
    private static String text(JsonNode item, String field, int maximum) {
        JsonNode value = item.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > maximum)
            throw new IllegalStateException("资产字段" + field + "缺少有效内容");
        return value.asText().trim();
    }
    /** Scan each incoming character once; nested arrays and quoted braces do not end an asset. */
    static final class Preview {
        private final BiConsumer<Integer, JsonNode> sink;
        private final StringBuilder buffer = new StringBuilder(), string = new StringBuilder();
        private int objects, arrays, start = -1, category;
        private boolean quoted, escape;
        private String lastString = "";
        Preview(BiConsumer<Integer, JsonNode> sink) { this.sink = sink; }
        synchronized void accept(String chunk) {
            if (buffer.length() + chunk.length() > 500_000) return;
            for (char c : chunk.toCharArray()) {
                int index = buffer.length(); buffer.append(c);
                if (quoted) {
                    if (escape) { escape = false; string.append(c); }
                    else if (c == '\\') { escape = true; string.append(c); }
                    else if (c == '"') { quoted = false; lastString = string.toString(); }
                    else string.append(c);
                    continue;
                }
                if (c == '"') { quoted = true; string.setLength(0); }
                else if (c == '[') {
                    if (objects == 1 && arrays == 0)
                        category = switch (lastString) { case "characters" -> 1; case "locations" -> 2; case "props" -> 3; default -> 0; };
                    arrays++;
                } else if (c == ']') { arrays--; if (arrays == 0) category = 0; }
                else if (c == '{') { if (objects == 1 && arrays == 1 && category != 0) start = index; objects++; }
                else if (c == '}') {
                    objects--;
                    if (objects == 1 && arrays == 1 && category != 0 && start >= 0) {
                        try { sink.accept(category, AtlasMediaSupport.OBJECT_MAPPER.readTree(buffer.substring(start, index + 1))); }
                        catch (java.io.IOException ignored) { /* Full response validation decides success. */ }
                        start = -1;
                    }
                }
            }
        }
    }
}
