package org.ruoyi.chain.split;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * rag 知识库md文档的切分
 */
@Component
@AllArgsConstructor
@Slf4j
public class MarkdownTextSplitter implements TextSplitter{
    @Override
    public List<String> split(String content, String kid) {
        //进行校验
        if (StrUtil.isBlank(content)){
            return Collections.emptyList();
        }
        //文档切分
        List<String> chunks = new ArrayList<>();
        // 将内容按行分割，支持 \n 和 \r\n 兼容的换行符
        String[] lines = content.split("\\r?\\n");

        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            // 如果该行是一个 Markdown 标题（以 # 开头，支持 1~6 个 #）
            if (line.trim().matches("^#{1,6}\\s.*")) {
                // 是标题，先把上一个chunk加入
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }
            }
            currentChunk.append(line).append("\n");
        }

        // 如果还有未加入的文本块（例如文末没有新标题），则加入到 chunks
        if (currentChunk.length()>0){
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }
}
