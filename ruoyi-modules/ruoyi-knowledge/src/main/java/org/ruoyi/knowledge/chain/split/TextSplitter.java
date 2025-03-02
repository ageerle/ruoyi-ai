package org.ruoyi.knowledge.chain.split;

import java.util.List;

/**
 * 文本切分
 */
public interface TextSplitter {

    List<String> split(String content);
}
