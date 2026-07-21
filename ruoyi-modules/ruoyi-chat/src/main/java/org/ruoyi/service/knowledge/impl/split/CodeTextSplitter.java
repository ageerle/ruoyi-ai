package org.ruoyi.service.knowledge.impl.split;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 代码文件分片器：按空行（函数/类之间的自然边界）切分，
 * 块合并到不超过块大小，超大块再按滑动窗口切分
 */
@Component
@AllArgsConstructor
@Slf4j
public class CodeTextSplitter implements TextSplitter {

    private final IKnowledgeInfoService knowledgeInfoService;

    @Override
    public List<String> split(String content, String kid) {
        int textBlockSize = 1000;
        int overlapChar = 50;
        if (StringUtils.isNotBlank(kid)) {
            try {
                KnowledgeInfoVo info = knowledgeInfoService.queryById(Long.parseLong(kid));
                if (info != null) {
                    if (info.getTextBlockSize() != null && info.getTextBlockSize() > 0) {
                        textBlockSize = info.getTextBlockSize().intValue();
                    }
                    if (info.getOverlapChar() != null && info.getOverlapChar() > 0) {
                        overlapChar = info.getOverlapChar().intValue();
                    }
                }
            } catch (Exception e) {
                log.warn("查询知识库配置失败，使用默认配置, kid={}", kid, e);
            }
        }
        // 按空行切分，保留段落间的自然语义边界
        String[] sections = content.split("\\n\\s*\\n");
        return SplitterSupport.mergeAndSplit(sections, textBlockSize, overlapChar);
    }
}
