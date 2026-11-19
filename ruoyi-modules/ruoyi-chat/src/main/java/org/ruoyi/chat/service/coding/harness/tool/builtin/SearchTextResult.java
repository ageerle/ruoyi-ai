package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.util.List;

/** Bounded search response; engine is either ripgrep or java. */
public record SearchTextResult(
    String query,
    boolean regex,
    String glob,
    String engine,
    List<SearchMatch> matches,
    boolean truncated
) {
    public SearchTextResult {
        matches = List.copyOf(matches);
    }
}
