package org.ruoyi.service.shortdrama.impl;

import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Versioned, bundled production skills. Loaded by default, independent of tool calling. */
public final class ShortDramaDirectorSkills {
    private ShortDramaDirectorSkills() {}
    public static final String VERSION = "director-emotion-v2";
    private static final Map<String,String> CACHE = new ConcurrentHashMap<>();
    public static String load(String... names) {
        StringBuilder result = new StringBuilder("【默认导演业务规范：在保持输出格式和用户意图的前提下优先遵循】\n");
        for (String name : names) result.append(CACHE.computeIfAbsent(name, ShortDramaDirectorSkills::read)).append("\n");
        return result.toString();
    }
    static String read(String name) {
        if (!name.matches("[a-z-]+")) throw new IllegalArgumentException("Invalid skill name");
        // Common-pool threads may not inherit Spring Boot's nested-jar context loader.
        try (var in = new ClassPathResource("short-drama/skills/"+name+"/SKILL.md",
            ShortDramaDirectorSkills.class.getClassLoader()).getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("---")) text = text.substring(text.indexOf("---",3)+3).strip();
            if (text.isBlank()) throw new IllegalStateException("Empty director skill: "+name);
            return "[skill:"+name+"@"+VERSION+"]\n"+text;
        } catch (Exception e) { throw new IllegalStateException("Cannot load director skill: "+name,e); }
    }
}
