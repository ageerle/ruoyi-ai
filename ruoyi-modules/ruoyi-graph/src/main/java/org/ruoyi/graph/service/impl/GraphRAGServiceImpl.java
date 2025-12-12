package org.ruoyi.graph.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.graph.constants.GraphConstants;
import org.ruoyi.graph.domain.GraphEdge;
import org.ruoyi.graph.domain.GraphVertex;
import org.ruoyi.graph.dto.ExtractedEntity;
import org.ruoyi.graph.dto.ExtractedRelation;
import org.ruoyi.graph.dto.GraphExtractionResult;
import org.ruoyi.graph.service.IGraphExtractionService;
import org.ruoyi.graph.service.IGraphRAGService;
import org.ruoyi.graph.service.IGraphStoreService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GraphRAG服务实现
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphRAGServiceImpl implements IGraphRAGService {

    private final IGraphExtractionService graphExtractionService;
    private final IGraphStoreService graphStoreService;

    @Override
    public GraphExtractionResult ingestText(String text, String knowledgeId, Map<String, Object> metadata) {
        return ingestTextWithModel(text, knowledgeId, metadata, null);
    }

    @Override
    public GraphExtractionResult ingestTextWithModel(String text, String knowledgeId, Map<String, Object> metadata, String modelName) {
        log.info("开始将文本入库到图谱，知识库ID: {}, 模型: {}, 文本长度: {}",
                knowledgeId, modelName != null ? modelName : "默认", text.length());

        try {
            // 1. 从文本中抽取实体和关系
            GraphExtractionResult extractionResult;
            if (StrUtil.isNotBlank(modelName)) {
                extractionResult = graphExtractionService.extractFromTextWithModel(text, modelName);
            } else {
                extractionResult = graphExtractionService.extractFromText(text);
            }

            if (!extractionResult.getSuccess()) {
                log.error("实体抽取失败: {}", extractionResult.getErrorMessage());
                return extractionResult;
            }

            // 2. 将抽取的实体转换为图节点
            List<GraphVertex> vertices = convertEntitiesToVertices(
                    extractionResult.getEntities(),
                    knowledgeId,
                    metadata
            );

            // 3. 批量添加节点到Neo4j，并建立实体名称→nodeId的映射
            Map<String, String> entityNameToNodeIdMap = new HashMap<>();
            if (!vertices.isEmpty()) {
                int addedCount = graphStoreService.addVertices(vertices);
                log.info("成功添加 {} 个节点到图谱", addedCount);

                // ⭐ 建立映射：实体名称 → nodeId
                for (GraphVertex vertex : vertices) {
                    entityNameToNodeIdMap.put(vertex.getName(), vertex.getNodeId());
                }
                log.debug("建立实体名称映射: {} 个实体", entityNameToNodeIdMap.size());
            }

            // 4. 将抽取的关系转换为图边，使用映射填充nodeId
            List<GraphEdge> edges = convertRelationsToEdges(
                    extractionResult.getRelations(),
                    knowledgeId,
                    metadata,
                    entityNameToNodeIdMap  // ⭐ 传入映射
            );

            // 5. 批量添加关系到Neo4j
            if (!edges.isEmpty()) {
                int addedCount = graphStoreService.addEdges(edges);
                log.info("成功添加 {} 个关系到图谱", addedCount);
            }

            return extractionResult;

        } catch (Exception e) {
            log.error("文本入库失败", e);
            return GraphExtractionResult.builder()
                    .entities(new ArrayList<>())
                    .relations(new ArrayList<>())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GraphExtractionResult ingestDocument(String documentText, String knowledgeId, Map<String, Object> metadata) {
        return ingestDocumentWithModel(documentText, knowledgeId, metadata, null);
    }

    @Override
    public GraphExtractionResult ingestDocumentWithModel(String documentText, String knowledgeId, Map<String, Object> metadata, String modelName) {
        log.info("开始将文档入库到图谱，知识库ID: {}, 模型: {}, 文档长度: {}",
                knowledgeId, modelName != null ? modelName : "默认", documentText.length());

        // 如果文档较短，直接处理
        if (documentText.length() < GraphConstants.RAG_MAX_SEGMENT_SIZE_IN_TOKENS * 4) {
            return ingestTextWithModel(documentText, knowledgeId, metadata, modelName);
        }

        // 文档较长，需要分片处理
        List<String> chunks = splitDocument(documentText);
        log.info("文档已分割为 {} 个片段", chunks.size());

        // 合并结果
        List<ExtractedEntity> allEntities = new ArrayList<>();
        List<ExtractedRelation> allRelations = new ArrayList<>();
        int totalTokenUsed = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.debug("处理第 {}/{} 个片段", i + 1, chunks.size());

            // 为每个片段添加序号元数据
            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunk_index", i);
            chunkMetadata.put("total_chunks", chunks.size());

            GraphExtractionResult result = ingestTextWithModel(chunk, knowledgeId, chunkMetadata, modelName);

            if (result.getSuccess()) {
                allEntities.addAll(result.getEntities());
                allRelations.addAll(result.getRelations());
                if (result.getTokenUsed() != null) {
                    totalTokenUsed += result.getTokenUsed();
                }
            }
        }

        // 去重实体（基于名称和类型）
        List<ExtractedEntity> uniqueEntities = deduplicateEntities(allEntities);
        log.info("去重后实体数: {} -> {}", allEntities.size(), uniqueEntities.size());

        return GraphExtractionResult.builder()
                .entities(uniqueEntities)
                .relations(allRelations)
                .tokenUsed(totalTokenUsed)
                .success(true)
                .build();
    }

    @Override
    public String retrieveFromGraph(String query, String knowledgeId, int maxResults) {
        log.info("从图谱检索相关内容，查询: {}, 知识库ID: {}", query, knowledgeId);

        try {
            // 1. 从查询中抽取关键词（简单分词）
            List<String> keywords = extractKeywords(query);
            log.debug("提取的关键词: {}", keywords);

            if (keywords.isEmpty()) {
                return "未能从查询中提取关键信息";
            }

            // 2. 在图谱中搜索相关实体节点
            List<GraphVertex> matchedNodes = new ArrayList<>();
            for (String keyword : keywords) {
                List<GraphVertex> nodes = graphStoreService.searchVerticesByName(
                        keyword, knowledgeId, Math.min(5, maxResults)
                );
                matchedNodes.addAll(nodes);
            }

            if (matchedNodes.isEmpty()) {
                return "图谱中未找到相关实体";
            }

            log.info("找到 {} 个匹配的实体节点", matchedNodes.size());

            // 3. 去重（按nodeId）
            Map<String, GraphVertex> uniqueNodes = new HashMap<>();
            for (GraphVertex node : matchedNodes) {
                uniqueNodes.putIfAbsent(node.getNodeId(), node);
            }
            matchedNodes = new ArrayList<>(uniqueNodes.values());

            // 限制结果数量
            if (matchedNodes.size() > maxResults) {
                matchedNodes = matchedNodes.subList(0, maxResults);
            }

            // 4. 为每个匹配节点获取邻居，构建子图上下文
            StringBuilder result = new StringBuilder();
            result.append("### 图谱检索结果\n\n");
            result.append(String.format("查询: %s\n", query));
            result.append(String.format("找到 %d 个相关实体:\n\n", matchedNodes.size()));

            for (int i = 0; i < matchedNodes.size(); i++) {
                GraphVertex node = matchedNodes.get(i);
                result.append(String.format("**%d. %s** (%s)\n", i + 1, node.getName(), node.getLabel()));

                if (StrUtil.isNotBlank(node.getDescription())) {
                    result.append(String.format("   描述: %s\n", node.getDescription()));
                }

                // 获取邻居节点（1跳）
                List<GraphVertex> neighbors = graphStoreService.getNeighbors(
                        node.getNodeId(), knowledgeId, 5
                );

                if (!neighbors.isEmpty()) {
                    result.append("   关联实体: ");
                    List<String> neighborNames = neighbors.stream()
                            .map(GraphVertex::getName)
                            .limit(5)
                            .collect(java.util.stream.Collectors.toList());
                    result.append(String.join(", ", neighborNames));
                    result.append("\n");
                }

                result.append("\n");
            }

            // 5. 添加统计信息
            result.append("---\n");
            result.append(String.format("总计: %d 个实体节点\n", matchedNodes.size()));

            return result.toString();

        } catch (Exception e) {
            log.error("图谱检索失败", e);
            return "检索失败: " + e.getMessage();
        }
    }

    /**
     * 从查询中提取关键词
     *
     * @param query 查询文本
     * @return 关键词列表
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();

        // 简单的中文分词策略
        // 1. 去除标点符号
        String cleaned = query.replaceAll("[\\p{Punct}\\s]+", " ");

        // 2. 按空格分割
        String[] words = cleaned.split("\\s+");

        // 3. 过滤停用词和短词
        Set<String> stopWords = new HashSet<>(java.util.Arrays.asList(
                "的", "了", "和", "是", "在", "我", "有", "个", "这", "那", "为",
                "与", "或", "但", "等", "及", "而", "中", "如", "一", "二", "三"
        ));

        for (String word : words) {
            word = word.trim();
            if (word.length() >= 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        // 4. 如果没有提取到关键词，尝试按2-3字切分
        if (keywords.isEmpty() && query.length() >= 2) {
            for (int i = 0; i <= query.length() - 2; i++) {
                String chunk = query.substring(i, Math.min(i + 3, query.length()));
                if (chunk.length() >= 2 && !stopWords.contains(chunk)) {
                    keywords.add(chunk);
                }
            }
        }

        // 去重并限制数量
        return keywords.stream()
                .distinct()
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean deleteGraphData(String knowledgeId) {
        log.info("删除知识库图谱数据，知识库ID: {}", knowledgeId);

        try {
            // 删除该知识库的所有节点和关系
            return graphStoreService.deleteByKnowledgeId(knowledgeId);
        } catch (Exception e) {
            log.error("删除图谱数据失败", e);
            return false;
        }
    }

    /**
     * 将抽取的实体转换为图节点
     */
    private List<GraphVertex> convertEntitiesToVertices(
            List<ExtractedEntity> entities,
            String knowledgeId,
            Map<String, Object> metadata) {

        List<GraphVertex> vertices = new ArrayList<>();

        for (ExtractedEntity entity : entities) {
            GraphVertex vertex = new GraphVertex();
            vertex.setNodeId(IdUtil.simpleUUID()); // 生成唯一ID
            vertex.setName(entity.getName());
            vertex.setLabel(entity.getType());
            vertex.setDescription(entity.getDescription());
            vertex.setKnowledgeId(knowledgeId);
            vertex.setConfidence(entity.getConfidence() != null ? entity.getConfidence() : 1.0);

            // 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                vertex.setMetadata(metadata);
            }

            vertices.add(vertex);
        }

        return vertices;
    }

    /**
     * 将抽取的关系转换为图边
     *
     * @param relations             抽取的关系列表
     * @param knowledgeId           知识库ID
     * @param metadata              元数据
     * @param entityNameToNodeIdMap 实体名称到节点ID的映射
     * @return 图边列表
     */
    private List<GraphEdge> convertRelationsToEdges(
            List<ExtractedRelation> relations,
            String knowledgeId,
            Map<String, Object> metadata,
            Map<String, String> entityNameToNodeIdMap) {

        List<GraphEdge> edges = new ArrayList<>();
        int skippedCount = 0;

        for (ExtractedRelation relation : relations) {
            // ⭐ 通过实体名称查找对应的nodeId
            String sourceNodeId = entityNameToNodeIdMap.get(relation.getSourceEntity());
            String targetNodeId = entityNameToNodeIdMap.get(relation.getTargetEntity());

            // 如果找不到对应的节点ID，跳过这个关系
            if (sourceNodeId == null || targetNodeId == null) {
                log.warn("⚠️ 跳过关系（节点未找到）: {} -> {}",
                        relation.getSourceEntity(), relation.getTargetEntity());
                skippedCount++;
                continue;
            }

            GraphEdge edge = new GraphEdge();
            edge.setEdgeId(IdUtil.simpleUUID());
            edge.setSourceNodeId(sourceNodeId);      // ⭐ 设置源节点ID
            edge.setTargetNodeId(targetNodeId);      // ⭐ 设置目标节点ID
            edge.setSourceName(relation.getSourceEntity());
            edge.setTargetName(relation.getTargetEntity());
            edge.setLabel("RELATED_TO"); // 默认关系类型
            edge.setDescription(relation.getDescription());
            edge.setWeight(relation.getStrength() / 10.0); // 转换为0-1的权重
            edge.setKnowledgeId(knowledgeId);
            edge.setConfidence(relation.getConfidence() != null ? relation.getConfidence() : 1.0);

            // 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                edge.setMetadata(metadata);
            }

            edges.add(edge);
        }

        if (skippedCount > 0) {
            log.warn("⚠️ 共跳过 {} 个关系（对应的实体节点未找到）", skippedCount);
        }

        return edges;
    }

    /**
     * 分割文档为多个片段
     */
    private List<String> splitDocument(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = GraphConstants.RAG_MAX_SEGMENT_SIZE_IN_TOKENS * 4; // 简单估算字符数
        int overlap = GraphConstants.RAG_SEGMENT_OVERLAP_IN_TOKENS * 4;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尝试在句子边界分割
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int boundary = Math.max(lastPeriod, lastNewline);

                if (boundary > start) {
                    end = boundary + 1;
                }
            }

            chunks.add(text.substring(start, end));

            // ⭐ 修复死循环：确保 start 一定会增加
            // 如果已经到达文本末尾，直接退出
            if (end >= text.length()) {
                break;
            }

            // 计算下一个起始位置，确保至少前进1个字符
            int nextStart = end - overlap;
            if (nextStart <= start) {
                // 如果 overlap 太大导致无法前进，强制前进到 end
                start = end;
            } else {
                start = nextStart;
            }
        }

        return chunks;
    }

    /**
     * 去重实体
     */
    private List<ExtractedEntity> deduplicateEntities(List<ExtractedEntity> entities) {
        Map<String, ExtractedEntity> entityMap = new HashMap<>();

        for (ExtractedEntity entity : entities) {
            String key = entity.getName() + "|" + entity.getType();

            if (!entityMap.containsKey(key)) {
                entityMap.put(key, entity);
            } else {
                // 如果已存在，保留置信度更高的
                ExtractedEntity existing = entityMap.get(key);
                double entityConf = entity.getConfidence() != null ? entity.getConfidence() : 1.0;
                double existingConf = existing.getConfidence() != null ? existing.getConfidence() : 1.0;
                if (entityConf > existingConf) {
                    entityMap.put(key, entity);
                }
            }
        }

        return new ArrayList<>(entityMap.values());
    }
}
