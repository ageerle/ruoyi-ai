package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AuditLog;

/** AuditLog mapper */
public interface AuditLogMapper extends BaseMapperPlus<AuditLog, AuditLog> {

    /**
     * DEF-4 链重建专用：仅更新 prev_hash/curr_hash 两列（业务字段只读）。
     * <p>唯一合法调用方：{@code AuditLogService.rebuildChain()}（超管修复工具，动作本身落审计）；
     * 业务代码禁止调用（G-02 只追加契约）。
     *
     * @param id       行主键
     * @param prevHash 重链后的前行哈希
     * @param currHash 重算后的本行哈希
     * @return 影响行数
     */
    @Update("UPDATE audit_logs SET prev_hash = #{prevHash}, curr_hash = #{currHash} WHERE id = #{id}")
    int updateChainHash(@Param("id") Long id, @Param("prevHash") String prevHash, @Param("currHash") String currHash);
}