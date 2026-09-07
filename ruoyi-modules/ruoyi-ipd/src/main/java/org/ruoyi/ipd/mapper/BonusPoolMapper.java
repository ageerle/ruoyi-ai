package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.BonusPool;

/**
 * 奖金池 Mapper（P3-4.2/4.3 AC-INC-16/17/18/19/20/21）
 */
public interface BonusPoolMapper extends BaseMapperPlus<BonusPool, BonusPool> {

    /**
     * P3-4.4 §2.1 + W4-B BonusPoolService.compute 防 DuplicateKey 改 409：
     * 按 projectId + 状态精确查一条奖金池（避开 SELECT * 与软删过滤在 Java 层错位）。
     * 命中即视为"已存在对应状态的奖金池"，由 Service 层抛 STATE_CONFLICT（HTTP 409）
     * 而非落入 UNIQUE KEY `uk_bp_project` 兜底 500。
     *
     * <p>注解 SQL 风格对齐 AuditChainHeadMapper / ProjectStageMapper（不依赖 XML 文件）。
     * tenant_id 与 del_flag 显式列出——多租户拦截器虽会自动注入租户过滤，但此处属于
     * 唯一性判定的硬约束读，必须在 SQL 自身可证伪（防御性双保险）。
     *
     * @param projectId 项目 ID
     * @param status    状态（DRAFT / CONFIRMED / DISTRIBUTED）
     * @return 命中实体；无则 null
     */
    @Select("SELECT * FROM bonus_pools"
          + " WHERE project_id = #{projectId} AND status = #{status}"
          + " AND tenant_id = '000000' AND del_flag = '0'"
          + " ORDER BY id DESC LIMIT 1")
    BonusPool selectByProjectIdAndStatus(@Param("projectId") Long projectId,
                                         @Param("status") String status);
}
