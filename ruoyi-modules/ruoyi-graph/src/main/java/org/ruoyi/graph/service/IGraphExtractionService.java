package org.ruoyi.graph.service;

import org.ruoyi.graph.dto.GraphExtractionResult;

/**
 * 图谱实体关系抽取服务接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface IGraphExtractionService {

    /**
     * 从文本中抽取实体和关系
     *
     * @param text 输入文本
     * @return 抽取结果
     */
    GraphExtractionResult extractFromText(String text);

    /**
     * 从文本中抽取实体和关系（自定义实体类型）
     *
     * @param text        输入文本
     * @param entityTypes 实体类型列表
     * @return 抽取结果
     */
    GraphExtractionResult extractFromText(String text, String[] entityTypes);

    /**
     * 从文本中抽取实体和关系（使用指定的LLM模型）
     *
     * @param text      输入文本
     * @param modelName LLM模型名称
     * @return 抽取结果
     */
    GraphExtractionResult extractFromTextWithModel(String text, String modelName);

    /**
     * 解析LLM响应为实体和关系
     *
     * @param response LLM响应文本
     * @return 抽取结果
     */
    GraphExtractionResult parseGraphResponse(String response);
}
