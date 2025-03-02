package org.ruoyi.knowledge.chain.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
@Primary
public class CharacterTextSplitter implements TextSplitter{
    private final SplitterProperties splitterProperties;
    @Override
    public List<String> split(String content) {
        List<String> chunkList = new ArrayList<>();
        if (content.contains(splitterProperties.getEndspliter())){
            // 按自定义分隔符切分
            String[] chunks = content.split(splitterProperties.getEndspliter());
            chunkList.addAll(Arrays.asList(chunks));
        }else {
            int indexMin = 0;
            int len = content.length();
            int i = 0;
            int right = 0;
            while (true) {
                if (len > right ){
                    int begin = i*splitterProperties.getSize() - splitterProperties.getOverlay();
                    if (begin < indexMin){
                        begin = indexMin;
                    }
                    int end = splitterProperties.getSize()*(i+1) + splitterProperties.getOverlay();
                    if (end > len){
                        end = len;
                    }
                    String chunk = content.substring(begin,end);
                    chunkList.add(chunk);
                    i++;
                    right = right + splitterProperties.getSize();
                }else {
                    break;
                }
            }
        }
        return chunkList;
    }
}
