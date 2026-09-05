package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Gate 评审要素管理（P1-6 补口）：种子 33 项已落库，超管可增删改+启停；
 * 要素编码全局唯一；否决位标记驱动 Gate 双签判定（P2 消费）。
 */
@Service
@RequiredArgsConstructor
public class GateElementService {

    private static final Set<String> GATES = Set.of("G1", "G2", "G3", "G4", "G5");

    private final GateElementMapper gateElementMapper;
    private final AuditLogService auditLogService;

    public List<GateElement> listByGate(String gateCode) {
        return gateElementMapper.selectList(new LambdaQueryWrapper<GateElement>()
            .eq(gateCode != null && !gateCode.isBlank(), GateElement::getGateCode, gateCode)
            .eq(GateElement::getEnabled, "1")
            .orderByAsc(GateElement::getSortOrder));
    }

    public GateElement create(GateElement e, String operator) {
        validate(e);
        Long dup = gateElementMapper.selectCount(new LambdaQueryWrapper<GateElement>()
            .eq(GateElement::getElementCode, e.getElementCode()));
        if (dup != null && dup > 0) {
            throw new ServiceException("要素编码已存在: " + e.getElementCode());
        }
        if (e.getIsVeto() == null) {
            e.setIsVeto("0");
        }
        if (e.getEnabled() == null) {
            e.setEnabled("1");
        }
        if (e.getSortOrder() == null) {
            e.setSortOrder(0);
        }
        gateElementMapper.insert(e);
        audit(operator, "CREATE", e.getId(), e.getGateCode() + "/" + e.getElementCode());
        return e;
    }

    public GateElement update(GateElement patch, String operator) {
        GateElement exist = gateElementMapper.selectById(patch.getId());
        if (exist == null) {
            throw new ServiceException("要素不存在: " + patch.getId());
        }
        if (patch.getElementName() != null) {
            exist.setElementName(patch.getElementName());
        }
        if (patch.getPassStandard() != null) {
            exist.setPassStandard(patch.getPassStandard());
        }
        if (patch.getIsVeto() != null) {
            exist.setIsVeto(patch.getIsVeto());
        }
        if (patch.getSortOrder() != null) {
            exist.setSortOrder(patch.getSortOrder());
        }
        if (patch.getEnabled() != null) {
            exist.setEnabled(patch.getEnabled());
        }
        gateElementMapper.updateById(exist);
        audit(operator, "UPDATE", exist.getId(), exist.getElementCode());
        return exist;
    }

    /** 停用（禁删：在途 gate_element_results 引用，G-02 证据链） */
    public GateElement disable(Long id, String operator) {
        GateElement exist = gateElementMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("要素不存在: " + id);
        }
        exist.setEnabled("0");
        gateElementMapper.updateById(exist);
        audit(operator, "UPDATE", id, exist.getElementCode() + " disabled");
        return exist;
    }

    private void validate(GateElement e) {
        if (e.getGateCode() == null || !GATES.contains(e.getGateCode())) {
            throw new ServiceException("gateCode 必须为 G1..G5: " + e.getGateCode());
        }
        if (e.getElementCode() == null || e.getElementCode().isBlank()) {
            throw new ServiceException("要素编码必填");
        }
        if (e.getElementName() == null || e.getElementName().isBlank()) {
            throw new ServiceException("要素名必填");
        }
    }

    /**
     * DEF-1（QA-03 矩阵实测）：after_data 为 MySQL JSON 列，纯文本直写触发
     * MysqlDataTruncation → 审计与业务同事务回滚，create 从未成功落库。
     * 统一走 {@link AuditEventData#json} 与全模块审计 JSON 契约对齐。
     */
    private void audit(String operator, String action, Long id, String detail) {
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("SUPER_ADMIN")
            .action(action).entityType("GATE_ELEMENT").entityId(id)
            .afterData(AuditEventData.json("detail", detail))
            .build());
    }
}