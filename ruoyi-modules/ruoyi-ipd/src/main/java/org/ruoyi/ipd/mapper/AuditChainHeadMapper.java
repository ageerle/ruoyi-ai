package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AuditChainHead;

/**
 * audit_log_chain_heads mapper（分配器锚行；仅 AuditLogService.append 事务内使用，P 悲观锁变体）。
 * <p>注解 SQL + FOR UPDATE 风格对齐 ProjectStageMapper 先例；锁定读仅需 SELECT 权限
 * （chain_heads 表级已授 SELECT,UPDATE，Q6 REVOKE 后库级仅剩 SELECT,INSERT 不构成阻碍）。
 */
public interface AuditChainHeadMapper extends BaseMapperPlus<AuditChainHead, AuditChainHead> {

    /**
     * 悲观锁读锚行：事务内持有行锁至提交，全局串行化 append（零重试、零 CAS 竞态；锁序单一无死锁环）。
     */
    @Select("SELECT chain_key, last_seq, last_hash, next_seq, initialized_at"
          + " FROM audit_log_chain_heads WHERE chain_key = #{chainKey} FOR UPDATE")
    AuditChainHead selectForUpdate(@Param("chainKey") String chainKey);

    /**
     * 推进锚行（last_seq/last_hash/next_seq 原子前移；与 selectForUpdate 同事务，锁保护下必中）。
     *
     * @return 影响行数（锁保护下正常恒 1；0 = schema/chain_key 漂移，调用方须 fail-fast）
     */
    @Update("UPDATE audit_log_chain_heads SET last_seq = #{lastSeq}, last_hash = #{lastHash}, next_seq = #{nextSeq}"
          + " WHERE chain_key = #{chainKey}")
    int advance(@Param("chainKey") String chainKey, @Param("lastSeq") long lastSeq,
                @Param("lastHash") String lastHash, @Param("nextSeq") long nextSeq);
}
