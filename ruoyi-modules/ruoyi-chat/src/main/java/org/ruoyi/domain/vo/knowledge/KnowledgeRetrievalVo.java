package org.ruoyi.domain.vo.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识检索测试结果视图对象
 *
 * @author RobustH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRetrievalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 片段ID
     */
    private String id;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 分片索引
     */
    private Integer idx;

    /**
     * 片段内容
     */
    private String content;

    /**
     * 相似度得分
     */
    private Double score;

    /**
     * 原始检索排名 (重排前)
     */
    private Integer originalIndex;

    /**
     * 原始检索得分 (重排前)
     */
    private Double rawScore;

    /**
     * 来源文档名称
     */
    private String sourceName;
}
