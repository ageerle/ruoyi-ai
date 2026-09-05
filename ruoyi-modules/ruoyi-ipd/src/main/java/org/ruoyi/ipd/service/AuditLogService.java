package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审计日志服务（只追加 + hash 链，v3 TS-08；AC-AUD-01 篡改可检）
 * ⚠️ 只追加：不提供任何 update/delete 方法（G-02 配套证据链）。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /** 追加一条审计（独立事务：业务失败不回滚审计） */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public AuditLog append(AuditLog draft) {
        AuditLog last = selectLast();
        String prevHash = last != null ? last.getCurrHash() : AuditHashChain.GENESIS;
        long seq = (last != null && last.getSeq() != null ? last.getSeq() : 0L) + 1;
        String canonical = AuditHashChain.canonical(seq, draft.getOperatorId(), draft.getOperatorName(),
            draft.getOperatorRole(), draft.getAction(), draft.getEntityType(), draft.getEntityId(),
            draft.getBeforeData(), draft.getAfterData(), draft.getReason(),
            draft.getCreateTime() != null ? draft.getCreateTime().getTime() : System.currentTimeMillis());
        draft.setSeq(seq);
        draft.setPrevHash(prevHash);
        draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonical));
        if (draft.getCreateTime() == null) {
            draft.setCreateTime(new Date());
        }
        if (draft.getTenantId() == null) {
            draft.setTenantId("000000");
        }
        auditLogMapper.insert(draft);
        return draft;
    }

    /** 全链校验：返回断裂/篡改的 seq 列表（空 = 链完整） */
    public List<Long> verifyChain() {
        List<Long> broken = new ArrayList<>();
        String expectPrev = AuditHashChain.GENESIS;
        Long expectSeq = 1L;
        for (AuditLog log : auditLogMapper.selectList(orderBySeq())) {
            String canonical = AuditHashChain.canonical(log.getSeq(), log.getOperatorId(), log.getOperatorName(),
                log.getOperatorRole(), log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getBeforeData(), log.getAfterData(), log.getReason(),
                log.getCreateTime() != null ? log.getCreateTime().getTime() : 0L);
            String expectHash = AuditHashChain.computeCurrHash(expectPrev, canonical);
            if (!expectHash.equals(log.getCurrHash())
                || !expectPrev.equals(nvl(log.getPrevHash()))
                || !expectSeq.equals(log.getSeq())) {
                broken.add(log.getSeq());
            }
            expectPrev = log.getCurrHash();
            expectSeq = log.getSeq() + 1;
        }
        return broken;
    }

    private AuditLog selectLast() {
        List<AuditLog> list = auditLogMapper.selectList(orderBySeq().last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private LambdaQueryWrapper<AuditLog> orderBySeq() {
        return new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getSeq);
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}