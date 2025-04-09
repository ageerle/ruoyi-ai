package org.ruoyi.chain.loader;

import java.io.InputStream;
import java.util.List;

/**
 * 资源载入
 */
public interface ResourceLoader {

    String getContent(InputStream inputStream);

    List<String> getChunkList(String content, String kid);
}
