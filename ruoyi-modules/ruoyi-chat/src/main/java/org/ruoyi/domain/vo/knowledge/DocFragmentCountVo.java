package org.ruoyi.domain.vo.knowledge;

import lombok.Data;

/**
 * 文档分块数统计 VO（用于 GROUP BY 查询结果接收）
 */
@Data
public class DocFragmentCountVo {

    /**
     * 文档ID（关联 knowledge_attach.doc_id）
     */
    private String docId;

    /**
     * 该文档下的分块数量
     */
    private Integer fragmentCount;
}
