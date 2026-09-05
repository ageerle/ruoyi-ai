package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;

import java.util.List;

/** P1-3.1：初始化在同一项目锁下使用当前读，避免已有 RR 快照遗漏其他事务已完成的结构。 */
@Mapper
public interface ProjectStageMapper extends BaseMapper<ProjectStage> {
    @Select("SELECT * FROM projects WHERE id=#{projectId} AND tenant_id='000000' AND del_flag='0' FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Project selectProjectForBootstrap(@Param("projectId") Long projectId);

    @Select("SELECT * FROM project_stages WHERE project_id=#{projectId} AND tenant_id='000000' AND del_flag='0' ORDER BY sort_order,id FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<ProjectStage> selectLiveByProject(@Param("projectId") Long projectId);

    // 不关联父阶段过滤：孤立、错阶段动作也必须被完整性检查看到。
    @Select("SELECT * FROM stage_actions WHERE project_id=#{projectId} AND tenant_id='000000' AND del_flag='0' ORDER BY id FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<StageAction> selectActionsForBootstrap(@Param("projectId") Long projectId);

    // 原始ID仅用于发现被租户/软删过滤隐藏的已有记录，不能将坏结构误认为空结构。
    @Select("SELECT id FROM project_stages WHERE project_id=#{projectId} ORDER BY id FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<Long> selectAllStageIdsForBootstrap(@Param("projectId") Long projectId);

    @Select("SELECT id FROM stage_actions WHERE project_id=#{projectId} ORDER BY id FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<Long> selectAllActionIdsForBootstrap(@Param("projectId") Long projectId);
}
