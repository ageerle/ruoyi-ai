package org.ruoyi.service.retrieval;

import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;

import java.util.List;

/**
 * 知识库检索服务接口
 * 整合粗召回（向量检索/关键词检索）和重排序流程
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

    /**
     * 执行知识库检索，返回详细结果对象（包含分数、文档ID等）
     * 支持混合检索和重排序
     *
     * @param queryVectorBo 查询参数
     * @return 检索结果列表
     */
    List<KnowledgeRetrievalVo> retrieve(QueryVectorBo queryVectorBo);
}
