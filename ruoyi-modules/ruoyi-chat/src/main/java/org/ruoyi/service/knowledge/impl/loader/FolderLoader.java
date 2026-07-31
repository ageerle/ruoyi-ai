package org.ruoyi.service.knowledge.impl.loader;

import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.DocumentSplitConfig;

import java.io.InputStream;
import java.util.List;

public class FolderLoader implements ResourceLoader {
    @Override
    public String getContent(InputStream inputStream) {
        return null;
    }

    @Override
    public List<String> getChunkList(String content, DocumentSplitConfig config) {
        return null;
    }
}
