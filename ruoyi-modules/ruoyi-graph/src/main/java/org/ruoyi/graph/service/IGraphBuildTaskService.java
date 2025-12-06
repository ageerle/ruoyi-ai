package org.ruoyi.graph.service;

import org.ruoyi.graph.domain.GraphBuildTask;

import java.util.List;

/**
 * 图谱构建任务服务接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface IGraphBuildTaskService {

    /**
     * 创建构建任务
     *
     * @param graphUuid   图谱UUID
     * @param knowledgeId 知识库ID
     * @param docId       文档ID（可选）
     * @param taskType    任务类型
     * @return 任务信息
     */
    GraphBuildTask createTask(String graphUuid, String knowledgeId, String docId, Integer taskType);

    /**
     * 启动构建任务（异步）
     *
     * @param taskUuid 任务UUID
     * @return 异步结果
     */
    void startTask(String taskUuid);

    /**
     * 根据UUID获取任务
     *
     * @param taskUuid 任务UUID
     * @return 任务信息
     */
    GraphBuildTask getByUuid(String taskUuid);

    /**
     * 根据图谱UUID获取任务列表
     *
     * @param graphUuid 图谱UUID
     * @return 任务列表
     */
    List<GraphBuildTask> listByGraphUuid(String graphUuid);

    /**
     * 获取图谱的最新构建任务
     *
     * @param graphUuid 图谱UUID
     * @return 最新任务
     */
    GraphBuildTask getLatestTask(String graphUuid);

    /**
     * 根据知识库ID获取任务列表
     *
     * @param knowledgeId 知识库ID
     * @return 任务列表
     */
    List<GraphBuildTask> listByKnowledgeId(String knowledgeId);

    /**
     * 获取待执行和执行中的任务
     *
     * @return 任务列表
     */
    List<GraphBuildTask> getPendingAndRunningTasks();

    /**
     * 更新任务进度
     *
     * @param taskUuid      任务UUID
     * @param progress      进度百分比
     * @param processedDocs 已处理文档数
     * @return 是否成功
     */
    boolean updateProgress(String taskUuid, Integer progress, Integer processedDocs);

    /**
     * 更新任务状态
     *
     * @param taskUuid 任务UUID
     * @param status   状态
     * @return 是否成功
     */
    boolean updateStatus(String taskUuid, Integer status);

    /**
     * 更新提取统计信息
     *
     * @param taskUuid           任务UUID
     * @param extractedEntities  提取的实体数
     * @param extractedRelations 提取的关系数
     * @return 是否成功
     */
    boolean updateExtractionStats(String taskUuid, Integer extractedEntities, Integer extractedRelations);

    /**
     * 标记任务为成功
     *
     * @param taskUuid      任务UUID
     * @param resultSummary 结果摘要
     * @return 是否成功
     */
    boolean markSuccess(String taskUuid, String resultSummary);

    /**
     * 标记任务为失败
     *
     * @param taskUuid     任务UUID
     * @param errorMessage 错误信息
     * @return 是否成功
     */
    boolean markFailed(String taskUuid, String errorMessage);

    /**
     * 取消任务
     *
     * @param taskUuid 任务UUID
     * @return 是否成功
     */
    boolean cancelTask(String taskUuid);

    /**
     * 重试失败的任务
     *
     * @param taskUuid 任务UUID
     * @return 新任务UUID
     */
    String retryTask(String taskUuid);
}
