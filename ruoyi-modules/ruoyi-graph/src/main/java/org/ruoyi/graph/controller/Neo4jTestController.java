package org.ruoyi.graph.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.graph.config.GraphProperties;
import org.ruoyi.graph.config.Neo4jConfig;
import org.ruoyi.graph.util.Neo4jTestUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Neo4j测试控制器
 * 仅用于开发环境测试Neo4j连接
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@RestController
@RequestMapping("/graph/test")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class Neo4jTestController {

    private final Neo4jTestUtil neo4jTestUtil;
    private final Neo4jConfig neo4jConfig;
    private final GraphProperties graphProperties;


    @GetMapping("/connection")
    public R<Map<String, Object>> testConnection() {
        Map<String, Object> result = neo4jTestUtil.testConnection();

        if ("SUCCESS".equals(result.get("connection"))) {
            return R.ok("Neo4j连接成功", result);
        } else {
            return R.fail(result.get("error").toString());
        }
    }


    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("neo4j", Map.of(
                "uri", neo4jConfig.getUri(),
                "username", neo4jConfig.getUsername(),
                "database", neo4jConfig.getDatabase(),
                "maxConnectionPoolSize", neo4jConfig.getMaxConnectionPoolSize()
        ));
        config.put("graph", Map.of(
                "enabled", graphProperties.getEnabled(),
                "databaseType", graphProperties.getDatabaseType(),
                "batchSize", graphProperties.getBatchSize(),
                "extraction", graphProperties.getExtraction(),
                "query", graphProperties.getQuery()
        ));
        return R.ok(config);
    }


    @PostMapping("/node")
    public R<Map<String, Object>> createTestNode(@RequestParam String name) {
        Map<String, Object> result = neo4jTestUtil.createTestNode(name);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok("测试节点创建成功", result);
        } else {
            return R.fail(result.get("error").toString());
        }
    }


    @GetMapping("/node/{name}")
    public R<Map<String, Object>> queryTestNode(@PathVariable String name) {
        Map<String, Object> result = neo4jTestUtil.queryTestNode(name);

        if (Boolean.TRUE.equals(result.get("found"))) {
            return R.ok("节点查询成功", result);
        } else {
            return R.ok("未找到节点", result);
        }
    }


    @PostMapping("/relationship")
    public R<Map<String, Object>> createTestRelationship(
            @RequestParam String source,
            @RequestParam String target) {

        Map<String, Object> result = neo4jTestUtil.createTestRelationship(source, target);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok("测试关系创建成功", result);
        } else {
            return R.fail(result.get("error").toString());
        }
    }


    @DeleteMapping("/nodes")
    public R<Map<String, Object>> deleteAllTestNodes() {
        Map<String, Object> result = neo4jTestUtil.deleteAllTestNodes();

        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok("测试节点清理完成", result);
        } else {
            return R.fail(result.get("error").toString());
        }
    }


    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics() {
        Map<String, Object> result = neo4jTestUtil.getStatistics();

        if (result.containsKey("error")) {
            return R.fail(result.get("error").toString());
        }
        return R.ok(result);
    }


    @GetMapping("/health")
    public R<String> health() {
        Map<String, Object> result = neo4jTestUtil.testConnection();

        if ("SUCCESS".equals(result.get("connection"))) {
            return R.ok("Neo4j服务正常");
        } else {
            return R.fail("Neo4j服务异常: " + result.get("error"));
        }
    }

    /**
     * 调试关系查询 - 查看指定知识库的所有关系
     */
    @GetMapping("/debug/relationships/{knowledgeId}")
    public R<Map<String, Object>> debugRelationships(@PathVariable String knowledgeId) {
        Map<String, Object> result = neo4jTestUtil.debugRelationships(knowledgeId);
        return R.ok(result);
    }
}
