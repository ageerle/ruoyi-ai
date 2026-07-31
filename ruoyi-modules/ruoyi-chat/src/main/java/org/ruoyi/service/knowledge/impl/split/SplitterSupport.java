package org.ruoyi.service.knowledge.impl.split;

import org.ruoyi.service.knowledge.DocumentSplitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Shared, deterministic split pipeline used by every supported document format. */
public final class SplitterSupport {
    private SplitterSupport() {}

    public static List<String> split(String content, DocumentSplitConfig config,
                                     Function<String, List<String>> naturalSections) {
        if (content == null || content.isBlank()) return List.of();
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> primary = literalSections(normalized, config.separator());
        List<String> result = new ArrayList<>();
        for (String part : primary) {
            List<String> sections = naturalSections.apply(part);
            String joined = sections.stream().filter(s -> s != null && !s.isBlank())
                .map(String::strip).reduce((a, b) -> a + "\n\n" + b).orElse("");
            result.addAll(slidingWindow(joined, config.blockSize(), config.overlap()));
        }
        return result;
    }

    static List<String> literalSections(String content, String separator) {
        if (separator == null || separator.isEmpty() || !content.contains(separator)) return List.of(content);
        List<String> parts = new ArrayList<>();
        for (String part : content.split(Pattern.quote(separator), -1)) {
            if (!part.isBlank()) parts.add(part);
        }
        return parts;
    }

    /** blockSize is a strict maximum; overlap is the repeated suffix/prefix length. */
    public static List<String> slidingWindow(String content, int blockSize, int overlap) {
        if (content == null || content.isBlank()) return List.of();
        String value = content.strip();
        List<String> chunks = new ArrayList<>();
        int step = blockSize - overlap;
        for (int start = 0; start < value.length(); start += step) {
            int end = Math.min(value.length(), start + blockSize);
            String chunk = value.substring(start, end).strip();
            if (!chunk.isEmpty()) chunks.add(chunk);
            if (end == value.length()) break;
        }
        return chunks;
    }

    /** Compatibility helper retained for callers/tests; now enforces strict maximum size. */
    public static List<String> mergeAndSplit(String[] sections, int blockSize, int overlap) {
        return split(String.join("\n\n", sections),
            new DocumentSplitConfig(null, blockSize, overlap, ""),
            text -> List.of(text));
    }

    static List<String> paragraphs(String content) {
        return List.of(content.split("\\n\\s*\\n+"));
    }

    static List<String> lines(String content) {
        return List.of(content.split("\\n+"));
    }
}
