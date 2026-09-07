package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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
@RequiredArgsConstructor
public class AllowanceLedgerService {

    /** W4-D：允许 SELECT/COUNT 的 Mapper（之前仅 AllowanceService 注入，本卡补齐）。 */
    private final AllowanceLedgerMapper allowanceLedgerMapper;

    /** W4-D：period 入参 YYYY-MM 校验，避免 list/pendingStop/autoScan 三个端点接受任意字符串。 */
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private static void validateMonth(String period) {
        if (period == null || !MONTH_PATTERN.matcher(period).matches()) {
            throw new IpdBusinessException("period 必须为 YYYY-MM（当前=" + period + "）");
        }
    }

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

    // ============================================================
    // W4-D 件 2：list / pendingStop / autoScan 三方法（前端 allowance.ts 调用配套）
    // ============================================================

    /**
     * W5-E-2.3 件 1.1：actor 入口校验——service 层不信任 controller 必传（防御性兜底）。
     * actor == null 或 actor.id() == null → UNAUTHORIZED。
     */
    private static void requireAuthenticated(IpdActor actor) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未登录");
        }
    }

    /**
     * W4-D 件 2 §1：月度津贴快照列表（按 period 必填 + 可选 personId 过滤）。
     * 端点 GET /api/v1/allowance/ledger 配套服务方法。
     *
     * <p>W5-E-2.3 P0 #3 修复：签名加 {@code actor} 第一参数，service 层入口校验 actor 非空（UNAUTHORIZED），
     * 消除「无 actor 可披露全公司津贴明细」IDOR 缺口。读操作范围保持宽松：
     * MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 四角色由 Controller
     * {@code @SaCheckPermission(OPERATION_KPI_QUERY)} + {@code requireInternal()} 双重守门（件 1.5）。
     *
     * <p>排序：项目 ID 升序、人员 ID 升序；软删除（delFlag=0）由 MyBatis-Plus {@code @TableLogic} 自动过滤。
     * <p>DTO 字段对齐留作 W4-D' 单独任务；当前先以 AllowanceLedger 原样返回，前端 type 与后端 domain 字段名差异由前端适配层兜底。
     *
     * @param actor   当前会话身份（必填，由 Controller {@code permission.requireInternal()} 传入）
     * @param period  YYYY-MM（必填，违反格式抛 IpdBusinessException）
     * @param personId 可选；为 null 时返回该月全员记录
     * @return AllowanceLedger 列表（可能为空但不会为 null）
     */
    public List<AllowanceLedger> list(IpdActor actor, String period, Long personId) {
        requireAuthenticated(actor);
        validateMonth(period);
        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, period);
        if (personId != null) {
            q.eq(AllowanceLedger::getPersonId, personId);
        }
        q.orderByAsc(AllowanceLedger::getProjectId, AllowanceLedger::getPersonId);
        List<AllowanceLedger> rows = allowanceLedgerMapper.selectList(q);
        return rows == null ? Collections.emptyList() : rows;
    }

    /**
     * W4-D 件 2 §2：待停发津贴列表（按 period 过滤；stopReason IS NOT NULL）。
     * 端点 GET /api/v1/allowance/pending-stop 配套服务方法。
     *
     * <p>W5-E-2.3 P0 #3 修复：签名加 {@code actor} 第一参数，service 层入口校验 actor 非空（UNAUTHORIZED），
     * 消除「无 actor 可披露全公司停发明细」IDOR 缺口。读操作范围与 {@link #list} 同款
     * （四角色由 Controller 双重守门，件 1.5）。
     *
     * <p>判定：{@code stopReason} 非空即视为「待停发」（P3-3.2 触发：SCORE_BELOW_60 / NO_OUTPUT_60_DAYS）。
     * <p>不输出已经 freeze 的台账（{@code finalAmount=0} 且 {@code stopReason} 非空 ⇒ 已停发确认）。
     *
     * @param actor  当前会话身份（必填，由 Controller {@code permission.requireInternal()} 传入）
     * @param period YYYY-MM
     * @return 停发原因非空的 AllowanceLedger 列表
     */
    public List<AllowanceLedger> pendingStop(IpdActor actor, String period) {
        requireAuthenticated(actor);
        validateMonth(period);
        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, period)
            .isNotNull(AllowanceLedger::getStopReason)
            .orderByAsc(AllowanceLedger::getProjectId, AllowanceLedger::getPersonId);
        List<AllowanceLedger> rows = allowanceLedgerMapper.selectList(q);
        return rows == null ? Collections.emptyList() : rows;
    }

    /**
     * W4-D 件 2 §3：月度自动扫描（按 period；返回当月所有台账记录数）。
     * 端点 POST /api/v1/allowance/auto-scan 配套服务方法（仅超管）。
     *
     * <p>W5-E-2.3 P0 #3 修复：签名加 {@code actor} 第一参数 + service 层两重守卫——
     * actor 非空（UNAUTHORIZED）与仅 SUPER_ADMIN（FORBIDDEN）。原注释明言「仅超管」但守卫只在
     * Controller（W4-D {@code requireAdmin()}），service 层裸奔（W5-E P0 #3 原话「controller 决定」）；
     * 此处方法内兜底与 Controller 注解/requireAdmin 同严，防资金域操作失防（与 BonusPool
     * compute/freeze/distribute 方法内兜底同款治理，SEC-06）。
     *
     * <p>当前为「扫描 + 计数」简单委派实现，复杂扫描（绩效分 <60 / 60 天无产出判定）留待后续任务，调用方 AllowanceService.determineStopReasonP332 已就绪。
     *
     * @param actor  当前会话身份（必填，须 SUPER_ADMIN；由 Controller {@code permission.requireAdmin()} 传入）
     * @param period YYYY-MM
     * @return 当月 AllowanceLedger 行数（Int 范围；>2^31 抛 IpdBusinessException）
     */
    public Integer autoScan(IpdActor actor, String period) {
        requireAuthenticated(actor);
        if (!"SUPER_ADMIN".equals(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "仅超管可执行月度扫描");
        }
        validateMonth(period);
        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, period);
        long cnt = allowanceLedgerMapper.selectCount(q);
        if (cnt > Integer.MAX_VALUE) {
            throw new IpdBusinessException("扫描结果超过 Integer.MAX_VALUE，请分页处理（cnt=" + cnt + "）");
        }
        return (int) cnt;
    }
}
