package org.ruoyi.service.knowledge.impl.split;

import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownTextSplitter implements TextSplitter {
    @Override
    public List<String> split(String content, DocumentSplitConfig config) {
        return SplitterSupport.split(content, config, this::sections);
    }

    /** Split headings without treating heading-looking lines inside fenced code as headings. */
    private List<String> sections(String markdown) {
        String[] lines = markdown.split("\\n", -1);
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean fenced = false;
        String fenceMarker = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                String marker = trimmed.substring(0, 3);
                if (!fenced) {
                    fenced = true;
                    fenceMarker = marker;
                } else if (marker.equals(fenceMarker)) {
                    fenced = false;
                    fenceMarker = null;
                }
            }
            boolean atx = !fenced && line.matches("^#{1,6}(\\s+.*)?$");
            boolean setextTitle = !fenced && i + 1 < lines.length
                && lines[i + 1].matches("^\\s*(=+|-+)\\s*$") && !line.isBlank();
            if ((atx || setextTitle) && current.length() > 0) flush(sections, current);
            current.append(line);
            if (i < lines.length - 1) current.append('\n');
            if (setextTitle) {
                current.append(lines[++i]);
                if (i < lines.length - 1) current.append('\n');
            }
        }
        flush(sections, current);
        return sections;
    }

    private static void flush(List<String> sections, StringBuilder current) {
        if (!current.toString().isBlank()) sections.add(current.toString());
        current.setLength(0);
    }
}
