package org.ruoyi.service.graph.impl;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.ruoyi.config.GraphProperties;
import org.ruoyi.domain.bo.graph.GraphEdge;
import org.ruoyi.domain.bo.graph.GraphVertex;
import org.ruoyi.service.graph.IGraphStoreService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

/**
 * 图存储服务实现
 * 负责与 Neo4j 图数据库交互
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphStoreServiceImpl implements IGraphStoreService {

    private final Driver neo4jDriver;
    private final GraphProperties graphProperties;

    // ==================== 节点操作 ====================

    @Override
    public boolean addVertex(GraphVertex vertex) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "CREATE (n:" + vertex.getLabel() + " {" +
                    "id: $id, " +
                    "name: $name, " +
                    "description: $description, " +
                    "knowledgeId: $knowledgeId, " +
                    "docIds: $docIds, " +
                    "properties: $properties, " +
                    "confidence: $confidence" +
                    "}) RETURN n";

            Result result = session.run(cypher, parameters(
                    "id", vertex.getNodeId(),  // ⭐ 修复：使用 nodeId 而不是 id
                    "name", vertex.getName(),
                    "description", vertex.getDescription(),
                    "knowledgeId", vertex.getKnowledgeId(),
                    "docIds", vertex.getDocIds(),
                    "properties", vertex.getProperties(),
                    "confidence", vertex.getConfidence()
            ));

            return result.hasNext();
        } catch (Exception e) {
            log.error("添加节点失败: {}", vertex, e);
            return false;
        }
    }

    @Override
    public int addVertices(List<GraphVertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        int batchSize = graphProperties.getBatchSize();

        try (Session session = neo4jDriver.session()) {
            // 分批处理
            for (int i = 0; i < vertices.size(); i += batchSize) {
                List<GraphVertex> batch = vertices.subList(
                        i, Math.min(i + batchSize, vertices.size())
                );

                successCount += session.writeTransaction(tx -> {
                    int count = 0;
                    for (GraphVertex vertex : batch) {
                        String cypher = "CREATE (n:" + vertex.getLabel() + " {" +
                                "id: $id, name: $name, description: $description, " +
                                "knowledgeId: $knowledgeId, docIds: $docIds, " +
                                "properties: $properties, confidence: $confidence})";

                        tx.run(cypher, parameters(
                                "id", vertex.getNodeId(),  // ⭐ 修复：使用 nodeId 而不是 id
                                "name", vertex.getName(),
                                "description", vertex.getDescription(),
                                "knowledgeId", vertex.getKnowledgeId(),
                                "docIds", vertex.getDocIds(),
                                "properties", vertex.getProperties(),
                                "confidence", vertex.getConfidence()
                        ));
                        count++;
                    }
                    return count;
                });
            }
        } catch (Exception e) {
            log.error("批量添加节点失败", e);
        }

        return successCount;
    }

    @Override
    public GraphVertex getVertex(String nodeId, String graphUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n) WHERE n.id = $nodeId AND n.knowledgeId = $graphUuid RETURN n";

            Result result = session.run(cypher, parameters(
                    "nodeId", nodeId,
                    "graphUuid", graphUuid
            ));

            if (result.hasNext()) {
                Record record = result.single();
                return nodeToVertex(record.get("n").asNode());
            }
            return null;
        } catch (Exception e) {
            log.error("获取节点失败: nodeId={}, graphUuid={}", nodeId, graphUuid, e);
            return null;
        }
    }

    @Override
    public List<GraphVertex> searchVertices(String graphUuid, String label, Integer limit) {
        try (Session session = neo4jDriver.session()) {
            StringBuilder cypher = new StringBuilder("MATCH (n");
            if (label != null && !label.isEmpty()) {
                cypher.append(":").append(label);
            }
            cypher.append(") WHERE n.knowledgeId = $graphUuid RETURN n");

            if (limit != null && limit > 0) {
                cypher.append(" LIMIT $limit");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("graphUuid", graphUuid);
            if (limit != null && limit > 0) {
                params.put("limit", limit);
            }

            Result result = session.run(cypher.toString(), params);

            return result.stream()
                    .map(record -> nodeToVertex(record.get("n").asNode()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索节点失败: graphUuid={}, label={}", graphUuid, label, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphVertex> searchVerticesByName(String graphUuid, String name) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n) WHERE n.knowledgeId = $graphUuid AND n.name CONTAINS $name RETURN n";

            Result result = session.run(cypher, parameters(
                    "graphUuid", graphUuid,
                    "name", name
            ));

            return result.stream()
                    .map(record -> nodeToVertex(record.get("n").asNode()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("按名称搜索节点失败: graphUuid={}, name={}", graphUuid, name, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphVertex> searchVerticesByName(String keyword, String knowledgeId, Integer limit) {
        List<GraphVertex> vertices = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            String cypher;
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("limit", limit);

            if (knowledgeId != null && !knowledgeId.isEmpty()) {
                cypher = "MATCH (n {knowledgeId: $knowledgeId}) " +
                        "WHERE n.name CONTAINS $keyword " +
                        "RETURN n LIMIT $limit";
                params.put("knowledgeId", knowledgeId);
            } else {
                cypher = "MATCH (n) " +
                        "WHERE n.name CONTAINS $keyword " +
                        "RETURN n LIMIT $limit";
            }

            Result result = session.run(cypher, params);

            result.stream().forEach(record -> {
                Node node = record.get("n").asNode();
                vertices.add(nodeToVertex(node));
            });

            log.info("搜索到 {} 个节点，关键词: {}", vertices.size(), keyword);
            return vertices;
        } catch (Exception e) {
            log.error("按关键词搜索节点失败: keyword={}, knowledgeId={}", keyword, knowledgeId, e);
            return vertices;
        }
    }

    @Override
    public boolean updateVertex(GraphVertex vertex) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n {id: $id, knowledgeId: $knowledgeId}) " +
                    "SET n.name = $name, n.description = $description, " +
                    "n.properties = $properties, n.confidence = $confidence " +
                    "RETURN n";

            Result result = session.run(cypher, parameters(
                    "id", vertex.getId(),
                    "knowledgeId", vertex.getKnowledgeId(),
                    "name", vertex.getName(),
                    "description", vertex.getDescription(),
                    "properties", vertex.getProperties(),
                    "confidence", vertex.getConfidence()
            ));

            return result.hasNext();
        } catch (Exception e) {
            log.error("更新节点失败: {}", vertex, e);
            return false;
        }
    }

    @Override
    public boolean deleteVertex(String nodeId, String graphUuid, boolean includeEdges) {
        try (Session session = neo4jDriver.session()) {
            String cypher;
            if (includeEdges) {
                cypher = "MATCH (n {id: $nodeId, knowledgeId: $graphUuid}) DETACH DELETE n";
            } else {
                cypher = "MATCH (n {id: $nodeId, knowledgeId: $graphUuid}) DELETE n";
            }

            session.run(cypher, parameters(
                    "nodeId", nodeId,
                    "graphUuid", graphUuid
            ));

            return true;
        } catch (Exception e) {
            log.error("删除节点失败: nodeId={}, graphUuid={}", nodeId, graphUuid, e);
            return false;
        }
    }

    // ==================== 关系操作 ====================

    @Override
    public boolean addEdge(GraphEdge edge) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (s {id: $startNodeId, knowledgeId: $knowledgeId}) " +
                    "MATCH (t {id: $endNodeId, knowledgeId: $knowledgeId}) " +
                    "CREATE (s)-[r:" + edge.getLabel() + " {" +
                    "id: $id, description: $description, weight: $weight, " +
                    "docIds: $docIds, properties: $properties, confidence: $confidence" +
                    "}]->(t) RETURN r";

            Result result = session.run(cypher, parameters(
                    "startNodeId", edge.getSourceNodeId(),
                    "endNodeId", edge.getTargetNodeId(),
                    "knowledgeId", edge.getKnowledgeId(),
                    "id", edge.getEdgeId(),
                    "description", edge.getDescription(),
                    "weight", edge.getWeight(),
                    "docIds", edge.getDocIds(),
                    "properties", edge.getProperties(),
                    "confidence", edge.getConfidence()
            ));

            return result.hasNext();
        } catch (Exception e) {
            log.error("添加关系失败: {}", edge, e);
            return false;
        }
    }

    @Override
    public int addEdges(List<GraphEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return 0;
        }

        log.info("🔄 开始批量添加 {} 个关系到Neo4j", edges.size());
        int successCount = 0;
        int failedCount = 0;
        int batchSize = graphProperties.getBatchSize();

        try (Session session = neo4jDriver.session()) {
            for (int i = 0; i < edges.size(); i += batchSize) {
                List<GraphEdge> batch = edges.subList(
                        i, Math.min(i + batchSize, edges.size())
                );

                int batchIndex = i / batchSize + 1;
                log.debug("处理第 {}/{} 批，本批 {} 个关系",
                        batchIndex, (edges.size() + batchSize - 1) / batchSize, batch.size());

                successCount += session.writeTransaction(tx -> {
                    int count = 0;
                    for (GraphEdge edge : batch) {
                        try {
                            String cypher = "MATCH (s {id: $startNodeId, knowledgeId: $knowledgeId}) " +
                                    "MATCH (t {id: $endNodeId, knowledgeId: $knowledgeId}) " +
                                    "CREATE (s)-[r:" + edge.getLabel() + " {" +
                                    "id: $id, knowledgeId: $knowledgeId, description: $description, weight: $weight, " +
                                    "docIds: $docIds, properties: $properties, confidence: $confidence" +
                                    "}]->(t)";

                            Result result = tx.run(cypher, parameters(
                                    "startNodeId", edge.getSourceNodeId(),
                                    "endNodeId", edge.getTargetNodeId(),
                                    "knowledgeId", edge.getKnowledgeId(),
                                    "id", edge.getEdgeId(),
                                    "description", edge.getDescription(),
                                    "weight", edge.getWeight(),
                                    "docIds", edge.getDocIds(),
                                    "properties", edge.getProperties(),
                                    "confidence", edge.getConfidence()
                            ));

                            // ⭐ 检查是否真的创建了关系
                            if (result.consume().counters().relationshipsCreated() > 0) {
                                count++;
                            } else {
                                log.warn("⚠️ 关系创建失败（节点未找到）: {} -> {} (knowledgeId: {})",
                                        edge.getSourceNodeId(), edge.getTargetNodeId(), edge.getKnowledgeId());
                            }
                        } catch (Exception e) {
                            log.error("❌ 添加单个关系失败: {} -> {}, 错误: {}",
                                    edge.getSourceNodeId(), edge.getTargetNodeId(), e.getMessage());
                        }
                    }
                    return count;
                });
            }
        } catch (Exception e) {
            log.error("❌ 批量添加关系失败", e);
        }

        failedCount = edges.size() - successCount;
        log.info("✅ 关系添加完成: 成功 {}/{}, 失败 {}", successCount, edges.size(), failedCount);

        if (failedCount > 0) {
            log.warn("⚠️ 有 {} 个关系添加失败，可能原因：", failedCount);
            log.warn("   1. 源节点或目标节点不存在");
            log.warn("   2. sourceNodeId/targetNodeId 不匹配");
            log.warn("   3. knowledgeId 不匹配");
        }

        return successCount;
    }

    @Override
    public GraphEdge getEdge(String edgeId, String graphUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (s)-[r]->(t) " +
                    "WHERE r.id = $edgeId AND r.knowledgeId = $graphUuid " +
                    "RETURN s, r, t";

            Result result = session.run(cypher, parameters(
                    "edgeId", edgeId,
                    "graphUuid", graphUuid
            ));

            if (result.hasNext()) {
                Record record = result.single();
                return relationshipToEdge(
                        record.get("s").asNode(),
                        record.get("r").asRelationship(),
                        record.get("t").asNode()
                );
            }
            return null;
        } catch (Exception e) {
            log.error("获取关系失败: edgeId={}, graphUuid={}", edgeId, graphUuid, e);
            return null;
        }
    }

    @Override
    public List<GraphEdge> searchEdges(String graphUuid, String sourceNodeId, String targetNodeId, Integer limit) {
        try (Session session = neo4jDriver.session()) {
            StringBuilder cypher = new StringBuilder("MATCH (s)-[r]->(t) WHERE r.knowledgeId = $graphUuid");

            Map<String, Object> params = new HashMap<>();
            params.put("graphUuid", graphUuid);

            if (sourceNodeId != null && !sourceNodeId.isEmpty()) {
                cypher.append(" AND s.id = $sourceNodeId");
                params.put("sourceNodeId", sourceNodeId);
            }

            if (targetNodeId != null && !targetNodeId.isEmpty()) {
                cypher.append(" AND t.id = $targetNodeId");
                params.put("targetNodeId", targetNodeId);
            }

            cypher.append(" RETURN s, r, t");

            if (limit != null && limit > 0) {
                cypher.append(" LIMIT $limit");
                params.put("limit", limit);
            }

            Result result = session.run(cypher.toString(), params);

            return result.stream()
                    .map(record -> relationshipToEdge(
                            record.get("s").asNode(),
                            record.get("r").asRelationship(),
                            record.get("t").asNode()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索关系失败: graphUuid={}", graphUuid, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphEdge> getNodeEdges(String nodeId, String graphUuid, String direction) {
        try (Session session = neo4jDriver.session()) {
            String cypher;
            switch (direction.toUpperCase()) {
                case "IN":
                    cypher = "MATCH (s)-[r]->(t {id: $nodeId, knowledgeId: $graphUuid}) RETURN s, r, t";
                    break;
                case "OUT":
                    cypher = "MATCH (s {id: $nodeId, knowledgeId: $graphUuid})-[r]->(t) RETURN s, r, t";
                    break;
                case "BOTH":
                default:
                    cypher = "MATCH (s)-[r]-(t {id: $nodeId, knowledgeId: $graphUuid}) RETURN s, r, t";
                    break;
            }

            Result result = session.run(cypher, parameters(
                    "nodeId", nodeId,
                    "graphUuid", graphUuid
            ));

            return result.stream()
                    .map(record -> relationshipToEdge(
                            record.get("s").asNode(),
                            record.get("r").asRelationship(),
                            record.get("t").asNode()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取节点关系失败: nodeId={}, graphUuid={}", nodeId, graphUuid, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateEdge(GraphEdge edge) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH ()-[r {id: $id, knowledgeId: $knowledgeId}]->() " +
                    "SET r.description = $description, r.weight = $weight, " +
                    "r.properties = $properties, r.confidence = $confidence " +
                    "RETURN r";

            Result result = session.run(cypher, parameters(
                    "id", edge.getEdgeId(),
                    "knowledgeId", edge.getKnowledgeId(),
                    "description", edge.getDescription(),
                    "weight", edge.getWeight(),
                    "properties", edge.getProperties(),
                    "confidence", edge.getConfidence()
            ));

            return result.hasNext();
        } catch (Exception e) {
            log.error("更新关系失败: {}", edge, e);
            return false;
        }
    }

    @Override
    public boolean deleteEdge(String edgeId, String graphUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH ()-[r {id: $edgeId, knowledgeId: $graphUuid}]->() DELETE r";

            session.run(cypher, parameters(
                    "edgeId", edgeId,
                    "graphUuid", graphUuid
            ));

            return true;
        } catch (Exception e) {
            log.error("删除关系失败: edgeId={}, graphUuid={}", edgeId, graphUuid, e);
            return false;
        }
    }

    // ==================== 图谱管理 ====================

    @Override
    public boolean createGraphSchema(String graphUuid) {
        try (Session session = neo4jDriver.session()) {
            // 创建索引以提高查询性能 - 使用正确的Neo4j 4.x/5.x语法
            session.run("CREATE INDEX entity_id_index IF NOT EXISTS FOR (n:Entity) ON (n.id)");
            session.run("CREATE INDEX entity_knowledge_id_index IF NOT EXISTS FOR (n:Entity) ON (n.knowledgeId)");
            session.run("CREATE INDEX entity_name_index IF NOT EXISTS FOR (n:Entity) ON (n.name)");

            // 为关系也创建索引
            session.run("CREATE INDEX relation_id_index IF NOT EXISTS FOR ()-[r:RELATION]-() ON (r.id)");
            session.run("CREATE INDEX relation_type_index IF NOT EXISTS FOR ()-[r:RELATION]-() ON (r.type)");

            log.info("图谱Schema创建成功: graphUuid={}", graphUuid);
            return true;
        } catch (Exception e) {
            log.error("创建图谱Schema失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    public boolean deleteGraph(String graphUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n {knowledgeId: $graphUuid}) DETACH DELETE n";

            session.run(cypher, parameters("graphUuid", graphUuid));

            log.info("图谱数据删除成功: graphUuid={}", graphUuid);
            return true;
        } catch (Exception e) {
            log.error("删除图谱数据失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getGraphStatistics(String graphUuid) {
        Map<String, Object> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            // 统计节点数
            Result nodeResult = session.run(
                    "MATCH (n {knowledgeId: $graphUuid}) RETURN count(n) as count",
                    parameters("graphUuid", graphUuid)
            );
            stats.put("nodeCount", nodeResult.single().get("count").asInt());

            // 统计关系数
            Result relResult = session.run(
                    "MATCH ()-[r {knowledgeId: $graphUuid}]->() RETURN count(r) as count",
                    parameters("graphUuid", graphUuid)
            );
            stats.put("relationshipCount", relResult.single().get("count").asInt());

        } catch (Exception e) {
            log.error("获取图谱统计信息失败: graphUuid={}", graphUuid, e);
        }

        return stats;
    }

    // ==================== 高级查询 ====================

    @Override
    public List<List<GraphVertex>> findPaths(String sourceNodeId, String targetNodeId, String graphUuid, Integer maxDepth) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH path = (s {id: $sourceNodeId, knowledgeId: $graphUuid})" +
                    "-[*1.." + (maxDepth != null ? maxDepth : 5) + "]->" +
                    "(t {id: $targetNodeId, knowledgeId: $graphUuid}) " +
                    "RETURN nodes(path) as path LIMIT 10";

            Result result = session.run(cypher, parameters(
                    "sourceNodeId", sourceNodeId,
                    "targetNodeId", targetNodeId,
                    "graphUuid", graphUuid
            ));

            List<List<GraphVertex>> paths = new ArrayList<>();
            result.stream().forEach(record -> {
                List<GraphVertex> path = record.get("path").asList(
                        value -> nodeToVertex(value.asNode())
                );
                paths.add(path);
            });

            return paths;
        } catch (Exception e) {
            log.error("查找路径失败: source={}, target={}, graphUuid={}", sourceNodeId, targetNodeId, graphUuid, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphVertex> findNeighbors(String nodeId, String graphUuid, Integer depth) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (s {id: $nodeId, knowledgeId: $graphUuid})" +
                    "-[*1.." + (depth != null ? depth : 1) + "]-(neighbor) " +
                    "RETURN DISTINCT neighbor";

            Result result = session.run(cypher, parameters(
                    "nodeId", nodeId,
                    "graphUuid", graphUuid
            ));

            return result.stream()
                    .map(record -> nodeToVertex(record.get("neighbor").asNode()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查找邻居节点失败: nodeId={}, graphUuid={}", nodeId, graphUuid, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> executeCypher(String cypher, Map<String, Object> params) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params != null ? params : Collections.emptyMap());

            return result.stream()
                    .map(Record::asMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("执行Cypher查询失败: {}", cypher, e);
            return Collections.emptyList();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * Neo4j Node 转换为 GraphVertex
     */
    private GraphVertex nodeToVertex(Node node) {
        GraphVertex vertex = new GraphVertex();
        vertex.setNodeId(node.get("id").asString(null));
        vertex.setLabel(node.labels().iterator().next());
        vertex.setName(node.get("name").asString(null));
        vertex.setDescription(node.get("description").asString(null));
        vertex.setKnowledgeId(node.get("knowledgeId").asString(null));
        vertex.setDocIds(node.get("docIds").asString(null));

        // 处理 confidence（可能为空）
        if (node.containsKey("confidence") && !node.get("confidence").isNull()) {
            vertex.setConfidence(node.get("confidence").asDouble());
        }

        // 处理 properties（转换为JSON字符串）
        if (node.containsKey("properties") && !node.get("properties").isNull()) {
            Map<String, Object> propsMap = node.get("properties").asMap();
            vertex.setProperties(JSON.toJSONString(propsMap));
        }

        return vertex;
    }

    /**
     * Neo4j Relationship 转换为 GraphEdge
     */
    private GraphEdge relationshipToEdge(Node source, Relationship rel, Node target) {
        GraphEdge edge = new GraphEdge();
        edge.setEdgeId(rel.get("id").asString(null));
        edge.setLabel(rel.type());
        edge.setSourceNodeId(source.get("id").asString(null));
        edge.setTargetNodeId(target.get("id").asString(null));
        edge.setDescription(rel.get("description").asString(null));
        edge.setKnowledgeId(rel.get("knowledgeId").asString(null));
        edge.setDocIds(rel.get("docIds").asString(null));

        // 处理 weight（可能为空）
        if (rel.containsKey("weight") && !rel.get("weight").isNull()) {
            edge.setWeight(rel.get("weight").asDouble());
        }

        // 处理 confidence（可能为空）
        if (rel.containsKey("confidence") && !rel.get("confidence").isNull()) {
            edge.setConfidence(rel.get("confidence").asDouble());
        }

        // 处理 properties（转换为JSON字符串）
        if (rel.containsKey("properties") && !rel.get("properties").isNull()) {
            Map<String, Object> propsMap = rel.get("properties").asMap();
            edge.setProperties(JSON.toJSONString(propsMap));
        }

        return edge;
    }

    // ==================== 新增的方法实现 ====================

    @Override
    public List<GraphVertex> queryVerticesByKnowledgeId(String knowledgeId, Integer limit) {
        List<GraphVertex> vertices = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (n {knowledgeId: $knowledgeId}) " +
                    "RETURN n LIMIT $limit";

            Result result = session.run(cypher, parameters(
                    "knowledgeId", knowledgeId,
                    "limit", limit
            ));

            result.stream().forEach(record -> {
                Node node = record.get("n").asNode();
                vertices.add(nodeToVertex(node));
            });

            log.info("查询到 {} 个节点，知识库ID: {}", vertices.size(), knowledgeId);
            return vertices;
        } catch (Exception e) {
            log.error("查询节点失败", e);
            return vertices;
        }
    }

    @Override
    public List<GraphEdge> queryEdgesByKnowledgeId(String knowledgeId, Integer limit) {
        List<GraphEdge> edges = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            // ⭐ 修复：通过节点的 knowledgeId 过滤关系，兼容旧数据
            String cypher = "MATCH (s {knowledgeId: $knowledgeId})-[r]->(t {knowledgeId: $knowledgeId}) " +
                    "RETURN s, r, t LIMIT $limit";

            log.info("🔍 开始查询关系 - knowledgeId: {}, limit: {}", knowledgeId, limit);
            log.debug("执行Cypher: {}", cypher);

            Result result = session.run(cypher, parameters(
                    "knowledgeId", knowledgeId,
                    "limit", limit
            ));

            int count = 0;
            while (result.hasNext()) {
                Record record = result.next();
                Node source = record.get("s").asNode();
                Relationship rel = record.get("r").asRelationship();
                Node target = record.get("t").asNode();

                // 调试：打印关系详情
                if (count < 3) {  // 只打印前3个
                    log.debug("关系#{} - 类型: {}, 起点: {} ({}), 终点: {} ({})",
                            count + 1,
                            rel.type(),
                            source.get("name").asString(),
                            source.get("id").asString(),
                            target.get("name").asString(),
                            target.get("id").asString()
                    );
                }

                edges.add(relationshipToEdge(source, rel, target));
                count++;
            }

            log.info("✅ 查询到 {} 个关系，知识库ID: {}", edges.size(), knowledgeId);
            return edges;
        } catch (Exception e) {
            log.error("❌ 查询关系失败 - knowledgeId: {}", knowledgeId, e);
            return edges;
        }
    }

    @Override
    public boolean deleteByKnowledgeId(String knowledgeId) {
        try (Session session = neo4jDriver.session()) {
            log.info("🗑️ 开始删除知识库图谱数据，knowledgeId: {}", knowledgeId);

            // ⭐ 先删除关系（通过节点的knowledgeId过滤，兼容旧数据）
            String deleteRelsQuery = "MATCH (s {knowledgeId: $knowledgeId})-[r]->(t {knowledgeId: $knowledgeId}) DELETE r";
            Result relResult = session.run(deleteRelsQuery, parameters("knowledgeId", knowledgeId));
            int deletedRels = relResult.consume().counters().relationshipsDeleted();
            log.info("✅ 删除了 {} 个关系", deletedRels);

            // 再删除节点
            String deleteNodesQuery = "MATCH (n {knowledgeId: $knowledgeId}) DELETE n";
            Result nodeResult = session.run(deleteNodesQuery, parameters("knowledgeId", knowledgeId));
            int deletedNodes = nodeResult.consume().counters().nodesDeleted();
            log.info("✅ 删除了 {} 个节点", deletedNodes);

            log.info("✅ 删除知识库图谱数据成功，knowledgeId: {}, 节点: {}, 关系: {}",
                    knowledgeId, deletedNodes, deletedRels);
            return true;
        } catch (Exception e) {
            log.error("❌ 删除知识库图谱数据失败，knowledgeId: {}", knowledgeId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getStatistics(String knowledgeId) {
        Map<String, Object> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            // 统计节点数
            String nodeCountQuery = "MATCH (n {knowledgeId: $knowledgeId}) RETURN count(n) as count";
            Result nodeResult = session.run(nodeCountQuery, parameters("knowledgeId", knowledgeId));
            int nodeCount = 0;
            if (nodeResult.hasNext()) {
                nodeCount = nodeResult.single().get("count").asInt();
                stats.put("nodeCount", nodeCount);
                stats.put("totalNodes", nodeCount);  // ⭐ 前端需要的字段
            }

            // ⭐ 统计关系数（通过节点过滤，与查询/删除逻辑一致）
            String relCountQuery = "MATCH (s {knowledgeId: $knowledgeId})-[r]->(t {knowledgeId: $knowledgeId}) RETURN count(r) as count";
            Result relResult = session.run(relCountQuery, parameters("knowledgeId", knowledgeId));
            int relCount = 0;
            if (relResult.hasNext()) {
                relCount = relResult.single().get("count").asInt();
                stats.put("relationshipCount", relCount);
                stats.put("totalEdges", relCount);  // ⭐ 前端需要的字段
            }

            // 统计节点类型分布
            String labelQuery = "MATCH (n {knowledgeId: $knowledgeId}) " +
                    "RETURN labels(n)[0] as label, count(*) as count " +
                    "ORDER BY count DESC LIMIT 10";
            Result labelResult = session.run(labelQuery, parameters("knowledgeId", knowledgeId));

            Map<String, Integer> labelDistribution = new HashMap<>();
            labelResult.stream().forEach(record -> {
                String label = record.get("label").asString();
                int count = record.get("count").asInt();
                labelDistribution.put(label, count);
            });
            stats.put("labelDistribution", labelDistribution);
            stats.put("entityTypes", labelDistribution);  // ⭐ 前端需要的字段

            log.info("📊 获取图谱统计信息: knowledgeId={}, 节点={}, 关系={}, 类型={}",
                    knowledgeId, nodeCount, relCount, labelDistribution.size());
            return stats;
        } catch (Exception e) {
            log.error("❌ 获取统计信息失败: knowledgeId={}", knowledgeId, e);
            return stats;
        }
    }

    @Override
    public List<GraphVertex> getNeighbors(String nodeId, String knowledgeId, Integer limit) {
        List<GraphVertex> neighbors = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            String cypher;
            Map<String, Object> params = new HashMap<>();
            params.put("nodeId", nodeId);
            params.put("limit", limit);

            if (knowledgeId != null && !knowledgeId.isEmpty()) {
                cypher = "MATCH (n {id: $nodeId, knowledgeId: $knowledgeId})-[]-(neighbor {knowledgeId: $knowledgeId}) " +
                        "RETURN DISTINCT neighbor LIMIT $limit";
                params.put("knowledgeId", knowledgeId);
            } else {
                cypher = "MATCH (n {id: $nodeId})-[]-(neighbor) " +
                        "RETURN DISTINCT neighbor LIMIT $limit";
            }

            Result result = session.run(cypher, params);

            result.stream().forEach(record -> {
                Node node = record.get("neighbor").asNode();
                neighbors.add(nodeToVertex(node));
            });

            log.info("查询到 {} 个邻居节点", neighbors.size());
            return neighbors;
        } catch (Exception e) {
            log.error("查询邻居节点失败", e);
            return neighbors;
        }
    }

    @Override
    public List<List<GraphVertex>> findPaths(String startNodeId, String endNodeId, Integer maxDepth) {
        List<List<GraphVertex>> paths = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH path = (start {id: $startNodeId})-[*1.." + maxDepth + "]-(end {id: $endNodeId}) " +
                    "RETURN nodes(path) as pathNodes " +
                    "LIMIT 10";

            Result result = session.run(cypher, parameters(
                    "startNodeId", startNodeId,
                    "endNodeId", endNodeId
            ));

            result.stream().forEach(record -> {
                List<Object> pathNodes = record.get("pathNodes").asList();
                List<GraphVertex> path = new ArrayList<>();

                for (Object obj : pathNodes) {
                    if (obj instanceof Node) {
                        path.add(nodeToVertex((Node) obj));
                    }
                }

                if (!path.isEmpty()) {
                    paths.add(path);
                }
            });

            log.info("查询到 {} 条路径", paths.size());
            return paths;
        } catch (Exception e) {
            log.error("查询路径失败", e);
            return paths;
        }
    }
}
