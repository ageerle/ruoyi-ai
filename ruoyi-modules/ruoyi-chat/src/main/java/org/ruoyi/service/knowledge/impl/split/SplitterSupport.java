package org.ruoyi.service.knowledge.impl.split;

import java.util.ArrayList;
import java.util.List;

/**
 * 分片工具：提供各 Splitter 共用的滑动窗口切分与片段合并能力
 */
public final class SplitterSupport {

    private SplitterSupport() {
    }

    /**
     * 滑动窗口切分：每块约 blockSize 字符，相邻块保留 overlap 字符重叠
     */
    public static List<String> slidingWindow(String content, int blockSize, int overlap) {
        List<String> chunkList = new ArrayList<>();
        int len = content.length();
        int right = 0;
        int i = 0;
        while (len > right) {
            int begin = i * blockSize - overlap;
            if (begin < 0) {
                begin = 0;
            }
            int end = blockSize * (i + 1) + overlap;
            if (end > len) {
                end = len;
            }
            String chunk = content.substring(begin, end).trim();
            if (!chunk.isEmpty()) {
                chunkList.add(chunk);
            }
            i++;
            right = right + blockSize;
        }
        return chunkList;
    }

    /**
     * 将小段按顺序合并到不超过 blockSize，超过 blockSize 的单段再用滑动窗口切分
     */
    public static List<String> mergeAndSplit(String[] sections, int blockSize, int overlap) {
        List<String> chunkList = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String section : sections) {
            if (section == null || section.isBlank()) {
                continue;
            }
            if (section.length() > blockSize) {
                // 超长段先冲刷当前缓冲，再单独窗口切分
                if (current.length() > 0) {
                    chunkList.add(current.toString().trim());
                    current.setLength(0);
                }
                chunkList.addAll(slidingWindow(section, blockSize, overlap));
            } else if (current.length() + section.length() > blockSize) {
                chunkList.add(current.toString().trim());
                current.setLength(0);
                current.append(section);
            } else {
                current.append(section);
            }
        }
        if (current.length() > 0) {
            chunkList.add(current.toString().trim());
        }
        return chunkList;
    }
}
