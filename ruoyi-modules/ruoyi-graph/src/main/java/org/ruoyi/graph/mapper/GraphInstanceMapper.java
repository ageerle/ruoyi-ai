package org.ruoyi.graph.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.graph.domain.GraphInstance;

/**
 * 知识图谱实例Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface GraphInstanceMapper extends BaseMapper<GraphInstance> {

    /**
     * 根据图谱UUID查询
     *
     * @param graphUuid 图谱UUID
     * @return 图谱实例
     */
    GraphInstance selectByGraphUuid(String graphUuid);

    /**
     * 根据知识库ID查询图谱列表
     *
     * @param knowledgeId 知识库ID
     * @return 图谱实例列表
     */
    java.util.List<GraphInstance> selectByKnowledgeId(String knowledgeId);

    /**
     * 更新节点和关系数量
     *
     * @param graphUuid         图谱UUID
     * @param nodeCount         节点数量
     * @param relationshipCount 关系数量
     * @return 影响行数
     */
    int updateCounts(String graphUuid, Integer nodeCount, Integer relationshipCount);

    /**
     * 更新图谱状态
     *
     * @param graphUuid 图谱UUID
     * @param status    状态
     * @return 影响行数
     */
    int updateStatus(String graphUuid, Integer status);
}
