package org.ruoyi.service.shortdrama.impl;

import java.util.List;
import java.util.Objects;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;

/** Resolve explicit costume bindings identically for keyframes and video references. */
final class ShortDramaAppearanceResolver {
    private ShortDramaAppearanceResolver() { }
    static ShortDramaCharacterAppearance resolve(List<ShortDramaCharacterAppearance> variants, String binding) {
        if (variants == null || variants.isEmpty()) return null;
        String key = binding == null ? "" : binding.strip();
        if (key.isEmpty() || key.equals("初始形象")) return variants.get(0);
        for (var item : variants) if (key.equals(item.getChangeReason())) return item;
        var display = java.util.regex.Pattern.compile("^\\[\\s*(\\d+)\\s*]\\s*(.*)$").matcher(key);
        if (display.matches()) {
            int index = Integer.parseInt(display.group(1));
            String name = display.group(2).strip();
            var indexed = variants.stream().filter(item -> Objects.equals(item.getAppearanceIndex(), index)).toList();
            if (indexed.size() == 1 && (name.isEmpty() || name.equals(indexed.get(0).getChangeReason()))) return indexed.get(0);
            throw new IllegalArgumentException("角色形象编号和名称不匹配：" + key);
        }
        String numeric = key.replaceAll("^\\[\\s*|\\s*\\]$", "");
        if (numeric.matches("\\d+")) {
            int index = Integer.parseInt(numeric);
            for (var item : variants) if (Objects.equals(item.getAppearanceIndex(), index)) return item;
        }
        var matches = variants.stream().filter(item -> item.getChangeReason() != null
            && item.getChangeReason().contains(key)).toList();
        if (matches.size() == 1) return matches.get(0);
        throw new IllegalArgumentException("角色形象绑定无法唯一匹配：" + key + "，请使用已存在形象名称或编号");
    }
}
