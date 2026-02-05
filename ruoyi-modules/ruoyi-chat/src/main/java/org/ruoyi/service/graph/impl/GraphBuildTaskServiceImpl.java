package org.ruoyi.service.graph.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.graph.GraphBuildTask;
import org.ruoyi.domain.bo.graph.GraphInstance;
import org.ruoyi.domain.bo.knowledge.KnowledgeAttachBo;
import org.ruoyi.domain.bo.knowledge.KnowledgeFragmentBo;
import org.ruoyi.domain.dto.GraphExtractionResult;
import org.ruoyi.domain.vo.knowledge.KnowledgeAttachVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.mapper.graph.GraphBuildTaskMapper;
import org.ruoyi.service.graph.IGraphBuildTaskService;
import org.ruoyi.service.graph.IGraphInstanceService;
import org.ruoyi.service.graph.IGraphRAGService;
import org.ruoyi.service.knowledge.IKnowledgeAttachService;
import org.ruoyi.service.knowledge.IKnowledgeFragmentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱构建任务服务实现
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphBuildTaskServiceImpl implements IGraphBuildTaskService {

    private final GraphBuildTaskMapper taskMapper;

    private final IGraphRAGService graphRAGService;

    private final IGraphInstanceService graphInstanceService;

    private final IKnowledgeFragmentService knowledgeFragmentService;

    private final IKnowledgeAttachService knowledgeAttachService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphBuildTask createTask(String graphUuid, String knowledgeId, String docId, Integer taskType) {
        GraphBuildTask task = new GraphBuildTask();
        task.setTaskUuid(IdUtil.fastSimpleUUID());
        task.setGraphUuid(graphUuid);
        task.setKnowledgeId(knowledgeId);
        task.setDocId(docId);

        // 设置任务类型和状态（使用整数）
        task.setTaskType(taskType != null ? taskType : 1);
        task.setTaskStatus(1); // 1-待处理
        task.setProgress(0);

        taskMapper.insert(task);

        log.info("创建图谱构建任务: taskId={}, taskUuid={}, graphUuid={}, knowledgeId={}, type={}",
                task.getId(), task.getTaskUuid(), graphUuid, knowledgeId, task.getTaskType());

        return task;
    }

    @Override
    @Async("graphBuildExecutor")  // ⭐ 启用异步执行，使用专用线程池
    public void startTask(String taskUuid) {
        // 记录线程信息
        String threadName = Thread.currentThread().getName();
        log.info("🚀 图谱构建任务启动 - taskUuid: {}, 线程: {}", taskUuid, threadName);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证任务存在性
            GraphBuildTask task = getByUuid(taskUuid);
            if (task == null) {
                log.error("❌ 任务不存在: taskUuid={}", taskUuid);
                return;
            }

            // 2. 检查任务状态（防止重复执行）
            if (task.getTaskStatus() != 1) {  // 1-待处理
                log.warn("⚠️ 任务状态不允许执行: taskUuid={}, currentStatus={}",
                        taskUuid, task.getTaskStatus());
                return;
            }

            // 3. 更新任务状态为运行中
            boolean statusUpdated = updateStatus(taskUuid, 2); // 2-运行中
            if (!statusUpdated) {
                log.error("❌ 更新任务状态失败: taskUuid={}", taskUuid);
                return;
            }

            log.info("✅ 任务状态已更新为运行中: taskUuid={}", taskUuid);

            // 4. 执行图谱构建逻辑
            try {
                executeTaskLogic(task);

                long duration = (System.currentTimeMillis() - startTime) / 1000;
                log.info("🎉 图谱构建任务完成: taskUuid={}, 耗时: {}秒, 线程: {}",
                        taskUuid, duration, threadName);

            } catch (OutOfMemoryError oom) {
                // 特殊处理OOM错误
                log.error("💥 图谱构建任务内存溢出: taskUuid={}, 线程: {}", taskUuid, threadName, oom);
                markFailed(taskUuid, "内存溢出，请减少批处理文档数量或增加JVM内存");

                // 建议垃圾回收
                System.gc();

            } catch (InterruptedException ie) {
                // 特殊处理中断异常
                Thread.currentThread().interrupt();
                log.error("⚠️ 图谱构建任务被中断: taskUuid={}, 线程: {}", taskUuid, threadName, ie);
                markFailed(taskUuid, "任务被中断: " + ie.getMessage());

            } catch (Exception e) {
                // 处理其他业务异常
                log.error("❌ 图谱构建任务执行失败: taskUuid={}, 线程: {}", taskUuid, threadName, e);

                // 提取简洁的错误信息
                String errorMsg = extractErrorMessage(e);
                markFailed(taskUuid, errorMsg);
            }

        } catch (Exception e) {
            // 处理外层异常（如数据库访问异常）
            log.error("❌ 图谱构建任务启动失败: taskUuid={}, 线程: {}", taskUuid, threadName, e);

            try {
                String errorMsg = extractErrorMessage(e);
                markFailed(taskUuid, errorMsg);
            } catch (Exception markFailEx) {
                log.error("❌ 标记任务失败时出错: taskUuid={}", taskUuid, markFailEx);
            }
        }
    }

    /**
     * 提取简洁的错误信息（用于前端显示）
     *
     * @param e 异常对象
     * @return 简洁的错误信息
     */
    private String extractErrorMessage(Exception e) {
        // 1. 优先使用自定义异常消息
        String message = e.getMessage();
        if (StrUtil.isNotBlank(message) && message.length() < 200) {
            return message;
        }

        // 2. 检查原因链
        Throwable cause = e.getCause();
        if (cause != null && StrUtil.isNotBlank(cause.getMessage())) {
            String causeMsg = cause.getMessage();
            if (causeMsg.length() < 200) {
                return causeMsg;
            }
        }

        // 3. 使用异常类名
        return e.getClass().getSimpleName() + ": " +
                (message != null ? message.substring(0, Math.min(150, message.length())) : "未知错误");
    }

    @Override
    public GraphBuildTask getByUuid(String taskUuid) {
        LambdaQueryWrapper<GraphBuildTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
        return taskMapper.selectOne(wrapper);
    }

    @Override
    public List<GraphBuildTask> listByGraphUuid(String graphUuid) {
        LambdaQueryWrapper<GraphBuildTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphBuildTask::getGraphUuid, graphUuid);
        wrapper.orderByDesc(GraphBuildTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public GraphBuildTask getLatestTask(String graphUuid) {
        LambdaQueryWrapper<GraphBuildTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphBuildTask::getGraphUuid, graphUuid);
        wrapper.orderByDesc(GraphBuildTask::getCreateTime);
        wrapper.last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    @Override
    public List<GraphBuildTask> listByKnowledgeId(String knowledgeId) {
        LambdaQueryWrapper<GraphBuildTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphBuildTask::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(GraphBuildTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public List<GraphBuildTask> getPendingAndRunningTasks() {
        LambdaQueryWrapper<GraphBuildTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GraphBuildTask::getTaskStatus, 1, 2); // 1-待处理, 2-运行中
        wrapper.orderByAsc(GraphBuildTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public boolean updateProgress(String taskUuid, Integer progress, Integer processedDocs) {
        try {
            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            wrapper.set(GraphBuildTask::getProgress, progress);

            if (processedDocs != null) {
                wrapper.set(GraphBuildTask::getProcessedDocs, processedDocs);
            }

            int rows = taskMapper.update(null, wrapper);
            log.info("📊 更新任务进度: taskUuid={}, progress={}%, processedDocs={}, rows={}",
                    taskUuid, progress, processedDocs, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新任务进度失败: taskUuid={}, progress={}", taskUuid, progress, e);
            return false;
        }
    }

    @Override
    public boolean updateStatus(String taskUuid, Integer status) {
        try {
            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            wrapper.set(GraphBuildTask::getTaskStatus, status);

            // 如果是开始运行，设置开始时间
            if (status == 2) {
                wrapper.set(GraphBuildTask::getStartTime, new Date());
            }

            // 如果是完成或失败，设置结束时间
            if (status == 3 || status == 4) {
                wrapper.set(GraphBuildTask::getEndTime, new Date());
            }

            int rows = taskMapper.update(null, wrapper);

            log.info("更新任务状态: taskUuid={}, status={}, rows={}", taskUuid, status, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新任务状态失败: taskUuid={}, status={}", taskUuid, status, e);
            return false;
        }
    }

    @Override
    public boolean updateExtractionStats(String taskUuid, Integer extractedEntities, Integer extractedRelations) {
        try {
            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);

            if (extractedEntities != null) {
                wrapper.set(GraphBuildTask::getExtractedEntities, extractedEntities);
            }
            if (extractedRelations != null) {
                wrapper.set(GraphBuildTask::getExtractedRelations, extractedRelations);
            }

            int rows = taskMapper.update(null, wrapper);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新提取统计失败: taskUuid={}", taskUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccess(String taskUuid, String resultSummary) {
        try {
            // 1. 更新任务状态
            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            wrapper.set(GraphBuildTask::getTaskStatus, 3); // 3-已完成
            wrapper.set(GraphBuildTask::getProgress, 100);
            wrapper.set(GraphBuildTask::getEndTime, new Date());
            wrapper.set(GraphBuildTask::getResultSummary, resultSummary);

            int rows = taskMapper.update(null, wrapper);

            // 2. 更新图谱实例状态为"已完成"
            GraphBuildTask task = getByUuid(taskUuid);
            if (task != null && task.getGraphUuid() != null) {
                graphInstanceService.updateStatus(task.getGraphUuid(), 20); // 20-已完成
                log.info("更新图谱实例状态为已完成: graphUuid={}", task.getGraphUuid());
            }

            log.info("标记任务成功: taskUuid={}, rows={}", taskUuid, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("标记任务成功失败: taskUuid={}", taskUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markFailed(String taskUuid, String errorMessage) {
        try {
            // 1. 更新任务状态
            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            wrapper.set(GraphBuildTask::getTaskStatus, 4); // 4-失败
            wrapper.set(GraphBuildTask::getErrorMessage, errorMessage);
            wrapper.set(GraphBuildTask::getEndTime, new Date());

            int rows = taskMapper.update(null, wrapper);

            // 2. 更新图谱实例状态为"失败"
            GraphBuildTask task = getByUuid(taskUuid);
            if (task != null && task.getGraphUuid() != null) {
                graphInstanceService.updateStatus(task.getGraphUuid(), 30); // 30-失败
                log.info("更新图谱实例状态为失败: graphUuid={}", task.getGraphUuid());
            }

            log.error("标记任务失败: taskUuid={}, error={}, rows={}", taskUuid, errorMessage, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("标记任务失败失败: taskUuid={}", taskUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(String taskUuid) {
        try {
            GraphBuildTask task = getByUuid(taskUuid);
            if (task == null) {
                log.error("任务不存在: taskUuid={}", taskUuid);
                return false;
            }

            // 只能取消待处理或运行中的任务
            if (task.getTaskStatus() != 1 && task.getTaskStatus() != 2) {
                log.warn("任务状态不允许取消: taskUuid={}, status={}", taskUuid, task.getTaskStatus());
                return false;
            }

            LambdaUpdateWrapper<GraphBuildTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            wrapper.set(GraphBuildTask::getTaskStatus, 4); // 4-失败
            wrapper.set(GraphBuildTask::getErrorMessage, "任务已取消");
            wrapper.set(GraphBuildTask::getEndTime, new Date());

            int rows = taskMapper.update(null, wrapper);

            log.info("取消任务: taskUuid={}, rows={}", taskUuid, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("取消任务失败: taskUuid={}", taskUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryTask(String taskUuid) {
        try {
            GraphBuildTask oldTask = getByUuid(taskUuid);
            if (oldTask == null) {
                log.error("任务不存在: taskUuid={}", taskUuid);
                return null;
            }

            // 创建新任务
            GraphBuildTask newTask = createTask(
                    oldTask.getGraphUuid(),
                    oldTask.getKnowledgeId(),
                    oldTask.getDocId(),
                    oldTask.getTaskType()
            );

            log.info("重试任务: oldTaskUuid={}, newTaskUuid={}", taskUuid, newTask.getTaskUuid());
            return newTask.getTaskUuid();
        } catch (Exception e) {
            log.error("重试任务失败: taskUuid={}", taskUuid, e);
            return null;
        }
    }

    /**
     * 执行图谱构建任务的核心逻辑
     *
     * @param task 构建任务
     * @throws Exception 执行过程中的异常
     */
    private void executeTaskLogic(GraphBuildTask task) throws Exception {
        String taskUuid = task.getTaskUuid();
        String graphUuid = task.getGraphUuid();
        String knowledgeId = task.getKnowledgeId();
        String docId = task.getDocId();
        Integer taskType = task.getTaskType();

        long startTime = System.currentTimeMillis();
        int totalDocs = 0;
        int processedDocs = 0;
        int successDocs = 0;  // ⭐ 新增：成功处理的文档数
        int failedDocs = 0;   // ⭐ 新增：失败的文档数
        int totalEntities = 0;
        int totalRelations = 0;

        // ⭐ 记录初始内存状态
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        log.info("📊 初始内存使用: {} MB / {} MB",
                initialMemory / 1024 / 1024,
                runtime.maxMemory() / 1024 / 1024);

        try {
            // 0. 获取图谱实例配置（包括LLM模型）
            String modelName = null;
            if (StrUtil.isNotBlank(graphUuid)) {
                GraphInstance graphInstance = graphInstanceService.getByUuid(graphUuid);
                if (graphInstance != null && StrUtil.isNotBlank(graphInstance.getModelName())) {
                    modelName = graphInstance.getModelName();
                    log.info("使用图谱实例配置的模型: {}", modelName);
                }
            }

            // 1. 获取需要处理的文档列表
            List<KnowledgeFragmentVo> documents;

            if (taskType == 1) {
                // 类型1: 全量构建（知识库所有文档）
                if (StrUtil.isBlank(knowledgeId)) {
                    throw new RuntimeException("知识库构建任务缺少知识库ID");
                }

                // 查询知识库下的所有文档(目前只处理单文档)
                KnowledgeFragmentBo bo = new KnowledgeFragmentBo();
                bo.setDocId(docId);
                log.info("🔍 准备查询文档: knowledgeId={}", knowledgeId);
                documents = knowledgeFragmentService.queryList(bo);
                log.info("📋 查询返回文档数: {}", documents != null ? documents.size() : "null");

            } else if (taskType == 2) {
                // 类型2: 重建（清空后全量重建）
                if (StrUtil.isBlank(knowledgeId)) {
                    throw new RuntimeException("知识库构建任务缺少知识库ID");
                }

                // ⭐ 先清空该知识库的旧图谱数据
                log.info("🗑️ 重建模式：先清空知识库的旧图谱数据，knowledgeId: {}", knowledgeId);
                boolean deleted = graphRAGService.deleteGraphData(knowledgeId);
                if (deleted) {
                    log.info("✅ 旧图谱数据清空成功");
                } else {
                    log.warn("⚠️ 旧图谱数据清空失败（可能是没有旧数据）");
                }

                // 查询知识库下的所有文档
                KnowledgeFragmentBo bo = new KnowledgeFragmentBo();
                bo.setDocId(docId);
                log.info("🔍 准备查询文档: knowledgeId={}, bo.getKid()={}", knowledgeId);
                documents = knowledgeFragmentService.queryList(bo);
                log.info("📋 查询返回文档数: {}", documents != null ? documents.size() : "null");

            } else if (taskType == 3) {
                // 类型3: 单文档增量构建
                if (StrUtil.isBlank(docId)) {
                    throw new RuntimeException("单文档构建任务缺少文档ID");
                }

                // 根据docId查询单个文档
                KnowledgeFragmentBo bo = new KnowledgeFragmentBo();
                bo.setDocId(docId);
                log.info("🔍 准备查询单个文档: docId={}, bo.getDocId()={}", docId, bo.getDocId());
                documents = knowledgeFragmentService.queryList(bo);
                log.info("📋 查询返回文档数: {}", documents != null ? documents.size() : "null");

            } else {
                throw new RuntimeException("未知的任务类型: " + taskType);
            }

            if (documents == null || documents.isEmpty()) {
                String errorMsg = String.format(
                        "❌ 没有找到需要处理的文档！\n" +
                                "  taskUuid: %s\n" +
                                "  knowledgeId: %s\n" +
                                "  docId: %s\n" +
                                "  taskType: %d\n" +
                                "  documents: %s\n" +
                                "请检查：\n" +
                                "  1. knowledge_attach 表中是否有 kid='%s' 的记录\n" +
                                "  2. knowledgeId 是否正确传递\n" +
                                "  3. KnowledgeAttachService.queryList() 是否正确执行",
                        taskUuid, knowledgeId, docId, taskType,
                        documents == null ? "null" : "empty list",
                        knowledgeId
                );
                log.warn(errorMsg);

                Map<String, Object> summary = new HashMap<>();
                summary.put("message", "没有找到需要处理的文档");
                summary.put("totalDocs", 0);
                summary.put("knowledgeId", knowledgeId);
                summary.put("taskType", taskType);
                markSuccess(taskUuid, JSON.toJSONString(summary));  // ⭐ 使用 JSON 序列化
                return;
            }

            totalDocs = documents.size();
            log.info("开始构建图谱，共 {} 个文档", totalDocs);

            // ⭐ 更新任务的 total_docs 字段
            LambdaUpdateWrapper<GraphBuildTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GraphBuildTask::getTaskUuid, taskUuid);
            updateWrapper.set(GraphBuildTask::getTotalDocs, totalDocs);
            taskMapper.update(null, updateWrapper);
            log.info("📊 更新任务total_docs: {}", totalDocs);

            // 限制处理文档数量，避免内存溢出
            int maxDocsPerBatch = 50; // 每批最多处理50个文档
            if (totalDocs > maxDocsPerBatch) {
                log.warn("文档数量较多({}个)，建议分批处理，当前批次限制为{}个", totalDocs, maxDocsPerBatch);
                documents = documents.subList(0, Math.min(maxDocsPerBatch, totalDocs));
                totalDocs = documents.size();

                // ⭐ 重新更新 total_docs（因为被限制了）
                LambdaUpdateWrapper<GraphBuildTask> updateWrapper2 = new LambdaUpdateWrapper<>();
                updateWrapper2.eq(GraphBuildTask::getTaskUuid, taskUuid);
                updateWrapper2.set(GraphBuildTask::getTotalDocs, totalDocs);
                taskMapper.update(null, updateWrapper2);
                log.info("📊 更新限制后的total_docs: {}", totalDocs);
            }

            // 2. 逐个处理文档（带内存管理和错误恢复）
            for (int i = 0; i < documents.size(); i++) {
                KnowledgeFragmentVo doc = documents.get(i);
                long docStartTime = System.currentTimeMillis();

                try {
                    // ⭐ 检查内存状态
                    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                    long maxMemory = runtime.maxMemory();
                    double memoryUsage = (double) usedMemory / maxMemory * 100;

                    if (memoryUsage > 80) {
                        log.warn("⚠️ 内存使用率过高: {}/{}MB ({}%), 建议垃圾回收",
                                usedMemory / 1024 / 1024,
                                maxMemory / 1024 / 1024,
                                String.format("%.2f", memoryUsage));
                        System.gc();
                        try {
                            Thread.sleep(1000);  // 等待GC完成
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("⚠️ 等待GC时被中断");
                        }
                    }

                    // 2.1 获取文档内容
                    String content = doc.getContent();
                    if (StrUtil.isBlank(content)) {
                        log.warn("⚠️ 文档内容为空，跳过: docId={}", doc.getDocId());
                        processedDocs++;
                        failedDocs++;
                        continue;
                    }

                    // 限制单个文档内容大小，避免内存溢出
                    if (content.length() > 50000) {
                        log.warn("⚠️ 文档内容过大({} 字符)，截断处理: docId={}",
                                content.length(), doc.getDocId());
                        content = content.substring(0, 50000);
                    }
                    KnowledgeAttachBo bo = new KnowledgeAttachBo();
                    bo.setDocId(docId);
                    KnowledgeAttachVo docInfo = knowledgeAttachService.queryList(bo).get(0);

                    // 2.2 准备元数据（不包含大字段）
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("docId", doc.getDocId());
                    metadata.put("docName", docInfo.getName());
                    metadata.put("docType", docInfo.getType());
                    metadata.put("kid", knowledgeId);

                    // 2.3 调用GraphRAG服务进行图谱入库（使用图谱实例配置的模型）
                    GraphExtractionResult result = null;
                    try {
                        if (content.length() > 2000) {
                            // 长文档，使用分片处理
                            result = graphRAGService.ingestDocumentWithModel(
                                    content, knowledgeId, metadata, modelName);
                        } else {
                            // 短文档，直接处理
                            result = graphRAGService.ingestTextWithModel(
                                    content, knowledgeId, metadata, modelName);
                        }
                    } catch (OutOfMemoryError oom) {
                        // OOM单独处理：强制GC后继续
                        log.error("💥 处理文档时OOM，强制垃圾回收: docId={}", doc.getDocId());
                        System.gc();
                        try {
                            Thread.sleep(2000);  // 等待GC完成
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("⚠️ 等待GC时被中断");
                            // 中断时不继续处理，跳出循环
                            throw ie;
                        }
                        processedDocs++;
                        failedDocs++;
                        continue;
                    } catch (Exception e) {
                        log.error("❌ LLM调用失败，跳过文档: docId={}, error={}",
                                doc.getDocId(), e.getMessage());
                        processedDocs++;
                        failedDocs++;
                        continue;
                    }

                    // 2.4 统计结果
                    if (result != null && result.getSuccess()) {
                        int entities = result.getEntities().size();
                        int relations = result.getRelations().size();
                        totalEntities += entities;
                        totalRelations += relations;
                        successDocs++;

                        long docDuration = System.currentTimeMillis() - docStartTime;
                        log.info("✅ 文档处理成功: docId={}, 实体数={}, 关系数={}, 耗时={}ms",
                                doc.getDocId(), entities, relations, docDuration);
                    } else {
                        failedDocs++;
                        log.warn("⚠️ 文档处理失败: docId={}, error={}",
                                doc.getDocId(), result != null ? result.getErrorMessage() : "unknown");
                    }

                    // 2.5 更新进度
                    processedDocs++;
                    int progress = (processedDocs * 100) / totalDocs;
                    log.info("📈 文档进度: {}/{}, 进度={}%", processedDocs, totalDocs, progress);
                    boolean updated = updateProgress(taskUuid, progress, processedDocs);
                    if (!updated) {
                        log.warn("⚠️ 进度更新失败: taskUuid={}, progress={}", taskUuid, progress);
                    }

                    // 2.6 定期进行垃圾回收和内存检查
                    if ((i + 1) % 10 == 0) {
                        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                        log.info("📊 已处理{}/{}个文档, 内存使用: {} MB",
                                i + 1, totalDocs, currentMemory / 1024 / 1024);
                        System.gc();
                        try {
                            Thread.sleep(500);  // 短暂等待GC
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("⚠️ 等待GC时被中断");
                        }
                    }

                } catch (InterruptedException ie) {
                    // 中断异常：重新抛出，终止任务
                    Thread.currentThread().interrupt();
                    log.error("⚠️ 任务被中断，停止处理文档: docId={}", doc.getDocId());
                    throw ie;
                } catch (Exception e) {
                    log.error("❌ 处理文档时发生异常: docId={}, error={}",
                            doc.getDocId(), e.getMessage(), e);
                    processedDocs++;
                    failedDocs++;
                    // 继续处理下一个文档（不中断整个任务）
                } finally {
                    // 释放文档引用，帮助GC
                    documents.set(i, null);
                }
            }

            // 3. 构建完成，生成详细摘要
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDocs", totalDocs);
            summary.put("processedDocs", processedDocs);
            summary.put("successDocs", successDocs);      // ⭐ 成功文档数
            summary.put("failedDocs", failedDocs);        // ⭐ 失败文档数
            summary.put("totalEntities", totalEntities);
            summary.put("totalRelations", totalRelations);
            summary.put("duration", duration + "秒");
            summary.put("avgTimePerDoc", totalDocs > 0 ? (duration * 1000 / totalDocs) + "ms" : "N/A");  // ⭐ 平均处理时间
            summary.put("memoryUsed", (finalMemory - initialMemory) / 1024 / 1024 + "MB");  // ⭐ 内存增量
            summary.put("status", "completed");
            summary.put("modelName", modelName != null ? modelName : "default");  // ⭐ 使用的模型

            // 更新统计信息到任务
            updateExtractionStats(taskUuid, totalEntities, totalRelations);

            markSuccess(taskUuid, JSON.toJSONString(summary));

            log.info("🎉 图谱构建任务完成汇总:");
            log.info("  - taskUuid: {}", taskUuid);
            log.info("  - 文档总数: {}", totalDocs);
            log.info("  - 成功处理: {} 个", successDocs);
            log.info("  - 失败文档: {} 个", failedDocs);
            log.info("  - 实体总数: {}", totalEntities);
            log.info("  - 关系总数: {}", totalRelations);
            log.info("  - 总耗时: {} 秒", duration);
            log.info("  - 平均耗时: {} ms/文档", totalDocs > 0 ? duration * 1000 / totalDocs : 0);
            log.info("  - 内存增量: {} MB", (finalMemory - initialMemory) / 1024 / 1024);

        } catch (InterruptedException ie) {
            // 中断异常：向上抛出
            Thread.currentThread().interrupt();
            log.error("⚠️ 图谱构建任务被中断: taskUuid={}", taskUuid, ie);
            throw ie;
        } catch (Exception e) {
            log.error("❌ 图谱构建任务执行失败: taskUuid={}", taskUuid, e);
            throw e;
        } finally {
            // 清理资源，帮助GC
            System.gc();
            log.info("📊 最终内存状态: {} MB / {} MB",
                    (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                    runtime.maxMemory() / 1024 / 1024);
        }
    }
}
