package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.BidInvitation;

/**
 * 招标单 Mapper（P2-3.1 BR-TEAM）
 */
public interface BidInvitationMapper extends BaseMapperPlus<BidInvitation, BidInvitation> {

    /**
     * P2-3.2：招标单行锁定读（当前读）。同一招标单上的应标提交/遴选/撤回串行化，
     * 关闭「双中标竞态（H-1）」与「同 rd_pm 并发重复应标落两行（M-1）」；
     * 后续普通一致性读在获锁之后建立快照，能看到前序事务已提交的应标行。
     */
    @Select("SELECT * FROM bid_invitations WHERE id=#{id} AND tenant_id='000000' AND del_flag='0' FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    BidInvitation selectByIdForUpdate(@Param("id") Long id);
}
