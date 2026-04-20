package org.ruoyi.service.retrieval;

import org.ruoyi.domain.bo.vector.QueryVectorBo;

import java.util.List;

/**
 * 知识库检索服务接口
 * 整合粗召回（向量检索）和重排序流程
 *
 * @author yang
 * @date 2026-04-19
 */
public interface KnowledgeRetrievalService {

    /**
     * 执行知识库检索，返回文本内容
     * 流程：向量粗召回 -> 重排序（可选） -> 返回结果
     *
     * @param queryVectorBo 查询参数
     * @return 文本内容列表
     */
    List<String> retrieveTexts(QueryVectorBo queryVectorBo);
}
