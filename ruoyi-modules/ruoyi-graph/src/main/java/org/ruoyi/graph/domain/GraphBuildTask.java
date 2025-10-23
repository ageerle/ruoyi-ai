package org.ruoyi.graph.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 图谱构建任务对象 graph_build_task
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@TableName("graph_build_task")
public class GraphBuildTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务UUID
     */
    private String taskUuid;

    /**
     * 图谱UUID
     */
    private String graphUuid;

    /**
     * 知识库ID
     */
    private String knowledgeId;

    /**
     * 文档ID（可选，null表示全量构建）
     */
    private String docId;

    /**
     * 任务类型：1全量构建、2增量更新、3重建
     */
    private Integer taskType;

    /**
     * 任务状态：1待执行、2执行中、3成功、4失败
     */
    private Integer taskStatus;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 总文档数
     */
    private Integer totalDocs;

    /**
     * 已处理文档数
     */
    private Integer processedDocs;

    /**
     * 提取的实体数
     */
    private Integer extractedEntities;

    /**
     * 提取的关系数
     */
    private Integer extractedRelations;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 结果摘要(JSON格式)
     */
    private String resultSummary;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
