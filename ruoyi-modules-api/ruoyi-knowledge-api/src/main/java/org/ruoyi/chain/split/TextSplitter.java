package org.ruoyi.chain.split;

import java.util.List;

/**
 * 文本切分
 */
public interface TextSplitter {

    /**
     * 文本切分
     *
     * @param content 文本内容
     * @param kid     知识库id
     * @return 切分后的文本列表
     */
    List<String> split(String content, String kid);
}
