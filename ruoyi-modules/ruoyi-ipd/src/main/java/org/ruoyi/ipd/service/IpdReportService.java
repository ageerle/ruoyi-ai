package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.dto.ReportExportResult;
import org.ruoyi.ipd.dto.ReportSummaryRow;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * P4-4.1 报表服务：项目绩效汇总列表 + 津贴/奖金/项目汇总三导出（AC-INC-34）。
 *
 * <p>BR-INC-15：汇总口径不重复计数（自然键 GROUP BY）；列表与导出同范围同筛选；
 * 金额统一 setScale(2, HALF_UP)（元）；列表 MAX_PAGE_SIZE=500 / 导出 MAX_EXPORT_SIZE=5000；
 * 导出写 EXPORT_REPORT 审计（独立事务，失败不阻断业务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpdReportService {

    /** 月份格式 YYYY-MM（BR-INC-15 列表与导出同口径校验） */
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    /** 列表分页上限（BR-INC-15 大数据分页保护） */
    private static final int MAX_PAGE_SIZE = 500;

    /** 单次导出行数上限（BR-INC-15 导出保护） */
    private static final int MAX_EXPORT_SIZE = 5000;

    /** 津贴台账导出列头（15 列，中文固定） */
    private static final List<String> ALLOWANCE_HEADERS = List.of(
        "月份", "人员ID", "姓名", "工号", "项目ID", "项目编号", "项目名称", "锁定评级",
        "基础额(元)", "终额(元)", "是否封顶", "停发原因", "停开始日期", "台账ID", "创建时间");

    /** 奖金台账导出列头（14 列，中文固定） */
    private static final List<String> BONUS_HEADERS = List.of(
        "奖金池ID", "项目ID", "项目编号", "项目名称", "目标销售额(元)", "奖金池比率",
        "基础池(元)", "难度系数", "达成率", "档位系数", "终池(元)", "状态", "计算时间", "分配时间");

    /** 项目汇总导出列头（10 列，中文固定） */
    private static final List<String> PROJECT_SUMMARY_HEADERS = List.of(
        "月份", "项目ID", "项目编号", "项目名称", "津贴终额合计(元)", "奖金终池合计(元)",
        "加权绩效平均分", "津贴行数", "奖金行数", "绩效行数");

    private final AllowanceLedgerMapper allowanceLedgerMapper;
    private final BonusPoolMapper bonusPoolMapper;
    private final ProjectScoreMapper projectScoreMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final PersonMapper personMapper;
    private final AuditLogService auditLogService;
    private final IpdPermission ipdPermission;

    /**
     * P4-4.1 §1：项目绩效汇总列表（分页，单项目一行）。
     *
     * <p>与 {@link #exportProjectSummary} 同范围同筛选（month + productId + keyword + actor 可见集）。
     * <p>口径（BR-INC-15 不重复计数）：
     * <ul>
     *   <li>津贴：SUM(finalAmount) GROUP BY projectId WHERE month=?（自然键 personId×projectId×month）</li>
     *   <li>奖金：SUM(finalPool) GROUP BY projectId（三态 DRAFT/CONFIRMED/DISTRIBUTED 均计入）</li>
     *   <li>绩效：AVG(weightedScore) GROUP BY projectId WHERE status='CONFIRMED'（自然键 projectId×personId×pmRole）</li>
     * </ul>
     * <p>pageSize 上限 {@link #MAX_PAGE_SIZE}=500；项目维度天然有限，内存分页。
     */
    public Page<ReportSummaryRow> listProjectSummaries(String month,
                                                       Long productId,
                                                       String keyword,
                                                       int pageNo,
                                                       int pageSize,
                                                       IpdActor actor) {
        validateMonth(month);
        int pn = Math.max(1, pageNo);
        int ps = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        List<Long> visibleProjectIds = resolveVisibleProjectIds(actor);
        if (visibleProjectIds != null && visibleProjectIds.isEmpty()) {
            return new Page<>(pn, ps, 0);
        }

        LambdaQueryWrapper<Project> pj = new LambdaQueryWrapper<>();
        if (visibleProjectIds != null) pj.in(Project::getId, visibleProjectIds);
        if (productId != null) pj.eq(Project::getProductId, productId);
        if (keyword != null && !keyword.isBlank()) {
            pj.and(w -> w.like(Project::getCode, keyword).or().like(Project::getName, keyword));
        }
        pj.orderByAsc(Project::getId);
        List<Project> projects = projectMapper.selectList(pj);

        Map<Long, BigDecimal> allowanceSum = sumAllowanceByProject(month, projects);
        Map<Long, BigDecimal> bonusSum = sumBonusByProject(projects);
        Map<Long, BigDecimal> scoreAvg = avgScoreByProject(projects);

        List<ReportSummaryRow> all = new ArrayList<>(projects.size());
        for (Project p : projects) {
            BigDecimal avg = scoreAvg.get(p.getId());
            all.add(new ReportSummaryRow(
                p.getId(),
                safe(p.getCode()),
                safe(p.getName()),
                month,
                allowanceSum.getOrDefault(p.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                bonusSum.getOrDefault(p.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                avg == null ? null : avg.setScale(2, RoundingMode.HALF_UP),
                allowanceCountByProject(month, p.getId()),
                bonusCountByProject(p.getId()),
                scoreCountByProject(p.getId())));
        }

        // Step 5：内存分页（项目数受 MAX_PAGE_SIZE 保护）
        int total = all.size();
        int from = Math.min((pn - 1) * ps, total);
        int to = Math.min(from + ps, total);
        List<ReportSummaryRow> pageRows = all.subList(from, to);

        Page<ReportSummaryRow> result = new Page<>(pn, ps, total);
        result.setRecords(pageRows);
        return result;
    }

    // ========================================================================
    //  导出：津贴 / 奖金 / 项目汇总
    // ========================================================================

    /**
     * P4-4.1 §2.1：津贴台账导出（AC-INC-34）。
     *
     * <p>范围：month + 可选 projectId + actor 可见过滤。
     * <p>脱敏：不导出凭证字段；列头固定中文。
     * <p>审计：写一条 EXPORT_REPORT（独立事务落名）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportExportResult exportAllowance(String month,
                                              Long projectId,
                                              Long personId,
                                              IpdActor actor) {
        validateMonth(month);
        List<Long> visibleProjectIds = resolveVisibleProjectIds(actor);

        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, month);
        if (projectId != null) q.eq(AllowanceLedger::getProjectId, projectId);
        if (personId != null) q.eq(AllowanceLedger::getPersonId, personId);
        if (visibleProjectIds != null) {
            if (visibleProjectIds.isEmpty()) return emptyAllowanceResult(month, projectId, personId, actor);
            q.in(AllowanceLedger::getProjectId, visibleProjectIds);
        }
        q.orderByAsc(AllowanceLedger::getProjectId, AllowanceLedger::getPersonId);
        List<AllowanceLedger> list = allowanceLedgerMapper.selectList(q);
        if (list.size() > MAX_EXPORT_SIZE) {
            list = list.subList(0, MAX_EXPORT_SIZE);
        }

        // 关联 person / project 名称（避免前端再 join）
        Set<Long> personIds = list.stream().map(AllowanceLedger::getPersonId).collect(Collectors.toSet());
        Set<Long> projectIds = list.stream().map(AllowanceLedger::getProjectId).collect(Collectors.toSet());
        Map<Long, String> personName = personIds.isEmpty() ? Map.of()
            : personMapper.selectBatchIds(personIds).stream()
                .collect(Collectors.toMap(Person::getId, Person::getName, (a, b) -> a));
        Map<Long, Project> projectMap = projectIds.isEmpty() ? Map.of()
            : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>(list.size());
        for (AllowanceLedger l : list) {
            Person p = personName.containsKey(l.getPersonId())
                ? personMapper.selectById(l.getPersonId()) : null;
            Project pj = projectMap.get(l.getProjectId());
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("月份", safe(month));
            row1.put("人员ID", l.getPersonId());
            row1.put("姓名", p == null ? "" : safe(p.getName()));
            row1.put("工号", p == null ? "" : safe(p.getEmployeeNo()));
            row1.put("项目ID", l.getProjectId());
            row1.put("项目编号", pj == null ? "" : safe(pj.getCode()));
            row1.put("项目名称", pj == null ? "" : safe(pj.getName()));
            row1.put("锁定评级", safe(l.getLockedLevel()));
            row1.put("基础额(元)", l.getBaseAmount() == null ? BigDecimal.ZERO.setScale(2) : l.getBaseAmount().setScale(2, RoundingMode.HALF_UP));
            row1.put("终额(元)", l.getFinalAmount() == null ? BigDecimal.ZERO.setScale(2) : l.getFinalAmount().setScale(2, RoundingMode.HALF_UP));
            row1.put("是否封顶", safe(l.getCapApplied()));
            row1.put("停发原因", safe(l.getStopReason()));
            row1.put("停开始日期", l.getStopStartDate() == null ? "" : l.getStopStartDate().toString());
            row1.put("台账ID", l.getId());
            row1.put("创建时间", l.getCreateTime() == null ? "" : l.getCreateTime().toString());
            rows.add(row1);
        }

        appendExportAudit(actor, "ALLOWANCE_LEDGER", list.size(),
            "month=" + month + " projectId=" + projectId + " personId=" + personId);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("month", month);
        if (projectId != null) filters.put("projectId", projectId);
        if (personId != null) filters.put("personId", personId);
        filters.put("scope", scopeLabel(actor));

        return new ReportExportResult("allowance", rows.size(),
            ALLOWANCE_HEADERS, rows, filters, actor.name(), nowIso());
    }

    /**
     * P4-4.1 §2.2：奖金台账导出（AC-INC-34）。
     *
     * <p>范围：projectId 必填（奖金池按项目维度聚合）；actor 可见过滤。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportExportResult exportBonus(Long projectId,
                                          String status,
                                          IpdActor actor) {
        // BR-INC-15：资金台账导出 service 层二次校验（防注解层漂移）
        ipdPermission.requireLeaderOrAdmin();
        if (projectId == null) {
            throw new IpdBusinessException("导出奖金台账必须指定 projectId");
        }
        validateProjectVisible(projectId, actor);

        LambdaQueryWrapper<BonusPool> q = new LambdaQueryWrapper<>();
        q.eq(BonusPool::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            q.eq(BonusPool::getStatus, status);
        }
        q.orderByDesc(BonusPool::getCalculatedAt);
        List<BonusPool> list = bonusPoolMapper.selectList(q);
        if (list.size() > MAX_EXPORT_SIZE) {
            list = list.subList(0, MAX_EXPORT_SIZE);
        }

        Project pj = projectMapper.selectById(projectId);

        List<Map<String, Object>> rows = new ArrayList<>(list.size());
        for (BonusPool b : list) {
            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("奖金池ID", b.getId());
            row2.put("项目ID", b.getProjectId());
            row2.put("项目编号", pj == null ? "" : safe(pj.getCode()));
            row2.put("项目名称", pj == null ? "" : safe(pj.getName()));
            row2.put("目标销售额(元)", b.getTargetSales() == null ? BigDecimal.ZERO.setScale(2) : b.getTargetSales().setScale(2, RoundingMode.HALF_UP));
            row2.put("奖金池比率", b.getPoolRate() == null ? BigDecimal.ZERO.setScale(4) : b.getPoolRate().setScale(4, RoundingMode.HALF_UP));
            row2.put("基础池(元)", b.getBasePool() == null ? BigDecimal.ZERO.setScale(2) : b.getBasePool().setScale(2, RoundingMode.HALF_UP));
            row2.put("难度系数", b.getCoefficient() == null ? BigDecimal.ZERO.setScale(2) : b.getCoefficient().setScale(2, RoundingMode.HALF_UP));
            row2.put("达成率", b.getAchievementRate() == null ? BigDecimal.ZERO.setScale(4) : b.getAchievementRate().setScale(4, RoundingMode.HALF_UP));
            row2.put("档位系数", b.getTierCoefficient() == null ? BigDecimal.ZERO.setScale(2) : b.getTierCoefficient().setScale(2, RoundingMode.HALF_UP));
            row2.put("终池(元)", b.getFinalPool() == null ? BigDecimal.ZERO.setScale(2) : b.getFinalPool().setScale(2, RoundingMode.HALF_UP));
            row2.put("状态", safe(b.getStatus()));
            row2.put("计算时间", b.getCalculatedAt() == null ? "" : b.getCalculatedAt().toString());
            row2.put("分配时间", b.getDistributedAt() == null ? "" : b.getDistributedAt().toString());
            rows.add(row2);
        }

        appendExportAudit(actor, "BONUS_POOL", list.size(),
            "projectId=" + projectId + " status=" + status);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("projectId", projectId);
        if (status != null) filters.put("status", status);
        filters.put("scope", scopeLabel(actor));

        return new ReportExportResult("bonus", rows.size(),
            BONUS_HEADERS, rows, filters, actor.name(), nowIso());
    }

    /**
     * P4-4.1 §2.3：项目汇总导出。
     *
     * <p>范围：month 必填 + 可选 productId + actor 可见过滤。
     * <p>导出上限 MAX_EXPORT_SIZE（项目维度通常不会突破）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportExportResult exportProjectSummary(String month,
                                                   Long productId,
                                                   String keyword,
                                                   IpdActor actor) {
        validateMonth(month);

        List<Long> visibleProjectIds = resolveVisibleProjectIds(actor);
        if (visibleProjectIds != null && visibleProjectIds.isEmpty()) {
            return emptyProjectSummaryResult(month, productId, keyword, actor);
        }

        LambdaQueryWrapper<Project> pj = new LambdaQueryWrapper<>();
        if (visibleProjectIds != null) pj.in(Project::getId, visibleProjectIds);
        if (productId != null) pj.eq(Project::getProductId, productId);
        if (keyword != null && !keyword.isBlank()) {
            pj.and(w -> w.like(Project::getCode, keyword).or().like(Project::getName, keyword));
        }
        pj.orderByAsc(Project::getId);
        List<Project> projects = projectMapper.selectList(pj);
        if (projects.size() > MAX_EXPORT_SIZE) projects = projects.subList(0, MAX_EXPORT_SIZE);

        Map<Long, BigDecimal> allowanceSum = sumAllowanceByProject(month, projects);
        Map<Long, BigDecimal> bonusSum = sumBonusByProject(projects);
        Map<Long, BigDecimal> scoreAvg = avgScoreByProject(projects);

        List<Map<String, Object>> rows = new ArrayList<>(projects.size());
        for (Project p : projects) {
            BigDecimal allow = allowanceSum.getOrDefault(p.getId(), BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal bonus = bonusSum.getOrDefault(p.getId(), BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal avg = scoreAvg.get(p.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("月份", safe(month));
            row.put("项目ID", p.getId());
            row.put("项目编号", safe(p.getCode()));
            row.put("项目名称", safe(p.getName()));
            row.put("津贴终额合计(元)", allow);
            row.put("奖金终池合计(元)", bonus);
            row.put("加权绩效平均分", avg == null ? "" : avg.setScale(2, RoundingMode.HALF_UP));
            row.put("津贴行数", allowanceCountByProject(month, p.getId()));
            row.put("奖金行数", bonusCountByProject(p.getId()));
            row.put("绩效行数", scoreCountByProject(p.getId()));
            rows.add(row);
        }

        appendExportAudit(actor, "PROJECT_SUMMARY", rows.size(),
            "month=" + month + " productId=" + productId);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("month", month);
        if (productId != null) filters.put("productId", productId);
        if (keyword != null) filters.put("keyword", keyword);
        filters.put("scope", scopeLabel(actor));

        return new ReportExportResult("project", rows.size(),
            PROJECT_SUMMARY_HEADERS, rows, filters, actor.name(), nowIso());
    }

    // ========================================================================
    //  内部工具
    // ========================================================================

    /**
     * 解析 actor 可见项目集。
     * <p>SUPER_ADMIN → null（全库）
     * <p>GROUP_LEADER → 本组（用 ProjectMember.groupId/Project.mainGroupId 二者择一）
     * <p>MARKET_PM / RD_PM → 自身参与的项目（ProjectMember）
     */
    private List<Long> resolveVisibleProjectIds(IpdActor actor) {
        if (actor == null) return List.of();
        if ("SUPER_ADMIN".equals(actor.role())) return null;
        if ("GROUP_LEADER".equals(actor.role())) {
            // 本组范围：直接查 ProjectMember 中 person.groupId 匹配的所有 project
            // 简化：查同组人员的所有 project 成员记录，再取 distinct projectId
            LambdaQueryWrapper<Person> pp = new LambdaQueryWrapper<>();
            pp.eq(Person::getGroupId, actor.groupId());
            List<Person> sameGroup = personMapper.selectList(pp);
            Set<Long> pids = sameGroup.stream()
                .map(Person::getId)
                .flatMap(pid -> projectMemberMapper.selectList(
                    new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getPersonId, pid)
                ).stream())
                .map(ProjectMember::getProjectId)
                .collect(Collectors.toSet());
            return new ArrayList<>(pids);
        }
        // MARKET_PM / RD_PM：自身参与的项目
        LambdaQueryWrapper<ProjectMember> pm = new LambdaQueryWrapper<>();
        pm.eq(ProjectMember::getPersonId, actor.id());
        List<ProjectMember> members = projectMemberMapper.selectList(pm);
        return members.stream().map(ProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    private void validateProjectVisible(Long projectId, IpdActor actor) {
        if ("SUPER_ADMIN".equals(actor.role())) return;
        List<Long> visible = resolveVisibleProjectIds(actor);
        if (visible == null || !visible.contains(projectId)) {
            throw new IpdBusinessException("项目不在当前操作人可见范围内");
        }
    }

    private Map<Long, BigDecimal> sumAllowanceByProject(String month, List<Project> projects) {
        if (projects.isEmpty()) return Map.of();
        List<Long> pids = projects.stream().map(Project::getId).collect(Collectors.toList());
        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, month).in(AllowanceLedger::getProjectId, pids);
        List<AllowanceLedger> all = allowanceLedgerMapper.selectList(q);
        // 不重复计数：自然键 (personId, projectId, month) 已由表设计保证；SUM(finalAmount) 直接聚合
        Map<Long, BigDecimal> out = new HashMap<>();
        for (AllowanceLedger l : all) {
            out.merge(l.getProjectId(),
                l.getFinalAmount() == null ? BigDecimal.ZERO : l.getFinalAmount(),
                BigDecimal::add);
        }
        return out;
    }

    private int allowanceCountByProject(String month, Long projectId) {
        LambdaQueryWrapper<AllowanceLedger> q = new LambdaQueryWrapper<>();
        q.eq(AllowanceLedger::getMonth, month).eq(AllowanceLedger::getProjectId, projectId);
        return Math.toIntExact(allowanceLedgerMapper.selectCount(q));
    }

    private Map<Long, BigDecimal> sumBonusByProject(List<Project> projects) {
        if (projects.isEmpty()) return Map.of();
        List<Long> pids = projects.stream().map(Project::getId).collect(Collectors.toList());
        LambdaQueryWrapper<BonusPool> q = new LambdaQueryWrapper<>();
        q.in(BonusPool::getProjectId, pids);
        // 三态都计入：DRAFT / CONFIRMED / DISTRIBUTED（不重复计数）
        List<BonusPool> all = bonusPoolMapper.selectList(q);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (BonusPool b : all) {
            out.merge(b.getProjectId(),
                b.getFinalPool() == null ? BigDecimal.ZERO : b.getFinalPool(),
                BigDecimal::add);
        }
        return out;
    }

    private int bonusCountByProject(Long projectId) {
        LambdaQueryWrapper<BonusPool> q = new LambdaQueryWrapper<>();
        q.eq(BonusPool::getProjectId, projectId);
        return Math.toIntExact(bonusPoolMapper.selectCount(q));
    }

    /**
     * 平均加权得分：自然键 (projectId, personId, pmRole) 已由表设计保证去重。
     * 仅取 status='CONFIRMED'（未确认的草稿不计入）。
     */
    private Map<Long, BigDecimal> avgScoreByProject(List<Project> projects) {
        if (projects.isEmpty()) return Map.of();
        List<Long> pids = projects.stream().map(Project::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ProjectScore> q = new LambdaQueryWrapper<>();
        q.in(ProjectScore::getProjectId, pids).eq(ProjectScore::getStatus, "CONFIRMED");
        List<ProjectScore> all = projectScoreMapper.selectList(q);
        Map<Long, BigDecimal[]> sumCnt = new HashMap<>();
        for (ProjectScore s : all) {
            BigDecimal[] sc = sumCnt.computeIfAbsent(s.getProjectId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (s.getWeightedScore() != null) {
                sc[0] = sc[0].add(s.getWeightedScore());
                sc[1] = sc[1].add(BigDecimal.ONE);
            }
        }
        Map<Long, BigDecimal> out = new HashMap<>();
        sumCnt.forEach((pid, sc) -> {
            if (sc[1].compareTo(BigDecimal.ZERO) > 0) {
                out.put(pid, sc[0].divide(sc[1], 4, RoundingMode.HALF_UP));
            }
        });
        return out;
    }

    private int scoreCountByProject(Long projectId) {
        LambdaQueryWrapper<ProjectScore> q = new LambdaQueryWrapper<>();
        q.eq(ProjectScore::getProjectId, projectId).eq(ProjectScore::getStatus, "CONFIRMED");
        return Math.toIntExact(projectScoreMapper.selectCount(q));
    }

    private void validateMonth(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new IpdBusinessException("月份格式必须为 YYYY-MM（当前=" + month + "）");
        }
    }

    private void appendExportAudit(IpdActor actor, String entityType, long count, String reason) {
        try {
            AuditLog draft = AuditLog.builder()
                .operatorId(actor.id())
                .operatorName(actor.name())
                .operatorRole(actor.role())
                .action("EXPORT_REPORT")
                .entityType(entityType)
                .reason("count=" + count + " " + reason)
                .build();
            auditLogService.append(draft);
        } catch (Exception ex) {
            // 审计失败不阻断业务（独立事务）
            log.warn("导出审计失败 entityType={} count={} reason={}", entityType, count, reason, ex);
        }
    }

    private String scopeLabel(IpdActor actor) {
        if (actor == null) return "ANON";
        return switch (actor.role()) {
            case "SUPER_ADMIN" -> "GLOBAL";
            case "GROUP_LEADER" -> "GROUP";
            default -> "OWN";
        };
    }

    private ReportExportResult emptyAllowanceResult(String month, Long projectId, Long personId, IpdActor actor) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("month", month);
        if (projectId != null) filters.put("projectId", projectId);
        if (personId != null) filters.put("personId", personId);
        filters.put("scope", scopeLabel(actor));
        appendExportAudit(actor, "ALLOWANCE_LEDGER", 0,
            "month=" + month + " projectId=" + projectId + " personId=" + personId);
        return new ReportExportResult("allowance", 0, ALLOWANCE_HEADERS, List.of(),
            filters, actor.name(), nowIso());
    }

    private ReportExportResult emptyProjectSummaryResult(String month, Long productId, String keyword, IpdActor actor) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("month", month);
        if (productId != null) filters.put("productId", productId);
        if (keyword != null) filters.put("keyword", keyword);
        filters.put("scope", scopeLabel(actor));
        appendExportAudit(actor, "PROJECT_SUMMARY", 0,
            "month=" + month + " productId=" + productId);
        return new ReportExportResult("project", 0, PROJECT_SUMMARY_HEADERS, List.of(),
            filters, actor.name(), nowIso());
    }

    private static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
