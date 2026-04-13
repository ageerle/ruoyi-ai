package org.ruoyi.domain.vo.knowledge;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识检索测试结果视图对象
 *
 * @author RobustH
 */
@Data
@Builder
public class KnowledgeRetrievalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
