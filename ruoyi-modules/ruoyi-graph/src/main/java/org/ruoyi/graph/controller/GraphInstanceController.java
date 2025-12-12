package org.ruoyi.graph.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.graph.domain.GraphBuildTask;
import org.ruoyi.graph.domain.GraphInstance;
import org.ruoyi.graph.enums.GraphStatusEnum;
import org.ruoyi.graph.service.IGraphBuildTaskService;
import org.ruoyi.graph.service.IGraphInstanceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱实例管理控制器
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/graph/instance")
@Tag(name = "图谱实例管理", description = "知识图谱实例的创建、查询、更新、删除")
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphInstanceController extends BaseController {

    private final IGraphInstanceService graphInstanceService;
    private final IGraphBuildTaskService buildTaskService;

    /**
     * 辅助方法：根据ID或UUID获取图谱实例
     */
    private GraphInstance getInstanceByIdOrUuid(String id) {
        GraphInstance instance = null;

        // 尝试作为数字ID查询
        try {
            Long numericId = Long.parseLong(id);
            instance = graphInstanceService.getById(numericId);
        } catch (NumberFormatException e) {
            // 不是数字，尝试作为UUID查询
            instance = graphInstanceService.getByUuid(id);
        }

        return instance;
    }

    /**
     * 创建图谱实例
     */
    @Operation(summary = "创建图谱实例")
    @PostMapping
    public R<GraphInstance> createInstance(@RequestBody GraphInstance graphInstance) {
        try {
            if (graphInstance.getKnowledgeId() == null || graphInstance.getKnowledgeId().trim().isEmpty()) {
                return R.fail("知识库ID不能为空");
            }
            if (graphInstance.getInstanceName() == null || graphInstance.getInstanceName().trim().isEmpty()) {
                return R.fail("图谱名称不能为空");
            }

            // 创建基础实例
            GraphInstance instance = graphInstanceService.createInstance(
                    graphInstance.getKnowledgeId(),
                    graphInstance.getInstanceName(),
                    graphInstance.getConfig()
            );

            // 设置扩展属性
            boolean needUpdate = false;
            if (graphInstance.getModelName() != null) {
                instance.setModelName(graphInstance.getModelName());
                needUpdate = true;
            }
            if (graphInstance.getEntityTypes() != null) {
                instance.setEntityTypes(graphInstance.getEntityTypes());
                needUpdate = true;
            }
            if (graphInstance.getRelationTypes() != null) {
                instance.setRelationTypes(graphInstance.getRelationTypes());
                needUpdate = true;
            }
            if (graphInstance.getRemark() != null) {
                instance.setRemark(graphInstance.getRemark());
                needUpdate = true;
            }

            // 如果有扩展属性，更新到数据库
            if (needUpdate) {
                graphInstanceService.updateInstance(instance);
            }

            return R.ok(instance);
        } catch (Exception e) {
            return R.fail("创建图谱实例失败: " + e.getMessage());
        }
    }

    /**
     * 更新图谱实例
     */
    @Operation(summary = "更新图谱实例")
    @PutMapping
    public R<GraphInstance> updateInstance(@RequestBody GraphInstance graphInstance) {
        try {
            if (graphInstance.getId() == null && (graphInstance.getGraphUuid() == null || graphInstance.getGraphUuid().trim().isEmpty())) {
                return R.fail("图谱ID不能为空");
            }

            // 如果有 instanceName，更新基本信息
            if (graphInstance.getInstanceName() != null) {
                // 这里可以添加更新实例名称的逻辑
            }

            // 更新配置
            if (graphInstance.getConfig() != null) {
                graphInstanceService.updateConfig(graphInstance.getGraphUuid(), graphInstance.getConfig());
            }

            // 更新模型名称、实体类型、关系类型等
            // 注意：这里需要在 Service 层实现完整的更新逻辑

            GraphInstance instance = graphInstanceService.getByUuid(graphInstance.getGraphUuid());
            return R.ok(instance);
        } catch (Exception e) {
            return R.fail("更新图谱实例失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID或UUID获取图谱实例
     */
    @Operation(summary = "获取图谱实例")
    @GetMapping("/{id}")
    public R<GraphInstance> getByUuid(@PathVariable String id) {
        try {
            GraphInstance instance = getInstanceByIdOrUuid(id);

            if (instance == null) {
                return R.fail("图谱实例不存在");
            }
            return R.ok(instance);
        } catch (Exception e) {
            return R.fail("获取图谱实例失败: " + e.getMessage());
        }
    }

    /**
     * 获取图谱实例列表（支持分页和条件查询）
     */
    @Operation(summary = "获取图谱实例列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String instanceName,
            @RequestParam(required = false) String knowledgeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            // 使用枚举转换前端状态字符串为数字状态码
            Integer graphStatus = GraphStatusEnum.getCodeByStatusKey(status);

            // 创建分页对象
            Page<GraphInstance> page = new Page<>(pageNum, pageSize);

            // 调用 Service 层分页查询
            Page<GraphInstance> result = graphInstanceService.queryPage(page, instanceName, knowledgeId, graphStatus);

            // 构造返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("rows", result.getRecords());
            data.put("total", result.getTotal());

            return R.ok(data);
        } catch (Exception e) {
            return R.fail("获取图谱列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据知识库ID获取图谱列表
     */
    @Operation(summary = "获取知识库的图谱列表")
    @GetMapping("/knowledge/{knowledgeId}")
    public R<List<GraphInstance>> listByKnowledge(@PathVariable String knowledgeId) {
        try {
            List<GraphInstance> instances = graphInstanceService.listByKnowledgeId(knowledgeId);
            return R.ok(instances);
        } catch (Exception e) {
            return R.fail("获取图谱列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新图谱状态
     */
    @Operation(summary = "更新图谱状态")
    @PutMapping("/status/{graphUuid}")
    public R<Void> updateStatus(
            @PathVariable String graphUuid,
            @RequestParam Integer status) {
        try {
            boolean success = graphInstanceService.updateStatus(graphUuid, status);
            return success ? R.ok() : R.fail("更新状态失败");
        } catch (Exception e) {
            return R.fail("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 更新图谱统计信息
     */
    @Operation(summary = "更新图谱统计")
    @PutMapping("/counts/{graphUuid}")
    public R<Void> updateCounts(
            @PathVariable String graphUuid,
            @RequestParam Integer nodeCount,
            @RequestParam Integer relationshipCount) {
        try {
            boolean success = graphInstanceService.updateCounts(graphUuid, nodeCount, relationshipCount);
            return success ? R.ok() : R.fail("更新统计失败");
        } catch (Exception e) {
            return R.fail("更新统计失败: " + e.getMessage());
        }
    }

    /**
     * 更新图谱配置
     */
    @Operation(summary = "更新图谱配置")
    @PutMapping("/config/{graphUuid}")
    public R<Void> updateConfig(
            @PathVariable String graphUuid,
            @RequestBody Map<String, String> request) {
        try {
            String config = request.get("config");
            boolean success = graphInstanceService.updateConfig(graphUuid, config);
            return success ? R.ok() : R.fail("更新配置失败");
        } catch (Exception e) {
            return R.fail("更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除图谱实例（软删除）
     */
    @Operation(summary = "删除图谱实例")
    @DeleteMapping("/{id}")
    public R<Void> deleteInstance(@PathVariable String id) {
        try {
            // 获取图谱实例
            GraphInstance instance = getInstanceByIdOrUuid(id);
            if (instance == null) {
                return R.fail("图谱实例不存在");
            }

            boolean success = graphInstanceService.deleteInstance(instance.getGraphUuid());
            return success ? R.ok() : R.fail("删除失败");
        } catch (Exception e) {
            return R.fail("删除图谱实例失败: " + e.getMessage());
        }
    }

    /**
     * 物理删除图谱实例及其数据
     */
    @Operation(summary = "彻底删除图谱")
    @DeleteMapping("/permanent/{graphUuid}")
    public R<Void> deleteInstanceAndData(@PathVariable String graphUuid) {
        try {
            boolean success = graphInstanceService.deleteInstanceAndData(graphUuid);
            return success ? R.ok() : R.fail("删除失败");
        } catch (Exception e) {
            return R.fail("彻底删除图谱失败: " + e.getMessage());
        }
    }

    /**
     * 获取图谱统计信息
     */
    @Operation(summary = "获取图谱统计")
    @GetMapping("/stats/{graphUuid}")
    public R<Map<String, Object>> getStatistics(@PathVariable String graphUuid) {
        try {
            Map<String, Object> stats = graphInstanceService.getStatistics(graphUuid);
            return R.ok(stats);
        } catch (Exception e) {
            return R.fail("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 构建图谱（全量构建知识库）
     */
    @Operation(summary = "构建图谱")
    @PostMapping("/build/{id}")
    public R<GraphBuildTask> buildGraph(@PathVariable String id) {
        try {
            // 获取图谱实例
            GraphInstance instance = getInstanceByIdOrUuid(id);

            if (instance == null) {
                return R.fail("图谱实例不存在");
            }

            // 更新状态为构建中
            graphInstanceService.updateStatus(instance.getGraphUuid(), 10); // 10=构建中

            // 创建构建任务（全量构建）
            GraphBuildTask task = buildTaskService.createTask(
                    instance.getGraphUuid(),
                    instance.getKnowledgeId(),
                    null,  // docId=null 表示全量构建
                    1      // taskType=1 全量构建
            );

            // 异步启动任务
            buildTaskService.startTask(task.getTaskUuid());

            return R.ok(task);
        } catch (Exception e) {
            return R.fail("启动构建任务失败: " + e.getMessage());
        }
    }

    /**
     * 重建图谱（清空后重新构建）
     */
    @Operation(summary = "重建图谱")
    @PostMapping("/rebuild/{id}")
    public R<GraphBuildTask> rebuildGraph(@PathVariable String id) {
        try {
            // 获取图谱实例
            GraphInstance instance = getInstanceByIdOrUuid(id);

            if (instance == null) {
                return R.fail("图谱实例不存在");
            }

            // 更新状态为构建中
            graphInstanceService.updateStatus(instance.getGraphUuid(), 10); // 10=构建中

            // 创建重建任务
            GraphBuildTask task = buildTaskService.createTask(
                    instance.getGraphUuid(),
                    instance.getKnowledgeId(),
                    null,  // docId=null 表示全量
                    2      // taskType=2 重建
            );

            // 异步启动任务
            buildTaskService.startTask(task.getTaskUuid());

            return R.ok(task);
        } catch (Exception e) {
            return R.fail("启动重建任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取构建状态
     */
    @Operation(summary = "获取构建状态")
    @GetMapping("/status/{id}")
    public R<Map<String, Object>> getBuildStatus(@PathVariable String id) {
        try {
            // 获取图谱实例
            GraphInstance instance = getInstanceByIdOrUuid(id);

            if (instance == null) {
                return R.fail("图谱实例不存在");
            }

            // 获取最新的构建任务
            GraphBuildTask latestTask = buildTaskService.getLatestTask(instance.getGraphUuid());

            Map<String, Object> result = new HashMap<>();
            result.put("graphStatus", instance.getGraphStatus());
            result.put("nodeCount", instance.getNodeCount());
            result.put("relationshipCount", instance.getRelationshipCount());

            if (latestTask != null) {
                result.put("taskStatus", latestTask.getTaskStatus());
                // ⭐ 确保 progress 不为 null，前端期望是 number 类型
                Integer progress = latestTask.getProgress();
                result.put("progress", progress != null ? progress : 0);
                result.put("errorMessage", latestTask.getErrorMessage());

                // 转换状态字符串（兼容前端）
                String status = "NOT_BUILT";
                if (instance.getGraphStatus() == 10) status = "BUILDING";
                else if (instance.getGraphStatus() == 20) status = "COMPLETED";
                else if (instance.getGraphStatus() == 30) status = "FAILED";

                result.put("status", status);
            } else {
                // ⭐ 如果没有任务，也返回默认值
                result.put("taskStatus", null);
                result.put("progress", 0);
                result.put("errorMessage", null);
                result.put("status", "NOT_BUILT");
            }

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取构建状态失败: " + e.getMessage());
        }
    }
}
