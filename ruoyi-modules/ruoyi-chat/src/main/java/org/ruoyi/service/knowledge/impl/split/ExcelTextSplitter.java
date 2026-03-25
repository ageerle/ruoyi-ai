package org.ruoyi.service.knowledge.impl.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ExcelTextSplitter implements TextSplitter {

    private final IKnowledgeInfoService knowledgeInfoService;

    @Override
    public List<String> split(String content, String kid) {
        // 默认配置
        String knowledgeSeparator = "#";
        int textBlockSize = 1000;
        int overlapChar = 50;

        // 根据知识库ID查询配置，覆盖默认值
        if (StringUtils.isNotBlank(kid)) {
            try {
                KnowledgeInfoVo info = knowledgeInfoService.queryById(Long.parseLong(kid));
                if (info != null) {
                    if (StringUtils.isNotBlank(info.getSeparator())) {
                        knowledgeSeparator = info.getSeparator();
                    }
                    if (info.getTextBlockSize() != null && info.getTextBlockSize() > 0) {
                        textBlockSize = info.getTextBlockSize().intValue();
                    }
                    if (info.getOverlapChar() != null && info.getOverlapChar() > 0) {
                        overlapChar = info.getOverlapChar().intValue();
                    }
                }
            } catch (Exception e) {
                log.warn("查询知识库配置失败，使用默认配置, kid={}", kid, e);
            }
        }
        List<String> chunkList = new ArrayList<>();
        if (content.contains(knowledgeSeparator) && StringUtils.isNotBlank(knowledgeSeparator)) {
            // 按自定义分隔符切分
            String[] chunks = content.split(knowledgeSeparator);
            chunkList.addAll(Arrays.asList(chunks));
        } else {
            int indexMin = 0;
            int len = content.length();
            int i = 0;
            int right = 0;
            while (true) {
                if (len > right) {
                    int begin = i * textBlockSize - overlapChar;
                    if (begin < indexMin) {
                        begin = indexMin;
                    }
                    int end = textBlockSize * (i + 1) + overlapChar;
                    if (end > len) {
                        end = len;
                    }
                    String chunk = content.substring(begin, end);
                    chunkList.add(chunk);
                    i++;
                    right = right + textBlockSize;
                } else {
                    break;
                }
            }
        }
        return chunkList;
    }
}
