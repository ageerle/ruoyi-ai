package org.ruoyi.graph.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识图谱片段实体
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base_graph_segment")
public class KnowledgeBaseGraphSegment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 片段UUID
     */
    private String uuid;

    /**
     * 知识库UUID
     */
    private String kbUuid;

    /**
     * 知识库条目UUID
     */
    private String kbItemUuid;

    /**
     * 文档UUID
     */
    private String docUuid;

    /**
     * 片段文本内容
     */
    private String segmentText;

    /**
     * 片段索引（第几个片段）
     */
    private Integer chunkIndex;

    /**
     * 总片段数
     */
    private Integer totalChunks;

    /**
     * 抽取状态：0-待处理 1-处理中 2-已完成 3-失败
     */
    private Integer extractionStatus;

    /**
     * 抽取的实体数量
     */
    private Integer entityCount;

    /**
     * 抽取的关系数量
     */
    private Integer relationCount;

    /**
     * 消耗的token数
     */
    private Integer tokenUsed;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 用户ID
     */
    private Long userId;
}
