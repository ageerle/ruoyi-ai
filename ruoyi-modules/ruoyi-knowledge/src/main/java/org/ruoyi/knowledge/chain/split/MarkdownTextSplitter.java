package org.ruoyi.knowledge.chain.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class MarkdownTextSplitter implements TextSplitter{
    @Override
    public List<String> split(String content) {
        return null;
    }
}
