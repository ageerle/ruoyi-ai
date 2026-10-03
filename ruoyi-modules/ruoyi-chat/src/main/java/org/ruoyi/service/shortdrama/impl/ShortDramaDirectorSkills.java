package org.ruoyi.service.shortdrama.impl;

import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Versioned, bundled production skills. Loaded by default, independent of tool calling. */
public final class ShortDramaDirectorSkills {
    private ShortDramaDirectorSkills() {}
    public static final String VERSION = "director-video-prompt-v11";
    public static final String PACING_VERSION = "content-pacing-advisory-v2";
    public static final String PLANNING_VERSION = "semantic-segment-advisory-v12";
    private static final Map<String,String> CACHE = new ConcurrentHashMap<>();
    public static String load(String... names) {
        StringBuilder result = new StringBuilder("【默认导演业务规范：在保持输出格式和用户意图的前提下优先遵循】\n");
        for (String name : names) result.append(CACHE.computeIfAbsent(name, ShortDramaDirectorSkills::read)).append("\n");
        return result.toString();
    }
    /** Only remove an exact duplicate in the selected-skill section. Edited bindings and references stay intact. */
    static String compactSelected(String direction, String... loadedNames) {
        for (String name : loadedNames) {
            int marker = direction.indexOf("[selected-skill:" + name + "@");
            if (marker < 0) continue;
            String rule = CACHE.computeIfAbsent(name, ShortDramaDirectorSkills::read);
            String body = rule.substring(rule.indexOf('\n') + 1).strip();
            int start = direction.indexOf(body, marker);
            int next = direction.indexOf("[selected-skill:", marker + 1);
            if (start >= 0 && (next < 0 || start < next))
                direction = direction.substring(0, start) + "[正文与本次默认技能完全一致，复用已加载正文；选中版本与附属参考仍生效]" + direction.substring(start + body.length());
        }
        return direction;
    }
    /**
     * Media models receive the concrete asset/shot instruction plus versioned
     * bindings. Full skill handbooks belong to planning and review prompts; when
     * repeated inside every image request they obscure the actual subject.
     */
    public static String media(String... names) {
        StringBuilder result = new StringBuilder("【媒体执行规则版本】\n");
        for (String name : names) {
            String loaded = CACHE.computeIfAbsent(name, ShortDramaDirectorSkills::read);
            int lineEnd = loaded.indexOf('\n');
            result.append(lineEnd < 0 ? loaded : loaded.substring(0, lineEnd)).append('\n');
        }
        return result.append("技能正文已在规划和审阅阶段应用；本次以当前资产或镜头的具体描述为可见内容。"
            + "人物身份与数量、时点、已批准参考及用户明确修订优先，不从技能手册增添人物、道具、年代或剧情。\n").toString();
    }
    static String read(String name) {
        if (!name.matches("[a-z-]+")) throw new IllegalArgumentException("Invalid skill name");
        // Common-pool threads may not inherit Spring Boot's nested-jar context loader.
        try (var in = new ClassPathResource("short-drama/skills/"+name+"/SKILL.md",
            ShortDramaDirectorSkills.class.getClassLoader()).getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("---")) text = text.substring(text.indexOf("---",3)+3).strip();
            if (text.isBlank()) throw new IllegalStateException("Empty director skill: "+name);
            String version = "video-prompt".equals(name) ? VERSION + "-" + PACING_VERSION
                : "cinematic-storyboard".equals(name) ? VERSION + "-" + PLANNING_VERSION : VERSION;
            return "[skill:"+name+"@"+version+"]\n"+text;
        } catch (Exception e) { throw new IllegalStateException("Cannot load director skill: "+name,e); }
    }
}
