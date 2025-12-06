package org.ruoyi.graph.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.ruoyi.graph.domain.GraphInstance;

import java.util.List;

/**
 * 图谱实例服务接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface IGraphInstanceService {

    /**
     * 创建图谱实例
     *
     * @param knowledgeId 知识库ID
     * @param graphName   图谱名称
     * @param config      配置信息
     * @return 图谱实例
     */
    GraphInstance createInstance(String knowledgeId, String graphName, String config);

    /**
     * 根据主键ID获取图谱实例
     *
     * @param id 主键ID
     * @return 图谱实例
     */
    GraphInstance getById(Long id);

    /**
     * 根据UUID获取图谱实例
     *
     * @param graphUuid 图谱UUID
     * @return 图谱实例
     */
    GraphInstance getByUuid(String graphUuid);

    /**
     * 更新图谱实例
     *
     * @param instance 图谱实例
     * @return 是否成功
     */
    boolean updateInstance(GraphInstance instance);

    /**
     * 根据知识库ID获取图谱列表
     *
     * @param knowledgeId 知识库ID
     * @return 图谱实例列表
     */
    List<GraphInstance> listByKnowledgeId(String knowledgeId);

    /**
     * 条件查询图谱实例列表（分页）
     *
     * @param page         分页对象
     * @param instanceName 图谱名称（模糊查询）
     * @param knowledgeId  知识库ID
     * @param graphStatus  图谱状态码
     * @return 分页结果
     */
    Page<GraphInstance> queryPage(Page<GraphInstance> page, String instanceName, String knowledgeId, Integer graphStatus);

    /**
     * 更新图谱状态
     *
     * @param graphUuid 图谱UUID
     * @param status    状态
     * @return 是否成功
     */
    boolean updateStatus(String graphUuid, Integer status);

    /**
     * 更新图谱统计信息
     *
     * @param graphUuid         图谱UUID
     * @param nodeCount         节点数量
     * @param relationshipCount 关系数量
     * @return 是否成功
     */
    boolean updateCounts(String graphUuid, Integer nodeCount, Integer relationshipCount);

    /**
     * 更新图谱配置
     *
     * @param graphUuid 图谱UUID
     * @param config    配置信息
     * @return 是否成功
     */
    boolean updateConfig(String graphUuid, String config);

    /**
     * 删除图谱实例（软删除）
     *
     * @param graphUuid 图谱UUID
     * @return 是否成功
     */
    boolean deleteInstance(String graphUuid);

    /**
     * 物理删除图谱实例及其数据
     *
     * @param graphUuid 图谱UUID
     * @return 是否成功
     */
    boolean deleteInstanceAndData(String graphUuid);

    /**
     * 获取图谱统计信息
     *
     * @param graphUuid 图谱UUID
     * @return 统计信息
     */
    java.util.Map<String, Object> getStatistics(String graphUuid);
}
