package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaAppearanceResolverTest {
    private ShortDramaCharacterAppearance appearance(int index, String name) {
        var row = new ShortDramaCharacterAppearance(); row.setAppearanceIndex(index); row.setChangeReason(name); return row;
    }
    @Test void resolvesDisplayLabelsUsedByThePlannerWithoutChangingCostume() {
        var initial = appearance(0, "初始形象"); var next = appearance(1, "雨中蓑衣"); var rows = List.of(initial, next);
        assertSame(initial, ShortDramaAppearanceResolver.resolve(rows, "[0]初始形象"));
        assertSame(next, ShortDramaAppearanceResolver.resolve(rows, "[1] 雨中蓑衣"));
        assertSame(next, ShortDramaAppearanceResolver.resolve(rows, "[1]"));
        assertSame(next, ShortDramaAppearanceResolver.resolve(rows, "1"));
    }
    @Test void refusesConflictingIndexAndNameOrMissingVariant() {
        var rows = List.of(appearance(0, "初始形象"), appearance(1, "雨中蓑衣"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAppearanceResolver.resolve(rows, "[0]雨中蓑衣"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAppearanceResolver.resolve(rows, "[2]初始形象"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAppearanceResolver.resolve(rows, "[1]未知服装"));
    }
}
