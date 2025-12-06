package org.ruoyi.graph.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.graph.domain.GraphEdge;
import org.ruoyi.graph.domain.GraphVertex;
import org.ruoyi.graph.dto.GraphExtractionResult;
import org.ruoyi.graph.service.IGraphExtractionService;
import org.ruoyi.graph.service.IGraphRAGService;
import org.ruoyi.graph.service.IGraphStoreService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱查询控制器
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/graph/query")
@Tag(name = "图谱查询", description = "知识图谱查询相关接口")
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphQueryController extends BaseController {

    private final IGraphStoreService graphStoreService;
    private final IGraphExtractionService graphExtractionService;
    private final IGraphRAGService graphRAGService;

    /**
     * 获取知识库的图谱数据
     */
    @Operation(summary = "获取知识库图谱")
    @GetMapping("/knowledge/{knowledgeId}")
    public R<Map<String, Object>> getGraphByKnowledge(
            @PathVariable String knowledgeId,
            @RequestParam(defaultValue = "100") Integer limit) {

        try {
            // 查询节点
            List<GraphVertex> vertices = graphStoreService.queryVerticesByKnowledgeId(knowledgeId, limit);

            // 查询关系
            List<GraphEdge> edges = graphStoreService.queryEdgesByKnowledgeId(knowledgeId, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            result.put("vertexCount", vertices.size());
            result.put("edgeCount", edges.size());

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("获取图谱数据失败: " + e.getMessage());
        }
    }

    /**
     * 搜索实体节点
     */
    @Operation(summary = "搜索实体")
    @GetMapping("/search/entity")
    public R<List<GraphVertex>> searchEntity(
            @RequestParam String keyword,
            @RequestParam(required = false) String knowledgeId,
            @RequestParam(defaultValue = "20") Integer limit) {

        try {
            List<GraphVertex> vertices = graphStoreService.searchVerticesByName(keyword, knowledgeId, limit);
            return R.ok(vertices);
        } catch (Exception e) {
            return R.fail("搜索实体失败: " + e.getMessage());
        }
    }

    /**
     * 查询实体的邻居节点
     */
    @Operation(summary = "查询邻居节点")
    @GetMapping("/neighbors/{nodeId}")
    public R<List<GraphVertex>> getNeighbors(
            @PathVariable String nodeId,
            @RequestParam(required = false) String knowledgeId,
            @RequestParam(defaultValue = "20") Integer limit) {

        try {
            List<GraphVertex> neighbors = graphStoreService.getNeighbors(nodeId, knowledgeId, limit);
            return R.ok(neighbors);
        } catch (Exception e) {
            return R.fail("查询邻居节点失败: " + e.getMessage());
        }
    }

    /**
     * 查询两个实体之间的路径
     */
    @Operation(summary = "查询实体路径")
    @GetMapping("/path")
    public R<List<List<GraphVertex>>> findPath(
            @RequestParam String startNodeId,
            @RequestParam String endNodeId,
            @RequestParam(defaultValue = "5") Integer maxDepth) {

        try {
            List<List<GraphVertex>> paths = graphStoreService.findPaths(startNodeId, endNodeId, maxDepth);
            return R.ok(paths);
        } catch (Exception e) {
            return R.fail("查询路径失败: " + e.getMessage());
        }
    }

    /**
     * 从文本抽取实体和关系（测试用）
     */
    @Operation(summary = "文本实体抽取")
    @PostMapping("/extract")
    public R<GraphExtractionResult> extractFromText(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return R.fail("文本不能为空");
            }

            String modelName = request.get("modelName");
            GraphExtractionResult result;

            if (modelName != null && !modelName.trim().isEmpty()) {
                result = graphExtractionService.extractFromTextWithModel(text, modelName);
            } else {
                result = graphExtractionService.extractFromText(text);
            }

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("实体抽取失败: " + e.getMessage());
        }
    }

    /**
     * 将文本入库到图谱
     */
    @Operation(summary = "文本入库")
    @PostMapping("/ingest")
    public R<GraphExtractionResult> ingestText(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String knowledgeId = (String) request.get("knowledgeId");
            String modelName = (String) request.get("modelName");

            if (text == null || text.trim().isEmpty()) {
                return R.fail("文本不能为空");
            }
            if (knowledgeId == null || knowledgeId.trim().isEmpty()) {
                return R.fail("知识库ID不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");

            GraphExtractionResult result;
            if (modelName != null && !modelName.trim().isEmpty()) {
                result = graphRAGService.ingestTextWithModel(text, knowledgeId, metadata, modelName);
            } else {
                result = graphRAGService.ingestText(text, knowledgeId, metadata);
            }
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("文本入库失败: " + e.getMessage());
        }
    }

    /**
     * 基于图谱检索
     */
    @Operation(summary = "图谱检索")
    @GetMapping("/retrieve")
    public R<String> retrieveFromGraph(
            @RequestParam String query,
            @RequestParam String knowledgeId,
            @RequestParam(defaultValue = "10") Integer maxResults) {

        try {
            String result = graphRAGService.retrieveFromGraph(query, knowledgeId, maxResults);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("图谱检索失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库的图谱数据
     */
    @Operation(summary = "删除图谱数据")
    @DeleteMapping("/knowledge/{knowledgeId}")
    public R<Void> deleteGraphData(@PathVariable String knowledgeId) {
        try {
            boolean success = graphRAGService.deleteGraphData(knowledgeId);
            return success ? R.ok() : R.fail("删除失败");
        } catch (Exception e) {
            return R.fail("删除图谱数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取图谱统计信息
     */
    @Operation(summary = "图谱统计")
    @GetMapping("/stats/{knowledgeId}")
    public R<Map<String, Object>> getGraphStats(@PathVariable String knowledgeId) {
        try {
            Map<String, Object> stats = graphStoreService.getStatistics(knowledgeId);
            return R.ok(stats);
        } catch (Exception e) {
            return R.fail("获取统计信息失败: " + e.getMessage());
        }
    }
}
