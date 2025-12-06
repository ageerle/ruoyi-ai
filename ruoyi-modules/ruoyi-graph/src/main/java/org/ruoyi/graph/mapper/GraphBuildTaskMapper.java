package org.ruoyi.graph.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.graph.domain.GraphBuildTask;

import java.util.List;

/**
 * 图谱构建任务Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface GraphBuildTaskMapper extends BaseMapper<GraphBuildTask> {

    /**
     * 根据任务UUID查询
     *
     * @param taskUuid 任务UUID
     * @return 构建任务
     */
    GraphBuildTask selectByTaskUuid(String taskUuid);

    /**
     * 根据图谱UUID查询任务列表
     *
     * @param graphUuid 图谱UUID
     * @return 任务列表
     */
    List<GraphBuildTask> selectByGraphUuid(String graphUuid);

    /**
     * 根据知识库ID查询任务列表
     *
     * @param knowledgeId 知识库ID
     * @return 任务列表
     */
    List<GraphBuildTask> selectByKnowledgeId(String knowledgeId);

    /**
     * 查询待执行和执行中的任务
     *
     * @return 任务列表
     */
    List<GraphBuildTask> selectPendingAndRunningTasks();

    /**
     * 更新任务进度
     *
     * @param taskUuid      任务UUID
     * @param progress      进度
     * @param processedDocs 已处理文档数
     * @return 影响行数
     */
    int updateProgress(String taskUuid, Integer progress, Integer processedDocs);

    /**
     * 更新任务状态
     *
     * @param taskUuid 任务UUID
     * @param status   状态
     * @return 影响行数
     */
    int updateStatus(String taskUuid, Integer status);
}
