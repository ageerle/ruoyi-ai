package org.ruoyi.service.shortdrama.impl;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import java.util.*;

/** Only short-drama templates and skills; never reads the generic agent or personal skill directories. */
@Service @RequiredArgsConstructor
public class ShortDramaSkillMarket {
    private final ShortDramaSkillCatalog catalog;
    public record Entry(String name, String title, String type, String description, boolean enabled,
                        String version, String body, List<ShortDramaSkillCatalog.SkillFile> files) {}

    public List<Entry> list() {
        List<Entry> result = new ArrayList<>();
        ShortDramaServiceImpl.systemPromptCatalog().forEach((title, body) -> result.add(new Entry(
            "system-" + DigestUtil.sha256Hex(title).substring(0, 16), title, "system", "当前短剧制作流程使用的系统提示词", true,
            DigestUtil.sha256Hex(body), body, List.of())));
        try {
            var resolver = new PathMatchingResourcePatternResolver(ShortDramaDirectorSkills.class.getClassLoader());
            var resources = resolver.getResources("classpath*:short-drama/skills/*/SKILL.md");
            Arrays.sort(resources, Comparator.comparing(resource -> resource.getDescription()));
            Set<String> seen = new HashSet<>();
            for (var resource : resources) {
                String url = resource.getURL().toString();
                String name = url.substring(url.indexOf("short-drama/skills/") + "short-drama/skills/".length()).split("/")[0];
                if (!seen.add(name)) continue;
                String body = ShortDramaDirectorSkills.read(name);
                String title = body.lines().filter(line -> line.startsWith("# ")).findFirst().orElse(name).replaceFirst("^# +", "");
                result.add(new Entry(name, title, "production", "默认短剧业务技能", true,
                    DigestUtil.sha256Hex(body), body, List.of()));
            }
        } catch (Exception error) { throw new IllegalStateException("读取默认短剧技能失败", error); }
        for (var option : catalog.list(null, true)) {
            var skill = catalog.detail(option.name());
            result.add(new Entry(skill.name(), skill.title(), skill.type(), skill.description(), skill.enabled(),
                skill.version(), skill.body(), skill.files()));
        }
        return result;
    }
}
