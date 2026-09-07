package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.dto.SharedKpiCollectReq;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.vo.SharedKpiCollectView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * K01-K04 共担 KPI 归集（P3-1.2）。
 *
 * <p>产品组长录入后按项目内双 PM 各追加一条不可变版本，相同业务得分保证
 * AC-KPI-11；NPS 有效样本不足 30 时只标记待补充、不进入分母。
 */
@Service
public class KpiSharedCollectionService {

    @Autowired
    public KpiSharedCollectionService(
        KpiRecordMapper kpiRecordMapper,
        ProjectMapper projectMapper,
        ProjectMemberMapper projectMemberMapper,
        PersonMapper personMapper,
        IpdPermission permission,
        AuditLogService auditLogService,
        ProductGroupMapper productGroupMapper,
        SystemConfigService systemConfigService,
        NotificationService notificationService) {
        this.kpiRecordMapper = kpiRecordMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.personMapper = personMapper;
        this.permission = permission;
        this.auditLogService = auditLogService;
        this.productGroupMapper = productGroupMapper;
        this.systemConfigService = systemConfigService;
        this.notificationService = notificationService;
    }

    /** 兼容 P3-1.2 早期测试/调用方；生产 Spring 使用完整构造器。 */
    public KpiSharedCollectionService(
        KpiRecordMapper kpiRecordMapper,
        ProjectMapper projectMapper,
        ProjectMemberMapper projectMemberMapper,
        PersonMapper personMapper,
        IpdPermission permission,
        AuditLogService auditLogService) {
        this(kpiRecordMapper, projectMapper, projectMemberMapper, personMapper, permission,
            auditLogService, null, null, null);
    }

    static final String K01 = "K01";
    static final String K02 = "K02";
    static final String K03 = "K03";
    static final String K04 = "K04";
    static final String TYPE_SHARED = "SHARED";
    static final String SEGMENT_FULL_SHARED = "FULL_SHARED";

    static final BigDecimal K01_WEIGHT = new BigDecimal("0.15");
    static final BigDecimal K02_WEIGHT = new BigDecimal("0.10");
    static final BigDecimal K03_WEIGHT = new BigDecimal("0.10");
    static final BigDecimal K04_WEIGHT = new BigDecimal("0.05");
    static final BigDecimal FUNCTIONAL_WEIGHT = new BigDecimal("0.60");
    static final BigDecimal SHARED_WEIGHT = new BigDecimal("0.40");
    static final int NPS_MIN_SAMPLE = 30;
    static final int REVISION_INITIAL = 1;

    private final KpiRecordMapper kpiRecordMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final PersonMapper personMapper;
    private final IpdPermission permission;
    private final AuditLogService auditLogService;
    private final ProductGroupMapper productGroupMapper;
    private final SystemConfigService systemConfigService;
    private final NotificationService notificationService;

    /** K01-K04 月度归集；同一请求为项目双 PM 原子追加相同版本。 */
    @Transactional(rollbackFor = Exception.class)
    public SharedKpiCollectView collectSharedKpi(IpdActor requestedActor, SharedKpiCollectReq request) {
        IpdActor actor = permission.requireLeaderOrAdmin();
        requireRequestedActorMatches(requestedActor, actor);
        requireCollectorRole(actor);
        validateRequest(request);

        Project project = requireProject(request.projectId());
        requireProjectAccess(actor, project);
        List<ProjectMember> members = activeMembers(request.projectId());
        ProjectMember marketPm = requireSingleMember(members, "MARKET_PM");
        ProjectMember rdPm = requireSingleMember(members, "RD_PM");
        requireMemberPerson(marketPm);
        requireMemberPerson(rdPm);

        List<MetricResult> metrics = calculateMetrics(request, project);
        BigDecimal sharedScore = weightedScore(metrics);

        Date now = new Date();
        Set<Long> participantIds = new LinkedHashSet<>();
        participantIds.add(marketPm.getPersonId());
        participantIds.add(rdPm.getPersonId());
        for (ProjectMember member : members) {
            if (member.getPersonId() != null) {
                participantIds.add(member.getPersonId());
            }
        }

        for (Long personId : List.of(marketPm.getPersonId(), rdPm.getPersonId())) {
            KpiRecord previous = kpiRecordMapper.selectOne(
                Wrappers.<KpiRecord>lambdaQuery()
                    .eq(KpiRecord::getProjectId, request.projectId())
                    .eq(KpiRecord::getPersonId, personId)
                    .eq(KpiRecord::getKpiType, TYPE_SHARED)
                    .eq(KpiRecord::getPeriod, request.period())
                    .orderByDesc(KpiRecord::getRevision)
                    .last("LIMIT 1 FOR UPDATE"));
            int revision = previous != null && previous.getRevision() != null
                ? previous.getRevision() + 1 : REVISION_INITIAL;
            String detail = AuditEventData.json(
                "source", "GROUP_LEADER_COLLECTION",
                "projectId", request.projectId(),
                "period", request.period(),
                "metrics", metrics,
                "participantIds", participantIds,
                "collectedBy", actor.id(),
                "revision", revision);
            KpiRecord row = KpiRecord.builder()
                .projectId(request.projectId())
                .personId(personId)
                .kpiType(TYPE_SHARED)
                .period(request.period())
                .sharedDetail(detail)
                .comprehensiveScore(sharedScore)
                .segment(SEGMENT_FULL_SHARED)
                .scoredBy(actor.id())
                .status("FINALIZED")
                .scoredAt(now)
                .revision(revision)
                .build();
            permission.bindCreateAudit(row, actor);
            kpiRecordMapper.insert(row);
        }

        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .action("KPI_SHARED_COLLECT")
            .entityType("kpi_records")
            .entityId(request.projectId())
            .reason("K01-K04 双 PM 同分归集")
            .afterData(AuditEventData.json(
                "projectId", request.projectId(),
                "period", request.period(),
                "sharedScore", sharedScore,
                "participantIds", participantIds))
            .createTime(now)
            .build());
        logResult(actor, request, sharedScore);
        return new SharedKpiCollectView(
            request.projectId(), request.period(), sharedScore, metrics.stream().map(MetricResult::view).toList());
    }

    /**
     * 次月第 N 个工作日 18:00 截止（P3-1.3）。周末跳过，不使用硬编码日历。
     */
    public static LocalDateTime resolveMonthlyDeadline(YearMonth nextMonth, int configuredDay, ZoneId zoneId) {
        if (nextMonth == null || zoneId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "截止月份/时区不能为空");
        }
        if (configuredDay < 1 || configuredDay > 31) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "工作日序号必须在 1~31");
        }
        LocalDate date = nextMonth.atDay(1);
        int remaining = configuredDay;
        while (remaining > 0) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                remaining--;
            }
            date = date.plusDays(1);
        }
        return LocalDateTime.of(date, LocalTime.of(18, 0));
    }

    /** 使用实时配置（kpi.monthlyDeadlineDay）的扫描入口。 */
    @Transactional(rollbackFor = Exception.class)
    public DeadlineScanResult scanMonthlyDeadlines(LocalDate scanDate, YearMonth collectionPeriod) {
        int configuredDay = systemConfigService == null
            ? 5 : systemConfigService.getIntValue("kpi.monthlyDeadlineDay", 5);
        return scanMonthlyDeadlines(scanDate, collectionPeriod, configuredDay);
    }

    /**
     * 只发到期提醒，不关闭或改写任何 KpiRecord。通知 outbox 的 receiver 级唯一键
     * 保证同日重复扫描幂等；已有 FINALIZED 归集直接跳过。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeadlineScanResult scanMonthlyDeadlines(
        LocalDate scanDate, YearMonth collectionPeriod, int configuredDay) {
        if (scanDate == null || collectionPeriod == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "扫描日期/归集周期不能为空");
        }
        LocalDate deadlineDate = resolveMonthlyDeadline(collectionPeriod.plusMonths(1), configuredDay,
            ZoneId.systemDefault()).toLocalDate();
        long overdueDays = ChronoUnit.DAYS.between(deadlineDate, scanDate);
        if (overdueDays <= 0) {
            return new DeadlineScanResult(0, 0, 0);
        }

        List<Project> projects = projectMapper.selectList(
            Wrappers.<Project>lambdaQuery()
                .in(Project::getStatus, List.of("ACTIVE", "LIFECYCLE"))
                .and(w -> w.ne(Project::getDelFlag, "1").or().isNull(Project::getDelFlag)));
        int day1 = 0;
        int day3 = 0;
        int skipped = 0;
        for (Project project : projects) {
            if (hasFinalizedCollection(project.getId(), collectionPeriod)) {
                skipped++;
                continue;
            }
            if (overdueDays >= 1) {
                day1 += notifyDay1(project, collectionPeriod);
            }
            if (overdueDays >= 3) {
                day3 += notifyDay3(project, collectionPeriod);
            }
        }
        return new DeadlineScanResult(day1, day3, skipped);
    }

    private boolean hasFinalizedCollection(Long projectId, YearMonth period) {
        Long count = kpiRecordMapper.selectCount(
            Wrappers.<KpiRecord>lambdaQuery()
                .eq(KpiRecord::getProjectId, projectId)
                .eq(KpiRecord::getPeriod, period.toString())
                .eq(KpiRecord::getKpiType, TYPE_SHARED)
                .eq(KpiRecord::getStatus, "FINALIZED")
                .last("LIMIT 1"));
        return count != null && count > 0;
    }

    /**
     * HIGH-4.1：使用实时配置（kpi.monthlyDeadlineDay）的截止日前 1 天提醒入口。
     * 只发 FYI 提醒，不关闭或改写任何 KpiRecord；已有 FINALIZED 归集直接跳过。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeadlineScanResult scanDueSoon(LocalDate scanDate, YearMonth collectionPeriod) {
        int configuredDay = systemConfigService == null
            ? 5 : systemConfigService.getIntValue("kpi.monthlyDeadlineDay", 5);
        return scanDueSoon(scanDate, collectionPeriod, configuredDay);
    }

    /**
     * HIGH-4.1：截止日前 1 天提醒（daysBefore=1 触发；其他天数静默返回）。
     * 收件人：产品组长（FYI）；同 receiver+dateStamp 维度去重，重复扫描幂等。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeadlineScanResult scanDueSoon(
        LocalDate scanDate, YearMonth collectionPeriod, int configuredDay) {
        if (scanDate == null || collectionPeriod == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "扫描日期/归集周期不能为空");
        }
        LocalDate deadlineDate = resolveMonthlyDeadline(collectionPeriod.plusMonths(1), configuredDay,
            ZoneId.systemDefault()).toLocalDate();
        long daysBefore = ChronoUnit.DAYS.between(scanDate, deadlineDate);
        if (daysBefore != 1) {
            return new DeadlineScanResult(0, 0, 0);
        }

        List<Project> projects = projectMapper.selectList(
            Wrappers.<Project>lambdaQuery()
                .in(Project::getStatus, List.of("ACTIVE", "LIFECYCLE"))
                .and(w -> w.ne(Project::getDelFlag, "1").or().isNull(Project::getDelFlag)));
        int reminded = 0;
        int skipped = 0;
        for (Project project : projects) {
            if (hasFinalizedCollection(project.getId(), collectionPeriod)) {
                skipped++;
                continue;
            }
            reminded += notifyDueSoon(project, collectionPeriod);
        }
        return new DeadlineScanResult(reminded, 0, skipped);
    }

    private int notifyDueSoon(Project project, YearMonth period) {
        Set<Long> receivers = leadersForProject(project);
        for (Long receiver : receivers) {
            publishDueSoon(receiver, project, period);
        }
        return receivers.size();
    }

    private void publishDueSoon(Long receiverId, Project project, YearMonth period) {
        if (notificationService == null) {
            return;
        }
        notificationService.publishDaily(receiverId, NotificationService.Types.KPI_DUE_SOON,
            NotificationService.KIND_FYI, "KPI_SHARED_COLLECTION", project.getId(),
            "共担 KPI 即将截止", "次日 18:00 为 K01-K04 归集截止日，请产品组长尽快归集",
            "/kpi/shared?period=" + period, java.util.Date.from(java.time.ZonedDateTime.now().toInstant()));
        auditLogService.append(AuditLog.builder()
            .operatorId(0L)
            .operatorName("KPI_DEADLINE_SCANNER")
            .action("KPI_DUE_SOON_REMIND")
            .entityType("projects")
            .entityId(project.getId())
            .reason("period=" + period)
            .afterData(AuditEventData.json("receiverId", receiverId, "projectId", project.getId(),
                "period", period.toString()))
            .createTime(new Date())
            .build());
    }

    /**
     * HIGH-4.1：当前生效的截止日配置视图（前端可读、运维可观察）。
     * <p>source 取值：
     * <ul>
     *   <li>FACTORY_DEFAULT — DB 无该行（含逻辑删除），使用 Java 默认值 5</li>
     *   <li>DB_ACTIVE — DB 有该行（未删）且 value 非空</li>
     *   <li>DB_INACTIVE — DB 有该行但 value 为空，回退 default_value</li>
     * </ul>
     */
    public DeadlineConfigView getDeadlineConfig() {
        int defaultDay = 5;
        if (systemConfigService == null) {
            return new DeadlineConfigView(defaultDay,
                resolveMonthlyDeadline(YearMonth.now().plusMonths(1), defaultDay,
                    ZoneId.systemDefault()),
                0, "FACTORY_DEFAULT", null);
        }
        // getValue 默认值=空字符串：空表示 config_value 未配置；非空表示已配置
        String configuredRaw = systemConfigService.getValue("kpi.monthlyDeadlineDay", "");
        java.util.LinkedHashMap<String, Object> view = systemConfigService.resolveAsOf(
            "kpi.monthlyDeadlineDay", new Date());
        String resolvedFrom = String.valueOf(view.get("resolvedFrom"));
        Object versionObj = view.get("version");
        int version = versionObj instanceof Integer ? (Integer) versionObj : 0;

        boolean hasConfig = configuredRaw != null && !configuredRaw.isBlank();
        boolean hasRow = !"NONE".equals(resolvedFrom);
        int day;
        try {
            day = hasConfig ? Integer.parseInt(configuredRaw.trim()) : defaultDay;
        } catch (NumberFormatException e) {
            day = defaultDay;
        }
        String source;
        if (!hasRow) {
            source = "FACTORY_DEFAULT";
        } else if (!hasConfig) {
            source = "DB_INACTIVE";
        } else {
            source = "DB_ACTIVE";
        }
        if (day < 1 || day > 31) {
            day = defaultDay;
        }
        YearMonth targetMonth = YearMonth.now().plusMonths(1);
        LocalDateTime cutoff = resolveMonthlyDeadline(targetMonth, day, ZoneId.systemDefault());
        return new DeadlineConfigView(day, cutoff, version, source,
            hasConfig ? configuredRaw : null);
    }


    /**
     * W4-E §1.5：按 projectId + period 列出当期全部 SHARED 归集记录（含所有 revision，按 revision DESC 排序）。
     *
     * <p>件 1 Controller {@code GET /api/v1/kpi/shared?projectId&period} 的服务入口；前端页 30 共担 KPI 归集列表读端点。
     * <p>同一项目双 PM 必产生 2 条同 revision 记录（K01-K04 双 PM 同分归集），新版本归集时 revision + 1 追加。
     * <p>不写审计、不变更状态，纯查询。
     *
     * @param projectId 项目主键（必填）
     * @param period YYYY-MM（必填）
     * @return KpiRecord 列表（可能为空但不会为 null）；按 revision DESC, id ASC 排序保证最新版本在前
     */
    public List<KpiRecord> listSharedKpis(Long projectId, String period) {
        if (projectId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "projectId 不能为空");
        }
        validatePeriodString(period);
        if (kpiRecordMapper == null) {
            return Collections.emptyList();
        }
        List<KpiRecord> rows = kpiRecordMapper.selectList(
            Wrappers.<KpiRecord>lambdaQuery()
                .eq(KpiRecord::getProjectId, projectId)
                .eq(KpiRecord::getPeriod, period)
                .eq(KpiRecord::getKpiType, TYPE_SHARED)
                .orderByDesc(KpiRecord::getRevision)
                .orderByAsc(KpiRecord::getId));
        return rows == null ? Collections.emptyList() : rows;
    }

    private static void validatePeriodString(String period) {
        if (period == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "period 不能为空");
        }
        try {
            YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "period 必须为 YYYY-MM");
        }
    }

    private int notifyDay1(Project project, YearMonth period) {
        Set<Long> receivers = leadersForProject(project);
        for (Long receiver : receivers) {
            publishDeadline(receiver, "KPI_SHARED_DEADLINE_DAY1", project, period,
                "共担 KPI 逾期提醒", "K01-K04 已逾期 1 天，请产品组长尽快归集");
        }
        return receivers.size();
    }

    private int notifyDay3(Project project, YearMonth period) {
        List<Person> admins = personMapper.selectList(
            Wrappers.<Person>lambdaQuery()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .ne(Person::getDelFlag, "1"));
        Set<Long> receivers = new LinkedHashSet<>();
        for (Person admin : admins) {
            if (admin.getId() != null) {
                receivers.add(admin.getId());
            }
        }
        for (Long receiver : receivers) {
            publishDeadline(receiver, "KPI_SHARED_DEADLINE_DAY3", project, period,
                "共担 KPI 逾期升级", "K01-K04 已逾期 3 天，请超管介入");
        }
        return receivers.size();
    }

    private Set<Long> leadersForProject(Project project) {
        Set<Long> leaders = new LinkedHashSet<>();
        for (ProjectMember member : activeMembers(project.getId())) {
            Person pm = member.getPersonId() == null ? null : personMapper.selectById(member.getPersonId());
            if (pm == null || pm.getGroupId() == null) {
                continue;
            }
            ProductGroup group = productGroupMapper.selectById(pm.getGroupId());
            if (group != null && group.getLeaderPersonId() != null) {
                leaders.add(group.getLeaderPersonId());
            }
        }
        if (project.getMainGroupId() != null) {
            ProductGroup main = productGroupMapper.selectById(project.getMainGroupId());
            if (main != null && main.getLeaderPersonId() != null) {
                leaders.add(main.getLeaderPersonId());
            }
        }
        return leaders;
    }

    private void publishDeadline(Long receiverId, String eventType, Project project, YearMonth period,
                                String title, String content) {
        if (notificationService == null) {
            return;
        }
        notificationService.publish(receiverId, eventType, NotificationService.KIND_ACTION,
            "KPI_SHARED_COLLECTION", project.getId(), title, content,
            "/kpi/shared?period=" + period);
        auditLogService.append(AuditLog.builder()
            .operatorId(0L)
            .operatorName("KPI_DEADLINE_SCANNER")
            .action("KPI_SHARED_DEADLINE_REMIND")
            .entityType("projects")
            .entityId(project.getId())
            .reason("eventType=" + eventType + " period=" + period)
            .afterData(AuditEventData.json("receiverId", receiverId, "projectId", project.getId(),
                "period", period.toString(), "eventType", eventType))
            .createTime(new Date())
            .build());
    }

    public record DeadlineScanResult(int day1Reminders, int day3Escalations, int skippedProjects) { }

    /**
     * HIGH-4.1：当前生效的月度截止日配置视图（{@code GET /api/v1/kpi/shared/deadline-config} 返回契约）。
     * <p>{@code source} 取值：
     * <ul>
     *   <li>{@code FACTORY_DEFAULT} — DB 无该行（含逻辑删除），使用 Java 默认值 5</li>
     *   <li>{@code DB_ACTIVE} — DB 有该行（未删）且 value 非空</li>
     *   <li>{@code DB_INACTIVE} — DB 有该行但 value 为空，回退 default_value</li>
     * </ul>
     *
     * @param dayOfMonth   截止日序号（次月第 N 个工作日）
     * @param cutoffTime   解析后的截止时刻（含工作日跳过）
     * @param version      当前版本号（无版本链时为 0）
     * @param source       取值来源
     * @param configuredValue 库内配置字符串（无则 null）
     */
    public record DeadlineConfigView(
        int dayOfMonth,
        LocalDateTime cutoffTime,
        int version,
        String source,
        String configuredValue) {
    }

    /** 达成率阶梯：每低 5 个百分点扣 1 分；<50%=0。 */
    public static BigDecimal calculateSharedAchievement(BigDecimal achievementPercent, BigDecimal fullScore) {
        if (achievementPercent == null || fullScore == null
            || fullScore.compareTo(BigDecimal.ZERO) < 0 || fullScore.compareTo(new BigDecimal("100")) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "满分必须在 [0,100]");
        }
        if (achievementPercent.compareTo(new BigDecimal("100")) >= 0) {
            return fullScore.setScale(2, RoundingMode.HALF_UP);
        }
        if (achievementPercent.compareTo(new BigDecimal("50")) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal deductions = new BigDecimal("100").subtract(achievementPercent)
            .divide(new BigDecimal("5"), 0, RoundingMode.CEILING);
        return fullScore.subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private List<MetricResult> calculateMetrics(SharedKpiCollectReq request, Project project) {
        requirePositive(project.getTargetSalesAmount(), "项目目标销售额");
        requirePositive(project.getTargetChannelCount(), "项目目标渠道数");
        requirePositive(project.getTargetSceneCount(), "项目规划场景数");
        if (project.getTargetNps() == null || project.getTargetNps() < 0 || project.getTargetNps() > 100) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "项目 NPS 目标必须在 [0,100]");
        }

        BigDecimal actualSales = decimal(request.actualSales());
        BigDecimal targetSales = project.getTargetSalesAmount();
        BigDecimal actualChannels = decimal(request.actualChannels());
        BigDecimal targetChannels = BigDecimal.valueOf(project.getTargetChannelCount());
        BigDecimal nps = BigDecimal.valueOf(request.promoters()).subtract(BigDecimal.valueOf(request.detractors()));
        BigDecimal landed = BigDecimal.valueOf(request.landedScenarios());
        BigDecimal planned = BigDecimal.valueOf(request.plannedScenarios());
        if (actualChannels.compareTo(BigDecimal.ZERO) < 0 || landed.compareTo(planned) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际渠道数/落地场景数不能超过目标");
        }

        MetricResult sales = MetricResult.of(K01, "FINANCE_OR_ERP", actualSales, targetSales,
            K01_WEIGHT, true, "销量/出货量达成率");
        MetricResult channel = MetricResult.of(K02, "CHANNEL_CRM", actualChannels, targetChannels,
            K02_WEIGHT, true, "渠道商覆盖达成率");
        boolean npsIncluded = request.npsSampleSize() >= NPS_MIN_SAMPLE;
        BigDecimal npsTarget = BigDecimal.valueOf(project.getTargetNps());
        BigDecimal npsScore = npsIncluded ? calculateNpsTargetScore(nps, npsTarget) : BigDecimal.ZERO;
        MetricResult npsMetric = MetricResult.custom(K03, "NPS_SURVEY", nps, npsTarget,
            npsScore.setScale(2, RoundingMode.HALF_UP), K03_WEIGHT, npsIncluded,
            npsIncluded ? "NPS=" + nps.toPlainString() : "样本不足 30，结果不计入，待补充");
        MetricResult scenario = MetricResult.of(K04, "SALES_ACCEPTANCE", landed, planned,
            K04_WEIGHT, true, "场景覆盖率");
        return new ArrayList<>(List.of(sales, channel, npsMetric, scenario));
    }

    private BigDecimal weightedScore(List<MetricResult> metrics) {
        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denominator = BigDecimal.ZERO;
        for (MetricResult metric : metrics) {
            if (metric.included()) {
                numerator = numerator.add(metric.score().multiply(metric.weight()));
                denominator = denominator.add(metric.weight());
            }
        }
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void validateRequest(SharedKpiCollectReq request) {
        if (request == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "归集请求不能为空");
        }
        if (request.promoters() == null || request.detractors() == null || request.npsSampleSize() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "NPS 样本字段不能为空");
        }
        if (request.promoters() + request.detractors() > request.npsSampleSize()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "推荐者+贬损者不能超过有效样本数");
        }
        try {
            YearMonth.parse(request.period());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "period 必须为 YYYY-MM");
        }
        if (request.targetChannels() == null || request.targetChannels() <= 0
            || request.plannedScenarios() == null || request.plannedScenarios() <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "目标渠道数/规划场景数必须大于 0");
        }
    }

    private Project requireProject(Long projectId) {
        Project project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private List<ProjectMember> activeMembers(Long projectId) {
        return projectMemberMapper.selectList(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .isNull(ProjectMember::getExitDate)
                .in(ProjectMember::getRole, List.of("MARKET_PM", "RD_PM"))
                .orderByAsc(ProjectMember::getId));
    }

    private ProjectMember requireSingleMember(List<ProjectMember> members, String role) {
        List<ProjectMember> roleMembers = members.stream()
            .filter(member -> role.equals(member.getRole()))
            .toList();
        if (roleMembers.size() != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "项目必须有且仅有一个在职" + role + " 才能归集");
        }
        return roleMembers.get(0);
    }

    private void requireMemberPerson(ProjectMember member) {
        Person person = member.getPersonId() == null ? null : personMapper.selectById(member.getPersonId());
        if (person == null || !member.getRole().equals(person.getPersonType())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "项目成员角色与人员角色不一致");
        }
    }

    private void requireCollectorRole(IpdActor actor) {
        if (actor == null || !Set.of("GROUP_LEADER", "SUPER_ADMIN").contains(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "K01-K04 仅产品组长可录入与复核");
        }
    }

    private void requireProjectAccess(IpdActor actor, Project project) {
        if (actor == null || !Set.of("GROUP_LEADER", "SUPER_ADMIN").contains(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "K01-K04 仅产品组长可录入与复核");
        }
        if ("SUPER_ADMIN".equals(actor.role())) {
            return;
        }
        List<ProjectMember> members = activeMembers(project.getId());
        Set<Long> memberGroupIds = new LinkedHashSet<>();
        for (ProjectMember member : members) {
            Person person = member.getPersonId() == null ? null : personMapper.selectById(member.getPersonId());
            if (person != null) {
                memberGroupIds.add(person.getGroupId());
            }
        }
        if (project.getMainGroupId() != null && project.getMainGroupId().equals(actor.groupId())) {
            return;
        }
        if (memberGroupIds.contains(actor.groupId())) {
            return;
        }
        throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "不能跨产品组归集 KPI");
    }

    private static void requireRequestedActorMatches(IpdActor requested, IpdActor authenticated) {
        if (requested != null && authenticated != null && !Objects.equals(requested.id(), authenticated.id())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "不能代替其他会话人归集");
        }
    }

    private static BigDecimal decimal(String value) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() < 0) {
                throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际值不能为负");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "数值字段格式非法");
        }
    }

    private static void requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, label + "必须大于 0");
        }
    }

    private static void requirePositive(Integer value, String label) {
        if (value == null || value <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, label + "必须大于 0");
        }
    }

    private static void logResult(IpdActor actor, SharedKpiCollectReq request, BigDecimal score) {
        org.slf4j.LoggerFactory.getLogger(KpiSharedCollectionService.class).info(
            "KPI 共担归集 actor={} projectId={} period={} score={}",
            actor.id(), request.projectId(), request.period(), score);
    }

    private static BigDecimal calculateNpsTargetScore(BigDecimal actual, BigDecimal target) {
        if (actual.compareTo(target) >= 0) {
            return new BigDecimal("100");
        }
        BigDecimal deductions = target.subtract(actual)
            .divide(new BigDecimal("5"), 0, RoundingMode.CEILING);
        return new BigDecimal("100").subtract(deductions).max(BigDecimal.ZERO);
    }

    record MetricResult(String code, String source, BigDecimal actual, BigDecimal target,
                        BigDecimal score, BigDecimal weight, boolean included, String message) {
        static MetricResult of(String code, String source, BigDecimal actual, BigDecimal target,
                               BigDecimal weight, boolean included, String message) {
            BigDecimal achievement = target.signum() == 0
                ? BigDecimal.ZERO
                : actual.divide(target, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            BigDecimal score = calculateSharedAchievement(achievement, new BigDecimal("100"));
            return new MetricResult(code, source, actual, target,
                score.setScale(2, RoundingMode.HALF_UP), weight, included, message);
        }

        static MetricResult custom(String code, String source, BigDecimal actual, BigDecimal target,
                                   BigDecimal score, BigDecimal weight, boolean included, String message) {
            return new MetricResult(code, source, actual, target,
                score.setScale(2, RoundingMode.HALF_UP), weight, included, message);
        }

        SharedKpiCollectView.Metric view() {
            return new SharedKpiCollectView.Metric(code, source,
                actual.toPlainString(), target.stripTrailingZeros().toPlainString(),
                score, weight, included, message);
        }
    }
}
