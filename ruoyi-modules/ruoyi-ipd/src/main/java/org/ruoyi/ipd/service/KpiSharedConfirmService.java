package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.KpiSharedConfirm;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.KpiSharedConfirmMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.vo.KpiSharedConfirmView;
import org.ruoyi.ipd.vo.SharedKpiCollectView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * P3-1.2-BACKEND 共担 KPI 双组长确认（ZK 原型页 20「责任PM填报 · 功能KPI直属组长复核 ·
 * 共担KPI双组长确认」；看板卡 56d97bb0）。
 *
 * <p>确认链：collect 归集成功 → {@link #ensurePendingRows} 按 K01-K04 各生成一条
 * PENDING 确认行 → 两位 GROUP_LEADER 依次签署（first / second），同人不可重复签署
 * （{@link ApiV1ErrorCode#DUAL_SIGN_INCOMPLETE 40002}），第二签落库后 CONFIRMED。
 *
 * <p>OVERDUE 为读时派生态（PENDING 且已过 deadlineAt），不落库、无扫描任务。
 * 复用 {@link KpiSharedCollectionService#resolveMonthlyDeadline}（次月第 N 个工作日
 * 18:00，kpi.monthlyDeadlineDay）与项目级鉴权口径（与 listSharedKpis 同严）。
 */
@Slf4j
@Service
public class KpiSharedConfirmService {

    /** 待确认（存储态） */
    public static final String ST_PENDING = "PENDING";
    /** 已双组长确认（终态） */
    public static final String ST_CONFIRMED = "CONFIRMED";
    /** 已逾期（读时派生态：PENDING 且已过 deadlineAt，不落库） */
    public static final String ST_OVERDUE = "OVERDUE";

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Set<String> CONFIRM_ROLES = Set.of("GROUP_LEADER", "SUPER_ADMIN");
    private static final int DEFAULT_DEADLINE_DAY = 5;

    /** K01-K04 指标名（与 KpiSharedCollectionService.MetricResult message 口径一致） */
    private static final Map<String, String> METRIC_NAMES = Map.of(
        "K01", "销量/出货量达成率",
        "K02", "渠道商覆盖达成率",
        "K03", "NPS 达成率",
        "K04", "场景覆盖率");

    private final KpiSharedConfirmMapper confirmMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final PersonMapper personMapper;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;

    public KpiSharedConfirmService(KpiSharedConfirmMapper confirmMapper,
                                   ProjectMapper projectMapper,
                                   ProjectMemberMapper projectMemberMapper,
                                   PersonMapper personMapper,
                                   AuditLogService auditLogService,
                                   SystemConfigService systemConfigService) {
        this.confirmMapper = confirmMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.personMapper = personMapper;
        this.auditLogService = auditLogService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 归集成功后生成 / 复位 K01-K04 确认行（PENDING）。
     *
     * <p>由 SharedKpiController.collect 在归集事务提交后调用：
     * <ul>
     *   <li>无确认行 → insert PENDING（weight 取归集指标权重，deadline = 次月第 N 工作日 18:00）</li>
     *   <li>已有 CONFIRMED 行（新 revision 重归集，数据已变）→ 双签清零、拉回 PENDING 重新确认</li>
     *   <li>已有 PENDING 行 → 仅刷新 deadline</li>
     * </ul>
     * 任何参数异常静默返回（不阻断归集主流程；归集自身已校验过参数）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensurePendingRows(Long projectId, String period, Long collectedBy,
                                  List<SharedKpiCollectView.Metric> metrics) {
        if (confirmMapper == null || projectId == null
            || period == null || !PERIOD_PATTERN.matcher(period).matches()
            || metrics == null || metrics.isEmpty()) {
            return;
        }
        Date deadline = resolveDeadline(period);
        for (SharedKpiCollectView.Metric metric : metrics) {
            if (metric == null || metric.code() == null || !METRIC_NAMES.containsKey(metric.code())) {
                continue;
            }
            KpiSharedConfirm existing = confirmMapper.selectOne(
                Wrappers.<KpiSharedConfirm>lambdaQuery()
                    .eq(KpiSharedConfirm::getProjectId, projectId)
                    .eq(KpiSharedConfirm::getPeriod, period)
                    .eq(KpiSharedConfirm::getMetricCode, metric.code())
                    .last("LIMIT 1"));
            if (existing == null) {
                confirmMapper.insert(KpiSharedConfirm.builder()
                    .projectId(projectId)
                    .period(period)
                    .personId(collectedBy)
                    .metricCode(metric.code())
                    .metricName(METRIC_NAMES.get(metric.code()))
                    .weight(metric.weight())
                    .deadlineAt(deadline)
                    .status(ST_PENDING)
                    .build());
            } else if (ST_CONFIRMED.equals(existing.getStatus())) {
                // 重归集后需重新双签：updateById 忽略 null 字段，显式 SET 置空双签
                confirmMapper.update(null, Wrappers.<KpiSharedConfirm>lambdaUpdate()
                    .eq(KpiSharedConfirm::getId, existing.getId())
                    .set(KpiSharedConfirm::getStatus, ST_PENDING)
                    .set(KpiSharedConfirm::getFirstConfirmedBy, null)
                    .set(KpiSharedConfirm::getFirstConfirmedAt, null)
                    .set(KpiSharedConfirm::getSecondConfirmedBy, null)
                    .set(KpiSharedConfirm::getSecondConfirmedAt, null)
                    .set(KpiSharedConfirm::getPersonId, collectedBy)
                    .set(KpiSharedConfirm::getDeadlineAt, deadline));
            } else {
                existing.setDeadlineAt(deadline);
                confirmMapper.updateById(existing);
            }
        }
        log.debug("P3-1.2-BACKEND 共担KPI确认行就位 projectId={} period={} metrics={}",
            projectId, period, metrics.size());
    }

    /**
     * 双组长确认视角列表（{@code GET /api/v1/kpi/shared/confirms}）。
     *
     * <p>权限口径与 {@code KpiSharedCollectionService.listSharedKpis} 同严：
     * actor 必填、项目须存在、租户须匹配、非 SUPER_ADMIN 须为项目在职成员。
     *
     * @param statusFilter PENDING=待确认 / CONFIRMED=已确认 / OVERDUE=已逾期（派生）；null=全部
     */
    public List<KpiSharedConfirmView> listConfirms(IpdActor actor, Long projectId,
                                                   String period, String statusFilter) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未认证或凭证失效");
        }
        if (projectId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "projectId 不能为空");
        }
        if (period == null || !PERIOD_PATTERN.matcher(period).matches()) {
            throw new IpdBusinessException(ApiV1ErrorCode.KPI_PERIOD_INVALID);
        }
        boolean filterPending = false;
        boolean filterConfirmed = false;
        boolean filterOverdue = false;
        if (statusFilter != null && !statusFilter.isBlank()) {
            switch (statusFilter) {
                case ST_PENDING -> filterPending = true;
                case ST_CONFIRMED -> filterConfirmed = true;
                case ST_OVERDUE -> filterOverdue = true;
                default -> throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                    "status 仅支持 PENDING/CONFIRMED/OVERDUE");
            }
        }
        if (confirmMapper == null) {
            return List.of();
        }
        // 项目级鉴权（与 listSharedKpis 同严）：不存在/跨租户/非成员 → 统一 FORBIDDEN 不泄漏存在性
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权访问该项目");
        }
        KpiSharedCollectionService.requireTenantMatch(LoginHelper.getTenantId(), project);
        if (!"SUPER_ADMIN".equals(actor.role())) {
            boolean isMember = projectMemberMapper.selectCount(
                Wrappers.<ProjectMember>lambdaQuery()
                    .eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getPersonId, actor.id())
                    .isNull(ProjectMember::getExitDate)) > 0;
            if (!isMember) {
                throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权访问该项目");
            }
        }

        LambdaQueryWrapper<KpiSharedConfirm> wrapper = Wrappers.<KpiSharedConfirm>lambdaQuery()
            .eq(KpiSharedConfirm::getProjectId, projectId)
            .eq(KpiSharedConfirm::getPeriod, period);
        if (filterPending || filterOverdue) {
            // OVERDUE 是 PENDING 的派生态，两者都从 PENDING 存储态取数后内存分流
            wrapper.eq(KpiSharedConfirm::getStatus, ST_PENDING);
        } else if (filterConfirmed) {
            wrapper.eq(KpiSharedConfirm::getStatus, ST_CONFIRMED);
        }
        wrapper.orderByAsc(KpiSharedConfirm::getMetricCode);
        List<KpiSharedConfirm> rows = confirmMapper.selectList(wrapper);

        Date now = new Date();
        Map<Long, String> personNameCache = new HashMap<>();
        List<KpiSharedConfirmView> views = new ArrayList<>(rows.size());
        for (KpiSharedConfirm row : rows) {
            boolean overdue = ST_PENDING.equals(row.getStatus())
                && row.getDeadlineAt() != null && row.getDeadlineAt().before(now);
            if (filterPending && overdue) {
                continue;
            }
            if (filterOverdue && !overdue) {
                continue;
            }
            views.add(toView(row, project, personNameCache, actor, overdue));
        }
        return views;
    }

    /**
     * 双组长签署（{@code POST /api/v1/kpi/shared/{id}/confirm}）。
     *
     * <p>规则：
     * <ol>
     *   <li>仅 GROUP_LEADER / SUPER_ADMIN 可签（与归集录入同一角色梯度）</li>
     *   <li>首签 → firstConfirmedBy/At，状态仍 PENDING（confirmed=false）</li>
     *   <li>同人重复签 → {@link ApiV1ErrorCode#DUAL_SIGN_INCOMPLETE 40002 双签未完成}</li>
     *   <li>第二位不同组长签 → secondConfirmedBy/At，status=CONFIRMED（confirmed=true）</li>
     *   <li>已 CONFIRMED 再签 → STATE_CONFLICT</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfirmResult confirm(IpdActor actor, Long confirmId) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未认证或凭证失效");
        }
        if (!CONFIRM_ROLES.contains(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "共担 KPI 双组长确认仅产品组长可签署");
        }
        if (confirmId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "confirmId 不能为空");
        }
        if (confirmMapper == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "KpiSharedConfirmMapper 未注入");
        }
        KpiSharedConfirm row = confirmMapper.selectById(confirmId);
        if (row == null || "1".equals(row.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "共担 KPI 确认记录不存在: " + confirmId);
        }
        if (ST_CONFIRMED.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "该共担 KPI 已完成双组长确认");
        }

        Date now = new Date();
        String round;
        if (row.getFirstConfirmedBy() == null) {
            row.setFirstConfirmedBy(actor.id());
            row.setFirstConfirmedAt(now);
            round = "first";
        } else if (row.getFirstConfirmedBy().equals(actor.id())) {
            // 双签规则：第二签必须为另一位组长；同人重复签 = 双签未完成
            throw new IpdBusinessException(ApiV1ErrorCode.DUAL_SIGN_INCOMPLETE,
                "双组长确认需第二位不同组长签署，同一人不能重复确认");
        } else {
            row.setSecondConfirmedBy(actor.id());
            row.setSecondConfirmedAt(now);
            row.setStatus(ST_CONFIRMED);
            round = "second";
        }
        confirmMapper.updateById(row);

        if (auditLogService != null) {
            auditLogService.append(AuditLog.builder()
                .operatorId(actor.id())
                .operatorName(actor.name())
                .action("KPI_SHARED_CONFIRM")
                .entityType("kpi_shared_confirms")
                .entityId(confirmId)
                .reason("round=" + round + " period=" + row.getPeriod() + " metric=" + row.getMetricCode())
                .afterData(AuditEventData.json(
                    "projectId", row.getProjectId(),
                    "period", row.getPeriod(),
                    "metricCode", row.getMetricCode(),
                    "round", round,
                    "status", row.getStatus(),
                    "confirmedBy", actor.id()))
                .createTime(now)
                .build());
        }
        boolean completed = ST_CONFIRMED.equals(row.getStatus());
        log.info("共担KPI双组长确认 confirmId={} round={} actor={} completed={}",
            confirmId, round, actor.id(), completed);
        return new ConfirmResult(completed,
            completed ? ST_CONFIRMED : ST_PENDING,
            str(row.getFirstConfirmedBy()), str(row.getSecondConfirmedBy()));
    }

    /** 确认签署结果（卡片契约 { confirmed: boolean } + 当前状态与双签人） */
    public record ConfirmResult(boolean confirmed, String status,
                                String firstConfirmedBy, String secondConfirmedBy) { }

    /* ---------- 私有 helpers ---------- */

    private KpiSharedConfirmView toView(KpiSharedConfirm row, Project project,
                                        Map<Long, String> personNameCache, IpdActor actor,
                                        boolean overdue) {
        String personName = null;
        if (row.getPersonId() != null) {
            personName = personNameCache.computeIfAbsent(row.getPersonId(), pid -> {
                Person p = personMapper.selectById(pid);
                return p == null ? null : p.getName();
            });
        }
        boolean confirmedByMe = actor.id().equals(row.getFirstConfirmedBy())
            || actor.id().equals(row.getSecondConfirmedBy());
        return new KpiSharedConfirmView(
            str(row.getId()), row.getPeriod(), str(row.getProjectId()), project.getName(),
            str(row.getPersonId()), personName, row.getMetricCode(), row.getMetricName(),
            row.getWeight(), iso(row.getDeadlineAt()), overdue ? ST_OVERDUE : row.getStatus(),
            str(row.getFirstConfirmedBy()), iso(row.getFirstConfirmedAt()),
            str(row.getSecondConfirmedBy()), iso(row.getSecondConfirmedAt()),
            confirmedByMe);
    }

    private Date resolveDeadline(String period) {
        int day = DEFAULT_DEADLINE_DAY;
        if (systemConfigService != null) {
            day = systemConfigService.getIntValue("kpi.monthlyDeadlineDay", DEFAULT_DEADLINE_DAY);
        }
        return Date.from(KpiSharedCollectionService
            .resolveMonthlyDeadline(YearMonth.parse(period).plusMonths(1), day, ZoneId.systemDefault())
            .atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String str(Long value) {
        return value == null ? null : value.toString();
    }

    private static String iso(Date date) {
        if (date == null) {
            return null;
        }
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
