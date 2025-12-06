package org.ruoyi.graph.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.graph.domain.GraphInstance;
import org.ruoyi.graph.mapper.GraphInstanceMapper;
import org.ruoyi.graph.service.IGraphInstanceService;
import org.ruoyi.graph.service.IGraphStoreService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱实例服务实现
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphInstanceServiceImpl implements IGraphInstanceService {

    private final GraphInstanceMapper graphInstanceMapper;
    private final IGraphStoreService graphStoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInstance createInstance(String knowledgeId, String graphName, String config) {
        // 检查是否已存在
        LambdaQueryWrapper<GraphInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphInstance::getKnowledgeId, knowledgeId);
        GraphInstance existing = graphInstanceMapper.selectOne(wrapper);

        if (existing != null) {
            log.warn("知识库 {} 已存在图谱实例", knowledgeId);
            return existing;
        }

        // 创建新实例
        GraphInstance instance = new GraphInstance();
        instance.setGraphUuid(String.valueOf(IdUtil.getSnowflake().nextId())); // UUID
        instance.setKnowledgeId(knowledgeId);
        instance.setGraphName(StringUtils.isNotBlank(graphName) ? graphName : "知识图谱-" + knowledgeId);
        instance.setGraphStatus(0); // 0-未构建（新建时状态为未构建，需手动点击"构建"按钮）
        instance.setNodeCount(0);
        instance.setRelationshipCount(0);

        // 解析配置
        if (StringUtils.isNotBlank(config)) {
            instance.setConfig(config);
        }

        graphInstanceMapper.insert(instance);

        // 创建 Neo4j Schema
        graphStoreService.createGraphSchema(knowledgeId);

        log.info("创建图谱实例成功: knowledgeId={}, instanceId={}", knowledgeId, instance.getId());
        return instance;
    }

    @Override
    public GraphInstance getById(Long id) {
        return graphInstanceMapper.selectById(id);
    }

    @Override
    public GraphInstance getByUuid(String graphUuid) {
        LambdaQueryWrapper<GraphInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphInstance::getGraphUuid, graphUuid);
        return graphInstanceMapper.selectOne(wrapper);
    }

    @Override
    public boolean updateInstance(GraphInstance instance) {
        try {
            int rows = graphInstanceMapper.updateById(instance);
            log.info("更新图谱实例: id={}, rows={}", instance.getId(), rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新图谱实例失败: id={}", instance.getId(), e);
            return false;
        }
    }

    @Override
    public List<GraphInstance> listByKnowledgeId(String knowledgeId) {
        LambdaQueryWrapper<GraphInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphInstance::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(GraphInstance::getCreateTime);
        return graphInstanceMapper.selectList(wrapper);
    }

    @Override
    public Page<GraphInstance> queryPage(Page<GraphInstance> page, String instanceName, String knowledgeId, Integer graphStatus) {
        LambdaQueryWrapper<GraphInstance> wrapper = new LambdaQueryWrapper<>();

        // 图谱名称模糊查询
        if (StringUtils.isNotBlank(instanceName)) {
            wrapper.like(GraphInstance::getGraphName, instanceName.trim());
        }

        // 知识库ID精确查询
        if (StringUtils.isNotBlank(knowledgeId)) {
            wrapper.eq(GraphInstance::getKnowledgeId, knowledgeId.trim());
        }

        // 状态精确查询
        if (graphStatus != null) {
            wrapper.eq(GraphInstance::getGraphStatus, graphStatus);
        }

        // 只查询未删除的记录
        wrapper.eq(GraphInstance::getDelFlag, "0");

        // 按创建时间倒序
        wrapper.orderByDesc(GraphInstance::getCreateTime);

        return graphInstanceMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean updateStatus(String graphUuid, Integer status) {
        try {
            LambdaUpdateWrapper<GraphInstance> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphInstance::getGraphUuid, graphUuid);
            wrapper.set(GraphInstance::getGraphStatus, status);

            int rows = graphInstanceMapper.update(null, wrapper);

            log.info("更新图谱状态: graphUuid={}, status={}, rows={}", graphUuid, status, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新图谱状态失败: graphUuid={}, status={}", graphUuid, status, e);
            return false;
        }
    }

    @Override
    public boolean updateCounts(String graphUuid, Integer nodeCount, Integer relationshipCount) {
        try {
            LambdaUpdateWrapper<GraphInstance> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphInstance::getGraphUuid, graphUuid);

            if (nodeCount != null) {
                wrapper.set(GraphInstance::getNodeCount, nodeCount);
            }
            if (relationshipCount != null) {
                wrapper.set(GraphInstance::getRelationshipCount, relationshipCount);
            }

            int rows = graphInstanceMapper.update(null, wrapper);

            log.info("更新图谱统计: graphUuid={}, nodeCount={}, relationshipCount={}, rows={}",
                    graphUuid, nodeCount, relationshipCount, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新图谱统计失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    public boolean updateConfig(String graphUuid, String config) {
        try {
            LambdaUpdateWrapper<GraphInstance> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GraphInstance::getGraphUuid, graphUuid);
            wrapper.set(GraphInstance::getConfig, config);

            int rows = graphInstanceMapper.update(null, wrapper);

            log.info("更新图谱配置: graphUuid={}, rows={}", graphUuid, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("更新图谱配置失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInstance(String graphUuid) {
        try {
            log.info("🗑️ 开始删除图谱实例及数据，graphUuid: {}", graphUuid);

            // ⭐ 1. 先获取实例信息（获取knowledgeId）
            GraphInstance instance = getByUuid(graphUuid);
            if (instance == null) {
                log.warn("⚠️ 图谱实例不存在: graphUuid={}", graphUuid);
                return false;
            }

            String knowledgeId = instance.getKnowledgeId();

            // ⭐ 2. 删除Neo4j中的图数据（通过knowledgeId）
            if (StrUtil.isNotBlank(knowledgeId)) {
                log.info("删除Neo4j图数据，knowledgeId: {}", knowledgeId);
                boolean neo4jDeleted = graphStoreService.deleteByKnowledgeId(knowledgeId);
                if (neo4jDeleted) {
                    log.info("✅ Neo4j图数据删除成功");
                } else {
                    log.warn("⚠️ Neo4j图数据删除失败（可能是没有数据）");
                }
            } else {
                log.warn("⚠️ 实例没有关联知识库ID，跳过Neo4j数据删除");
            }

            // 3. 删除MySQL中的实例记录
            LambdaQueryWrapper<GraphInstance> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GraphInstance::getGraphUuid, graphUuid);
            int rows = graphInstanceMapper.delete(wrapper);

            log.info("✅ 删除图谱实例成功: graphUuid={}, knowledgeId={}, rows={}",
                    graphUuid, knowledgeId, rows);
            return rows > 0;
        } catch (Exception e) {
            log.error("❌ 删除图谱实例失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInstanceAndData(String graphUuid) {
        try {
            // 1. 删除 Neo4j 中的图谱数据
            boolean graphDeleted = graphStoreService.deleteGraph(graphUuid);

            // 2. 删除 MySQL 中的实例记录
            boolean instanceDeleted = deleteInstance(graphUuid);

            log.info("删除图谱实例及数据: graphUuid={}, graphDeleted={}, instanceDeleted={}",
                    graphUuid, graphDeleted, instanceDeleted);

            return graphDeleted && instanceDeleted;
        } catch (Exception e) {
            log.error("删除图谱实例及数据失败: graphUuid={}", graphUuid, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getStatistics(String graphUuid) {
        try {
            // 从 Neo4j 获取实时统计
            Map<String, Object> stats = graphStoreService.getGraphStatistics(graphUuid);

            // 更新到 MySQL（异步）
            if (stats.containsKey("nodeCount") && stats.containsKey("relationshipCount")) {
                updateCounts(
                        graphUuid,
                        (Integer) stats.get("nodeCount"),
                        (Integer) stats.get("relationshipCount")
                );
            }

            // 添加实例信息
            GraphInstance instance = getByUuid(graphUuid);
            if (instance != null) {
                stats.put("graphName", instance.getGraphName());
                stats.put("status", instance.getGraphStatus());
                stats.put("createTime", instance.getCreateTime());
                stats.put("updateTime", instance.getUpdateTime());
            }

            return stats;
        } catch (Exception e) {
            log.error("获取图谱统计信息失败: graphUuid={}", graphUuid, e);
            return new HashMap<>();
        }
    }
}
