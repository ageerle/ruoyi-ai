package org.ruoyi.ipd.service.ai;

import java.util.List;
import java.util.Set;
import java.util.Locale;

/**
 * P4-2.1 Provider 派发器（BR-AI-PROV-01：未匹配走 DefaultTester，不阻断）。
 * <p>
 * 构造时按 testers 顺序合并 aliases；查找按"大小写不敏感"匹配。
 * 不存在匹配时：若列表里有 provider=="default" 的则返回它；否则仍返回列表第一个（保底不返 null）。
 */
public final class ProviderRegistry {

    private final List<AiProviderTester> testers;
    private final AiProviderTester fallback;

    public ProviderRegistry(List<AiProviderTester> testers) {
        if (testers == null || testers.isEmpty()) {
            throw new IllegalArgumentException("At least one tester required");
        }
        this.testers = List.copyOf(testers);
        AiProviderTester def = null;
        for (AiProviderTester t : this.testers) {
            if ("default".equalsIgnoreCase(t.provider())) {
                def = t;
                break;
            }
        }
        this.fallback = def != null ? def : this.testers.get(0);
    }

    /**
     * 按 provider 名查 tester。null/空/未匹配 → fallback。
     */
    public AiProviderTester find(String provider) {
        if (provider == null || provider.isBlank()) {
            return fallback;
        }
        String key = provider.trim().toLowerCase(Locale.ROOT);
        for (AiProviderTester t : testers) {
            Set<String> aliases = t.aliases();
            if (aliases == null) {
                continue;
            }
            for (String a : aliases) {
                if (a != null && a.toLowerCase(Locale.ROOT).equals(key)) {
                    return t;
                }
            }
        }
        return fallback;
    }

    public List<AiProviderTester> all() {
        return testers;
    }
}
