package org.ruoyi.graph.service;

import org.ruoyi.graph.dto.GraphExtractionResult;

import java.util.Map;

/**
 * GraphRAG服务接口
 * 负责文档的图谱化处理和基于图谱的检索
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface IGraphRAGService {

    /**
     * 将文本入库到图谱
     *
     * @param text        文本内容
     * @param knowledgeId 知识库ID
     * @param metadata    元数据
     * @return 抽取结果
     */
    GraphExtractionResult ingestText(String text, String knowledgeId, Map<String, Object> metadata);

    /**
     * 将文本入库到图谱（指定模型）
     *
     * @param text        文本内容
     * @param knowledgeId 知识库ID
     * @param metadata    元数据
     * @param modelName   LLM模型名称
     * @return 抽取结果
     */
    GraphExtractionResult ingestTextWithModel(String text, String knowledgeId, Map<String, Object> metadata, String modelName);

    /**
     * 将文档入库到图谱（自动分片）
     *
     * @param documentText 文档内容
     * @param knowledgeId  知识库ID
     * @param metadata     元数据
     * @return 总抽取结果（合并所有分片）
     */
    GraphExtractionResult ingestDocument(String documentText, String knowledgeId, Map<String, Object> metadata);

    /**
     * 将文档入库到图谱（自动分片，指定模型）
     *
     * @param documentText 文档内容
     * @param knowledgeId  知识库ID
     * @param metadata     元数据
     * @param modelName    LLM模型名称
     * @return 总抽取结果（合并所有分片）
     */
    GraphExtractionResult ingestDocumentWithModel(String documentText, String knowledgeId, Map<String, Object> metadata, String modelName);

    /**
     * 基于图谱检索相关内容
     *
     * @param query       查询文本
     * @param knowledgeId 知识库ID
     * @param maxResults  最大结果数
     * @return 检索到的相关实体和关系
     */
    String retrieveFromGraph(String query, String knowledgeId, int maxResults);

    /**
     * 删除知识库的图谱数据
     *
     * @param knowledgeId 知识库ID
     * @return 是否成功
     */
    boolean deleteGraphData(String knowledgeId);
}
