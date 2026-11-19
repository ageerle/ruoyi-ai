package org.ruoyi.chat.service.knowledge.impl.split;

import org.ruoyi.chat.service.knowledge.DocumentSplitConfig;
import org.ruoyi.chat.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExcelTextSplitter implements TextSplitter {
    @Override
    public List<String> split(String content, DocumentSplitConfig config) {
        return SplitterSupport.split(content, config, SplitterSupport::lines);
    }
}
