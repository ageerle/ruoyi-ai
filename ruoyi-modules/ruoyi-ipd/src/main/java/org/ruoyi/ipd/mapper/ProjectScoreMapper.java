package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.ProjectScore;

import java.util.List;

/**
 * 项目绩效评分 Mapper（P3-2.1/2.2/2.3）
 *
 * <p>P3-4.5 扩展：取数策略 PROJECT_SCORE / LAST_QUARTER 需按项目查档；ProjectScore 实体
 * 不含 period 字段（当前架构按维度/角色归档），按 projectId 倒序取最近一条即可。
 */
public interface ProjectScoreMapper extends BaseMapperPlus<ProjectScore, ProjectScore> {

    /**
     * P3-4.5 BR-INC-07：按 projectId 查最近一条项目绩效评分（用于 PROJECT_SCORE / LAST_QUARTER 策略路由）。
     *
     * <p>实现：selectList 按 createTime 倒序取 1 条。当前 P3-2.x 阶段 ProjectScore 实体不含
     * period 字段；多期数据按 scoredAt 自然时序区分。WEIGHTED_AVG 策略由调用方在 Service 层做加权。
     *
     * @param projectId 项目 ID
     * @return 最近一条项目绩效评分（无记录返回空集合）
     */
    default List<ProjectScore> selectLatestByProject(Long projectId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectScore>()
            .eq(ProjectScore::getProjectId, projectId)
            .orderByDesc(ProjectScore::getCreateTime)
            .last("LIMIT 1"));
    }
}
