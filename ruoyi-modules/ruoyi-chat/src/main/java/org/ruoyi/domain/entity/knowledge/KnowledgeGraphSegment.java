package org.ruoyi.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识图谱片段对象 knowledge_graph_segment
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_graph_segment")
public class KnowledgeGraphSegment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
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
    private Long chunkIndex;

    /**
     * 总片段数
     */
    private Long totalChunks;

    /**
     * 抽取状态：0-待处理 1-处理中 2-已完成 3-失败
     */
    private Long extractionStatus;

    /**
     * 抽取的实体数量
     */
    private Long entityCount;

    /**
     * 抽取的关系数量
     */
    private Long relationCount;

    /**
     * 消耗的token数
     */
    private Long tokenUsed;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 备注
     */
    private String remark;


}
