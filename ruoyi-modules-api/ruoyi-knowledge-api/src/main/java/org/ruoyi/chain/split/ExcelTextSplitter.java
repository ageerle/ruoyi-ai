package org.ruoyi.chain.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ExcelTextSplitter implements TextSplitter {


    @Override
    public List<String> split(String content, String kid) {
        // 使用默认配置
        String knowledgeSeparator = "#";
        int textBlockSize = 10000;
        int overlapChar = 500;
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
