package org.ruoyi.util;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

/**
 * Neo4j连接测试工具类
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class Neo4jTestUtil {

    private final Driver driver;

    public Neo4jTestUtil(Driver driver) {
        this.driver = driver;
    }

    /**
     * 测试Neo4j连接
     *
     * @return 测试结果
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            // 1. 测试基本连接
            Result pingResult = session.run("RETURN 1 as num");
            if (pingResult.hasNext()) {
                Record record = pingResult.single();
                result.put("connection", "SUCCESS");
                result.put("pingResult", record.get("num").asInt());
            }

            // 2. 获取数据库信息
            Result dbInfoResult = session.run("CALL dbms.components() YIELD name, versions, edition");
            if (dbInfoResult.hasNext()) {
                Record dbInfo = dbInfoResult.single();
                result.put("neo4jVersion", dbInfo.get("versions").asList().get(0));
                result.put("neo4jEdition", dbInfo.get("edition").asString());
            }

            // 3. 测试节点数量
            Result countResult = session.run("MATCH (n) RETURN count(n) as count");
            if (countResult.hasNext()) {
                result.put("totalNodes", countResult.single().get("count").asInt());
            }

            log.info("Neo4j连接测试成功: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Neo4j连接测试失败", e);
            result.put("connection", "FAILED");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 创建测试节点
     *
     * @param name 节点名称
     * @return 创建的节点信息
     */
    public Map<String, Object> createTestNode(String name) {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            Result queryResult = session.run(
                    "CREATE (p:TestNode {name: $name, createTime: datetime()}) RETURN p",
                    parameters("name", name)
            );

            if (queryResult.hasNext()) {
                Record record = queryResult.single();
                Node node = record.get("p").asNode();

                result.put("success", true);
                result.put("nodeId", node.elementId());
                result.put("labels", node.labels());
                result.put("properties", node.asMap());

                log.info("测试节点创建成功: {}", result);
            }

            return result;

        } catch (Exception e) {
            log.error("创建测试节点失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 查询测试节点
     *
     * @param name 节点名称
     * @return 节点信息
     */
    public Map<String, Object> queryTestNode(String name) {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            Result queryResult = session.run(
                    "MATCH (p:TestNode {name: $name}) RETURN p",
                    parameters("name", name)
            );

            if (queryResult.hasNext()) {
                Record record = queryResult.single();
                Node node = record.get("p").asNode();

                result.put("found", true);
                result.put("nodeId", node.elementId());
                result.put("properties", node.asMap());
            } else {
                result.put("found", false);
            }

            return result;

        } catch (Exception e) {
            log.error("查询测试节点失败", e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 删除所有测试节点
     *
     * @return 删除的节点数量
     */
    public Map<String, Object> deleteAllTestNodes() {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            Result queryResult = session.run(
                    "MATCH (p:TestNode) DELETE p RETURN count(p) as deleted"
            );

            if (queryResult.hasNext()) {
                int deleted = queryResult.single().get("deleted").asInt();
                result.put("success", true);
                result.put("deletedCount", deleted);
                log.info("删除测试节点数量: {}", deleted);
            }

            return result;

        } catch (Exception e) {
            log.error("删除测试节点失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 创建测试关系
     *
     * @param sourceName 源节点名称
     * @param targetName 目标节点名称
     * @return 创建结果
     */
    public Map<String, Object> createTestRelationship(String sourceName, String targetName) {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            // 先创建两个节点
            session.run(
                    "MERGE (s:TestNode {name: $sourceName}) " +
                            "MERGE (t:TestNode {name: $targetName})",
                    parameters("sourceName", sourceName, "targetName", targetName)
            );

            // 创建关系
            Result queryResult = session.run(
                    "MATCH (s:TestNode {name: $sourceName}) " +
                            "MATCH (t:TestNode {name: $targetName}) " +
                            "CREATE (s)-[r:TEST_RELATION {createTime: datetime()}]->(t) " +
                            "RETURN r",
                    parameters("sourceName", sourceName, "targetName", targetName)
            );

            if (queryResult.hasNext()) {
                result.put("success", true);
                result.put("source", sourceName);
                result.put("target", targetName);
                log.info("测试关系创建成功: {} -> {}", sourceName, targetName);
            }

            return result;

        } catch (Exception e) {
            log.error("创建测试关系失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 获取Neo4j统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            // 节点总数
            Result nodeCountResult = session.run("MATCH (n) RETURN count(n) as count");
            result.put("totalNodes", nodeCountResult.single().get("count").asInt());

            // 关系总数
            Result relCountResult = session.run("MATCH ()-[r]->() RETURN count(r) as count");
            result.put("totalRelationships", relCountResult.single().get("count").asInt());

            // 节点标签分布
            Result labelResult = session.run(
                    "MATCH (n) RETURN labels(n) as label, count(*) as count ORDER BY count DESC LIMIT 10"
            );
            Map<String, Integer> labelDistribution = new HashMap<>();
            labelResult.stream().forEach(record -> {
                String label = record.get("label").asList().toString();
                int count = record.get("count").asInt();
                labelDistribution.put(label, count);
            });
            result.put("labelDistribution", labelDistribution);

            log.info("Neo4j统计信息: {}", result);
            return result;

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 调试关系查询 - 详细诊断指定知识库的关系数据
     *
     * @param knowledgeId 知识库ID
     * @return 调试信息
     */
    public Map<String, Object> debugRelationships(String knowledgeId) {
        Map<String, Object> result = new HashMap<>();

        try (Session session = driver.session()) {
            log.info("🔍 开始调试关系查询 - knowledgeId: {}", knowledgeId);

            // 1. 检查该知识库的节点数量
            Result nodeCountResult = session.run(
                    "MATCH (n {knowledgeId: $knowledgeId}) RETURN count(n) as count",
                    parameters("knowledgeId", knowledgeId)
            );
            int nodeCount = nodeCountResult.single().get("count").asInt();
            result.put("nodeCount", nodeCount);
            log.info("✅ 该知识库节点数量: {}", nodeCount);

            // 2. 检查总关系数量（不限制知识库）
            Result totalRelResult = session.run("MATCH ()-[r]->() RETURN count(r) as count");
            int totalRelCount = totalRelResult.single().get("count").asInt();
            result.put("totalRelationships", totalRelCount);
            log.info("✅ 数据库总关系数量: {}", totalRelCount);

            // 3. 检查带 knowledgeId 的关系数量（旧查询方式）
            Result relWithKnowledgeIdResult = session.run(
                    "MATCH ()-[r {knowledgeId: $knowledgeId}]->() RETURN count(r) as count",
                    parameters("knowledgeId", knowledgeId)
            );
            int relWithKnowledgeId = relWithKnowledgeIdResult.single().get("count").asInt();
            result.put("relationshipsWithKnowledgeId", relWithKnowledgeId);
            log.info("✅ 带 knowledgeId 的关系数量: {}", relWithKnowledgeId);

            // 4. 通过节点过滤关系数量（新查询方式）
            Result relByNodesResult = session.run(
                    "MATCH (s {knowledgeId: $knowledgeId})-[r]->(t {knowledgeId: $knowledgeId}) RETURN count(r) as count",
                    parameters("knowledgeId", knowledgeId)
            );
            int relByNodes = relByNodesResult.single().get("count").asInt();
            result.put("relationshipsByNodes", relByNodes);
            log.info("✅ 通过节点过滤的关系数量: {}", relByNodes);

            // 5. 采样前5个关系详情（如果有）
            if (relByNodes > 0) {
                Result sampleResult = session.run(
                        "MATCH (s {knowledgeId: $knowledgeId})-[r]->(t {knowledgeId: $knowledgeId}) " +
                                "RETURN s.name as sourceName, s.id as sourceId, type(r) as relType, " +
                                "t.name as targetName, t.id as targetId, r.knowledgeId as relKnowledgeId " +
                                "LIMIT 5",
                        parameters("knowledgeId", knowledgeId)
                );

                java.util.List<Map<String, Object>> samples = new java.util.ArrayList<>();
                sampleResult.stream().forEach(record -> {
                    Map<String, Object> sample = new HashMap<>();
                    sample.put("sourceName", record.get("sourceName").asString());
                    sample.put("sourceId", record.get("sourceId").asString());
                    sample.put("relationType", record.get("relType").asString());
                    sample.put("targetName", record.get("targetName").asString());
                    sample.put("targetId", record.get("targetId").asString());
                    sample.put("relationshipKnowledgeId",
                            record.get("relKnowledgeId").isNull() ? null : record.get("relKnowledgeId").asString());
                    samples.add(sample);
                });
                result.put("sampleRelationships", samples);
                log.info("✅ 采样到 {} 个关系", samples.size());
            }

            // 6. 检查是否有孤立节点（没有任何关系的节点）
            Result isolatedNodesResult = session.run(
                    "MATCH (n {knowledgeId: $knowledgeId}) " +
                            "WHERE NOT (n)-[]-() " +
                            "RETURN count(n) as count",
                    parameters("knowledgeId", knowledgeId)
            );
            int isolatedNodes = isolatedNodesResult.single().get("count").asInt();
            result.put("isolatedNodes", isolatedNodes);
            log.info("✅ 孤立节点数量: {}", isolatedNodes);

            // 7. 诊断结论
            Map<String, String> diagnosis = new HashMap<>();
            if (nodeCount == 0) {
                diagnosis.put("issue", "该知识库没有节点数据");
                diagnosis.put("solution", "请先构建图谱或检查 knowledgeId 是否正确");
            } else if (relByNodes == 0 && totalRelCount > 0) {
                diagnosis.put("issue", "数据库中有关系，但该知识库查询不到");
                diagnosis.put("solution", "可能是 knowledgeId 不匹配，检查关系的 knowledgeId 属性");
            } else if (relByNodes == 0) {
                diagnosis.put("issue", "该知识库没有关系数据");
                diagnosis.put("solution", "检查 LLM 抽取是否识别到实体关系");
            } else {
                diagnosis.put("status", "正常");
                diagnosis.put("message", "关系数据正常，可以正常查询");
            }
            result.put("diagnosis", diagnosis);

            log.info("🎯 调试完成: {}", diagnosis);
            return result;

        } catch (Exception e) {
            log.error("❌ 调试关系查询失败", e);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
