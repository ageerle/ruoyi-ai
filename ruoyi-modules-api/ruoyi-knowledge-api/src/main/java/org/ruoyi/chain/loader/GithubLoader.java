package org.ruoyi.chain.loader;

import org.ruoyi.chain.loader.ResourceLoader;

import java.io.InputStream;
import java.util.List;

public class GithubLoader implements ResourceLoader {
    @Override
    public String getContent(InputStream inputStream) {
        return null;
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return null;
    }
}
