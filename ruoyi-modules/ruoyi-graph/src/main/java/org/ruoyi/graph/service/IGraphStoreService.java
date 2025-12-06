package org.ruoyi.graph.service;

import org.ruoyi.graph.domain.GraphEdge;
import org.ruoyi.graph.domain.GraphVertex;

import java.util.List;

/**
 * 图存储服务接口
 * 核心服务：负责与Neo4j图数据库交互
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface IGraphStoreService {

    // ==================== 节点操作 ====================

    /**
     * 添加单个节点
     *
     * @param vertex 节点信息
     * @return 是否成功
     */
    boolean addVertex(GraphVertex vertex);

    /**
     * 批量添加节点
     *
     * @param vertices 节点列表
     * @return 成功添加的节点数
     */
    int addVertices(List<GraphVertex> vertices);

    /**
     * 获取节点信息
     *
     * @param nodeId    节点ID
     * @param graphUuid 图谱UUID
     * @return 节点信息
     */
    GraphVertex getVertex(String nodeId, String graphUuid);

    /**
     * 根据条件搜索节点
     *
     * @param graphUuid 图谱UUID
     * @param label     节点标签（可选）
     * @param limit     返回数量限制
     * @return 节点列表
     */
    List<GraphVertex> searchVertices(String graphUuid, String label, Integer limit);

    /**
     * 根据名称搜索节点
     *
     * @param graphUuid 图谱UUID
     * @param name      节点名称
     * @return 节点列表
     */
    List<GraphVertex> searchVerticesByName(String graphUuid, String name);

    /**
     * 根据关键词和知识库ID搜索节点
     *
     * @param keyword     关键词
     * @param knowledgeId 知识库ID（可选）
     * @param limit       限制数量
     * @return 节点列表
     */
    List<GraphVertex> searchVerticesByName(String keyword, String knowledgeId, Integer limit);

    /**
     * 根据知识库ID查询节点
     *
     * @param knowledgeId 知识库ID
     * @param limit       限制数量
     * @return 节点列表
     */
    List<GraphVertex> queryVerticesByKnowledgeId(String knowledgeId, Integer limit);

    /**
     * 更新节点信息
     *
     * @param vertex 节点信息
     * @return 是否成功
     */
    boolean updateVertex(GraphVertex vertex);

    /**
     * 删除节点
     *
     * @param nodeId       节点ID
     * @param graphUuid    图谱UUID
     * @param includeEdges 是否同时删除相关关系
     * @return 是否成功
     */
    boolean deleteVertex(String nodeId, String graphUuid, boolean includeEdges);

    // ==================== 关系操作 ====================

    /**
     * 添加关系
     *
     * @param edge 关系信息
     * @return 是否成功
     */
    boolean addEdge(GraphEdge edge);

    /**
     * 批量添加关系
     *
     * @param edges 关系列表
     * @return 成功添加的关系数
     */
    int addEdges(List<GraphEdge> edges);

    /**
     * 获取关系信息
     *
     * @param edgeId    关系ID
     * @param graphUuid 图谱UUID
     * @return 关系信息
     */
    GraphEdge getEdge(String edgeId, String graphUuid);

    /**
     * 搜索关系
     *
     * @param graphUuid    图谱UUID
     * @param sourceNodeId 源节点ID（可选）
     * @param targetNodeId 目标节点ID（可选）
     * @param limit        返回数量限制
     * @return 关系列表
     */
    List<GraphEdge> searchEdges(String graphUuid, String sourceNodeId, String targetNodeId, Integer limit);

    /**
     * 根据知识库ID查询关系
     *
     * @param knowledgeId 知识库ID
     * @param limit       限制数量
     * @return 关系列表
     */
    List<GraphEdge> queryEdgesByKnowledgeId(String knowledgeId, Integer limit);

    /**
     * 获取节点的所有关系
     *
     * @param nodeId    节点ID
     * @param graphUuid 图谱UUID
     * @param direction 方向: IN(入边), OUT(出边), BOTH(双向)
     * @return 关系列表
     */
    List<GraphEdge> getNodeEdges(String nodeId, String graphUuid, String direction);

    /**
     * 更新关系信息
     *
     * @param edge 关系信息
     * @return 是否成功
     */
    boolean updateEdge(GraphEdge edge);

    /**
     * 删除关系
     *
     * @param edgeId    关系ID
     * @param graphUuid 图谱UUID
     * @return 是否成功
     */
    boolean deleteEdge(String edgeId, String graphUuid);

    // ==================== 图谱管理 ====================

    /**
     * 创建图谱Schema
     *
     * @param graphUuid 图谱UUID
     * @return 是否成功
     */
    boolean createGraphSchema(String graphUuid);

    /**
     * 删除整个图谱数据
     *
     * @param graphUuid 图谱UUID
     * @return 是否成功
     */
    boolean deleteGraph(String graphUuid);

    /**
     * 根据知识库ID删除图谱数据
     *
     * @param knowledgeId 知识库ID
     * @return 是否成功
     */
    boolean deleteByKnowledgeId(String knowledgeId);

    /**
     * 获取图谱统计信息
     *
     * @param graphUuid 图谱UUID
     * @return 统计信息 {nodeCount, relationshipCount}
     */
    java.util.Map<String, Object> getGraphStatistics(String graphUuid);

    /**
     * 根据知识库ID获取统计信息
     *
     * @param knowledgeId 知识库ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getStatistics(String knowledgeId);

    // ==================== 高级查询 ====================

    /**
     * 查找两个节点之间的路径
     *
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param graphUuid    图谱UUID
     * @param maxDepth     最大深度
     * @return 路径列表
     */
    List<List<GraphVertex>> findPaths(String sourceNodeId, String targetNodeId, String graphUuid, Integer maxDepth);

    /**
     * 查找路径（简化版）
     *
     * @param startNodeId 起始节点ID
     * @param endNodeId   结束节点ID
     * @param maxDepth    最大深度
     * @return 路径列表
     */
    List<List<GraphVertex>> findPaths(String startNodeId, String endNodeId, Integer maxDepth);

    /**
     * 查找节点的邻居节点
     *
     * @param nodeId    节点ID
     * @param graphUuid 图谱UUID
     * @param depth     深度（几度关系）
     * @return 邻居节点列表
     */
    List<GraphVertex> findNeighbors(String nodeId, String graphUuid, Integer depth);

    /**
     * 获取节点的邻居（简化版）
     *
     * @param nodeId      节点ID
     * @param knowledgeId 知识库ID（可选）
     * @param limit       限制数量
     * @return 邻居节点列表
     */
    List<GraphVertex> getNeighbors(String nodeId, String knowledgeId, Integer limit);

    /**
     * 执行自定义Cypher查询
     *
     * @param cypher Cypher查询语句
     * @param params 参数
     * @return 查询结果
     */
    List<java.util.Map<String, Object>> executeCypher(String cypher, java.util.Map<String, Object> params);
}
