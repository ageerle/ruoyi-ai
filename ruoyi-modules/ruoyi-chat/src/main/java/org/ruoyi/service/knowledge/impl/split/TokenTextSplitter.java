package org.ruoyi.service.knowledge.impl.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.knowledge.TextSplitter;
import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class TokenTextSplitter implements TextSplitter {
    @Override
    public List<String> split(String content, DocumentSplitConfig config) {
        return SplitterSupport.split(content, config, SplitterSupport::paragraphs);
    }
}
