package org.ruoyi.service.knowledge.impl.split;

import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class CharacterTextSplitter implements TextSplitter {
    @Override
    public List<String> split(String content, DocumentSplitConfig config) {
        return SplitterSupport.split(content, config, SplitterSupport::paragraphs);
    }
}
