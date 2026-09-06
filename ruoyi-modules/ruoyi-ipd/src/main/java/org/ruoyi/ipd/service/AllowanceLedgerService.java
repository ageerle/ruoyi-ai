package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * P3-3.1 月度津贴台账基础额、锁级与 2 倍封顶服务
 *
 * <p>AC：BR-INC-02/03；卡 P3-3 描述。
 * <p>核心规则：
 * <ul>
 *   <li>锁定评级：成员绑定时快照 lockedLevel（L1–L5）</li>
 *   <li>多项目叠加：单项目 baseAmount 累加，finalAmount ≤ baseAmount × capMultiplier（默认 2 倍）</li>
 *   <li>&lt;60 停发 / 无产出 60 天停发：见 P3-3.2（独立卡）</li>
 *   <li>L1–L5 基础额 = allowance.{level}，由 API 配置（与项目绩效得分不互相推导）</li>
 * </ul>
 *
 * <p>P3-3.1 单卡实现：锁定评级校验 + 多项目叠加 + 2 倍封顶 + 草稿。停发逻辑由 P3-3.2 完成。
 */
@Slf4j
@Service
public class AllowanceLedgerService {

    /** 合法锁定评级 */
    public static final List<String> LOCKED_LEVELS = Arrays.asList("L1", "L2", "L3", "L4", "L5");

    /** 默认封顶倍数（多项目叠加 ≤ 2 倍） */
    public static final BigDecimal DEFAULT_CAP_MULTIPLIER = new BigDecimal("2.0");

    /**
     * 校验锁定评级。
     */
    public static void validateLockedLevel(String lockedLevel) {
        if (lockedLevel == null || !LOCKED_LEVELS.contains(lockedLevel)) {
            throw new IpdBusinessException("锁定评级必须为 L1..L5（当前=" + lockedLevel + "）");
        }
    }

    /**
     * 校验基础额：必须 ≥ 0。
     */
    public static void validateBaseAmount(BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IpdBusinessException("津贴基础额必须 ≥ 0");
        }
    }

    /**
     * 校验封顶倍数：必须 ≥ 1（默认 2.0）。
     */
    public static void validateCapMultiplier(BigDecimal capMultiplier) {
        if (capMultiplier == null) {
            return;
        }
        if (capMultiplier.compareTo(BigDecimal.ONE) < 0) {
            throw new IpdBusinessException("封顶倍数必须 ≥ 1.0");
        }
    }

    /**
     * P3-3.1 多项目叠加 2 倍封顶计算。
     * <pre>
     *   finalAmount = min(Σ baseAmount[i], baseAmount × capMultiplier)
     *   capApplied  = "1" if 触发封顶 else "0"
     * </pre>
     * 其中 baseAmount = 单项目基础额（锁定评级对应），capMultiplier 默认 2.0。
     *
     * @param baseAmountList 单项目基础额列表（同一人员的不同项目）
     * @param capMultiplier  封顶倍数（可为 null，默认 2.0）
     * @return AllowanceLedger 草稿（finalAmount + capApplied 已填充）
     */
    public AllowanceLedger calcFinalAmount(AllowanceLedger draft,
                                           List<BigDecimal> baseAmountList,
                                           BigDecimal capMultiplier) {
        if (draft == null) {
            throw new IpdBusinessException("津贴草稿不能为空");
        }
        validateLockedLevel(draft.getLockedLevel());
        validateCapMultiplier(capMultiplier);

        BigDecimal cap = capMultiplier == null ? DEFAULT_CAP_MULTIPLIER : capMultiplier;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal b : baseAmountList) {
            validateBaseAmount(b);
            sum = sum.add(b);
        }
        BigDecimal capLine = baseAmountList.isEmpty() ? BigDecimal.ZERO
            : baseAmountList.get(0).multiply(cap);
        BigDecimal finalAmount = sum.min(capLine).setScale(2, RoundingMode.HALF_UP);

        // 恰好等于 capLine 不触发封顶（仅超额触发）
        String capApplied = sum.compareTo(capLine) > 0 ? "1" : "0";

        draft.setBaseAmount(baseAmountList.isEmpty() ? BigDecimal.ZERO : baseAmountList.get(0));
        draft.setFinalAmount(finalAmount);
        draft.setCapApplied(capApplied);
        log.debug("津贴封顶计算 personId={} sum={} capLine={} final={} capApplied={}",
            draft.getPersonId(), sum, capLine, finalAmount, capApplied);
        return draft;
    }

    /**
     * 草稿录入：绑定时锁定评级。
     */
    @Transactional(rollbackFor = Exception.class)
    public AllowanceLedger draftBinding(AllowanceLedger draft) {
        validateLockedLevel(draft.getLockedLevel());
        validateBaseAmount(draft.getBaseAmount());
        draft.setCapApplied("0");
        draft.setFinalAmount(draft.getBaseAmount());
        return draft;
    }
}
