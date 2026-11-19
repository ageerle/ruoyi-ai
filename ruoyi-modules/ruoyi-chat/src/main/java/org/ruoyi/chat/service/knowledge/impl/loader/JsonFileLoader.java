package org.ruoyi.chat.service.knowledge.impl.loader;

import org.ruoyi.chat.service.knowledge.ResourceLoader;
import org.ruoyi.chat.service.knowledge.DocumentSplitConfig;

import java.io.InputStream;
import java.util.List;

public class JsonFileLoader implements ResourceLoader {
    @Override
    public String getContent(InputStream inputStream) {
        return null;
    }

    @Override
    public List<String> getChunkList(String content, DocumentSplitConfig config) {
        return null;
    }
}
