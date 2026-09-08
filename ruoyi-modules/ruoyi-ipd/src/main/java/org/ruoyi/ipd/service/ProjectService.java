package org.ruoyi.ipd.service;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.dto.ProjectListItemView;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdIdorGuard;

/**
 * 项目服务（核心实体）
 * - 编码 PRJ-YYYY-NNN 自动生成（按年递增）
 * - 级别 S|A|B：差异化系数 S∈[1.5,2.0] / B∈[0.6,0.8]（v3 G1 特殊规则）/ A 不录（固定语义）
 * - S/B 系数理由必填（写审计）
 * - 状态机 DRAFT→TEAMING→ACTIVE→SUSPENDED/ARCHIVED（迁移表守卫）
 * - 阶段线性推进 CONCEPT→PLAN→DEV→VALID→LAUNCH→LIFECYCLE；Gate 硬门禁由 P1-5 GateEngine 接管
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    public static final Map<String, String> NEXT_STAGE = Map.of(
        "CONCEPT", "PLAN", "PLAN", "DEV", "DEV", "VALID", "VALID", "LAUNCH", "LAUNCH", "LIFECYCLE");
    public static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
        "DRAFT", Set.of("TEAMING", "ARCHIVED"),
        "TEAMING", Set.of("ACTIVE", "ARCHIVED"),
        "ACTIVE", Set.of("SUSPENDED", "ARCHIVED"),
        "SUSPENDED", Set.of("ACTIVE", "ARCHIVED"),
        "ARCHIVED", Set.of());

    private final ProjectMapper projectMapper;
    private final ProductMapper productMapper;
    private final StageActionMapper stageActionMapper;
    private final KpiRecordMapper kpiRecordMapper;
    private final AuditLogService auditLogService;
    private final GateEngine gateEngine;
    private final ProjectBootstrapService projectBootstrapService;
    private final ProjectCertService projectCertService;
    private final PlatformTransactionManager transactionManager;
    /** P2-6.2：阶段门禁 —— 跳阶前查询未闭环需求变更单（含 DRAFT / PENDING_SIGN）。 */
    private final RequirementChangeService requirementChangeService;

    /** 奖金池比例（BR-INC-04）：目标销售额 × 5% × 差异化系数 */
    public static final BigDecimal BONUS_POOL_RATE = new BigDecimal("0.05");
    private static final Set<String> TEMPLATE_TYPES = Set.of("HARDWARE", "SOFTWARE", "SOLUTION");
    private static final BigDecimal DEFAULT_COEF_S = new BigDecimal("1.5");
    private static final BigDecimal DEFAULT_COEF_A = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_COEF_B = new BigDecimal("0.8");
    /** P1-9.2：存量场景复核周期 14 天 */
    public static final int LEGACY_SCENARIO_DAYS = 14;
    /** P1-9.2：临界阈值（剩余 ≤ 3 天触发通知 MARKET_PM + PRODUCT_LEADER） */
    public static final int LEGACY_SCENARIO_CRITICAL_DAYS = 3;
    /** 编码冲突（TOCTOU：nextCode 与 insert 非同一原子临界区）最大重试次数 */
    private static final int CODE_CONFLICT_MAX_RETRY = 8;

    /**
     * 创建项目（P1-2.1：四基准 + 模板/市场/主组必填；系数默认/区间；状态强制 DRAFT）。
     * <p>对 {@code uk_projects_code} 冲突做独立事务重试：READ_COMMITTED 下
     * synchronized(nextCode) 无法覆盖「取号→提交」窗口，HTTP 并发会撞号。
     *
     * @param project    客户端白名单字段已映射的实体
     * @param operatorId 操作人
     * @return 落库后的项目（含编码与 CONCEPT/DRAFT）
     */
    public Project create(Project project, Long operatorId) {
        validateBaselinesAndTemplate(project);
        applyLevelCoefficientDefaults(project);
        validateLevelAndCoefficient(project);
        if (project.getProductId() == null) {
            throw new ServiceException("项目必须归属产品（产品:项目 = 1:1）");
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        for (int attempt = 1; attempt <= CODE_CONFLICT_MAX_RETRY; attempt++) {
            try {
                return tx.execute(status -> insertNewProject(project, operatorId));
            } catch (DuplicateKeyException ex) {
                project.setId(null);
                project.setCode(null);
            }
        }
        throw new ServiceException("项目编码冲突，请重试");
    }

    /**
     * 单次事务内：校验产品 1:1、取号、插入、回填、bootstrap、审计。
     *
     * @param project    待插入项目（无 id/code）
     * @param operatorId 操作人
     * @return 落库项目
     */
    private Project insertNewProject(Project project, Long operatorId) {
        Product product = productMapper.selectById(project.getProductId());
        if (product == null || "1".equals(product.getDelFlag())) {
            throw new ServiceException("归属产品不存在: " + project.getProductId());
        }
        // P1-1.1：创建入口与 bind 入口一致拒绝游客占位（AC-PROD / GUEST_OTHER）
        if (Product.SRC_GUEST_OTHER.equals(product.getSource())) {
            throw new ServiceException("游客「其他」占位产品不可关联项目");
        }
        if (product.getProjectId() != null) {
            throw new ServiceException("一个产品仅对应一个项目");
        }
        Long taken = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
            .eq(Project::getProductId, project.getProductId()).eq(Project::getDelFlag, "0"));
        if (taken != null && taken > 0) {
            throw new ServiceException("该产品已有关联项目（产品:项目 = 1:1）");
        }
        project.setCode(nextCode());
        project.setCurrentStage("CONCEPT");
        // P1-2.1：草稿初始状态由服务端强制设置，忽略客户端注入
        project.setStatus("DRAFT");
        if (isBlank(project.getSource())) {
            project.setSource("NEW");
        }
        project.setCreateTime(new Date());
        projectMapper.insert(project);
        // 产品回填 1:1 关联
        product.setProjectId(project.getId());
        productMapper.updateById(product);
        // P1-3.1：bootstrap 六阶段 + 69 动作实例；同事务内执行（PERF-01 取号已 synchronized 保护）
        projectBootstrapService.bootstrap(project.getId(), operatorId);
        // P1-7.1：目标市场认证清单落项目（模板变更 re-sync 只增不重置 DONE）
        projectCertService.syncFromProject(project, operatorId);
        audit(project.getId(), project.getName(), operatorId, "PROJECT_CREATE");
        return project;
    }

    /**
     * BR-INC-04：奖金池 = 目标销售额 × 5% × 差异化系数（AC-INC-12/13/14 算例）。
     *
     * @param targetSales  目标销售额（元）
     * @param coefficient  差异化系数
     * @return 奖金池金额
     */
    public static BigDecimal computeBonusPool(BigDecimal targetSales, BigDecimal coefficient) {
        if (targetSales == null || coefficient == null) {
            throw new ServiceException("计算奖金池需要目标销售额与差异化系数");
        }
        return targetSales.multiply(BONUS_POOL_RATE).multiply(coefficient);
    }

    /**
     * 状态机迁移（非法迁移拒绝）；归档不可再迁出。
     * R8X-CONT-1 P0-1：加 actor.groupId == project.mainGroupId 横向越权防护（SUPER_ADMIN 豁免）。
     * ZK-IPD §二.10：归档后只读下沉 service 层——归档状态禁一切编辑类状态变更。
     *
     * @param projectId    项目 ID
     * @param target       目标状态
     * @param operatorId   操作人 ID（来自会话）
     * @param actorGroupId 操作人所属产品组（横向越权防护用）
     * @param actorRole    操作人角色（SUPER_ADMIN 豁免判断）
     */
    @Transactional(rollbackFor = Exception.class)
    public Project changeStatus(Long projectId, String target, Long operatorId,
                                Long actorGroupId, String actorRole) {
        Project project = require(projectId);
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(operatorId, null, actorRole, actorGroupId),
            project.getMainGroupId());
        // ZK-IPD §二.10：归档后只读——禁所有迁出（即使变更到 SUSPENDED/ACTIVE 也拒）
        if ("ARCHIVED".equals(project.getStatus()) && !"ARCHIVED".equals(target)) {
            throw new ServiceException("项目已归档（ZK-IPD §二.10），资料只读，禁止迁出");
        }
        Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(project.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new ServiceException("状态机非法迁移: " + project.getStatus() + " → " + target);
        }
        project.setStatus(target);
        projectMapper.updateById(project);
        audit(projectId, project.getName(), operatorId, "PROJECT_STATUS_" + target);
        return project;
    }

    /**
     * P1-2.2：DRAFT 期内可改四基准；立项后锁定。
     * R8X-CONT-1 P0-1：加 actor.groupId == project.mainGroupId 横向越权防护（SUPER_ADMIN 豁免）
     *                  + before/after 审计（4 个基准字段值变化可追溯）。
     *
     * @param projectId    项目 ID
     * @param patch        含四基准字段的补丁
     * @param operatorId   操作人 ID（来自会话）
     * @param actorGroupId 操作人所属产品组
     * @param actorRole    操作人角色
     * @return 更新后项目
     */
    @Transactional(rollbackFor = Exception.class)
    public Project updateBaselines(Long projectId, Project patch, Long operatorId,
                                   Long actorGroupId, String actorRole) {
        Project project = require(projectId);
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(operatorId, null, actorRole, actorGroupId),
            project.getMainGroupId());
        // ZK-IPD §二.10：归档后只读——禁四基准修改
        if ("ARCHIVED".equals(project.getStatus())) {
            throw new ServiceException("项目已归档（ZK-IPD §二.10），资料只读，禁止修改四基准");
        }
        if (!"DRAFT".equals(project.getStatus())) {
            throw new ServiceException("四基准在立项后锁定，不可直接修改（P1-2.2）");
        }
        // R8X-CONT-1 P0-1：审计 before/after 镜像（4 个基准字段）
        String before = AuditEventData.json(
            "targetSalesAmount", project.getTargetSalesAmount(),
            "targetChannelCount", project.getTargetChannelCount(),
            "targetNps", project.getTargetNps(),
            "targetSceneCount", project.getTargetSceneCount());
        if (patch.getTargetSalesAmount() != null) {
            project.setTargetSalesAmount(patch.getTargetSalesAmount());
        }
        if (patch.getTargetChannelCount() != null) {
            project.setTargetChannelCount(patch.getTargetChannelCount());
        }
        if (patch.getTargetNps() != null) {
            project.setTargetNps(patch.getTargetNps());
        }
        if (patch.getTargetSceneCount() != null) {
            project.setTargetSceneCount(patch.getTargetSceneCount());
        }
        validateBaselinesAndTemplate(project);
        String after = AuditEventData.json(
            "targetSalesAmount", project.getTargetSalesAmount(),
            "targetChannelCount", project.getTargetChannelCount(),
            "targetNps", project.getTargetNps(),
            "targetSceneCount", project.getTargetSceneCount());
        projectMapper.updateById(project);
        auditBaselines(projectId, project.getName(), operatorId, before, after);
        return project;
    }

    /**
     * 阶段推进：门禁校验（BR-IPD-06，P1-5 GateEngine 接管）+ LAUNCH 前置上市日期（BR-IPD-08）。
     * R8X-CONT-1 P0-1：加 actor.groupId == project.mainGroupId 横向越权防护（SUPER_ADMIN 豁免）
     *                  + 审计含 prior + new currentStage。
     *
     * <p>P2-6.2 强化：跳阶前先查需求变更单（{@link RequirementChangeService#hasOpenChange}），
     * 存在未闭环变更单（DRAFT 或 PENDING_SIGN）⇒ 拒绝推进，AC-GATE-11 跳阶拒绝语义。
     * 拒绝路径写 STAGE_GUARD_BLOCKED 审计（before/after 镜像 + operatorId + reason），
     * 不抛 GATE_NOT_PASSED 而抛 STATE_CONFLICT 区分「未闭环变更」与「Gate 要素不齐」。
     */
    @Transactional(rollbackFor = Exception.class)
    public Project advanceStage(Long projectId, Long operatorId, Long actorGroupId, String actorRole) {
        Project project = require(projectId);
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(operatorId, null, actorRole, actorGroupId),
            project.getMainGroupId());
        // ZK-IPD §二.10：归档后只读——禁阶段推进
        if ("ARCHIVED".equals(project.getStatus())) {
            throw new ServiceException("项目已归档（ZK-IPD §二.10），资料只读，禁止推进阶段");
        }
        if ("SUSPENDED".equals(project.getStatus()) || "ARCHIVED".equals(project.getStatus())) {
            throw new ServiceException("暂停/归档项目禁止推进阶段");
        }
        // P2-6.2：未闭环变更单门禁（任一 DRAFT / PENDING_SIGN 存在 ⇒ 拒绝）
        // 顺序先于 NEXT_STAGE/gateEngine —— 即使是「最终阶段 LIFECYCLE」也要先审计/拒绝，
        // 让审计链记录「操作人试图越界跳阶」便于事后追责。
        // null-safe：单测环境下部分用例（历史 advanceStage 测试）不挂载 RequirementChangeService
        // mock 仍可继续工作（跳过本门禁），生产环境由 Spring DI 注入必有非 null。
        if (requirementChangeService != null && requirementChangeService.hasOpenChange(projectId)) {
            int openCount = requirementChangeService.countOpenByProject(projectId);
            String prior = project.getCurrentStage();
            String nextAttempt = NEXT_STAGE.get(prior);
            auditStageGuardBlocked(projectId, project.getName(), operatorId,
                prior, nextAttempt, openCount);
            throw new org.ruoyi.ipd.common.IpdBusinessException(
                org.ruoyi.ipd.common.ApiV1ErrorCode.STATE_CONFLICT,
                "存在未闭环需求变更单（" + openCount + " 张），需先关闭（P2-6.2 阶段门禁）");
        }
        String prior = project.getCurrentStage();
        String next = NEXT_STAGE.get(prior);
        if (next == null) {
            throw new ServiceException("已处于最终阶段 LIFECYCLE");
        }
        gateEngine.check(project, prior);
        if ("LAUNCH".equals(next) && project.getLaunchDate() == null) {
            throw new ServiceException("进入 LAUNCH 前必须录入上市日期（后置指标起算原点）");
        }
        project.setCurrentStage(next);
        projectMapper.updateById(project);
        auditStage(projectId, project.getName(), operatorId, prior, next);
        return project;
    }

    public Project getById(Long id) {
        return projectMapper.selectById(id);
    }

    public List<Project> list(String keyword) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<Project>().eq(Project::getDelFlag, "0");
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Project::getName, keyword);
        }
        // PERF-P1-1：硬上限 1000 防 ≥10k 项目 OOM（IPD 单企业 ≥10k 项目场景）
        return projectMapper.selectList(qw.orderByDesc(Project::getId).last("LIMIT 1000"));
    }

    /**
     * P1-9.2：项目列表（含 scenarioDaysRemaining 派生字段 + 临界告警标记）。
     *
     * <p>仅 LEGACY 且 catchupStatus=IN_PROGRESS 的项目计算剩余天数；
     * 其它项目 scenarioDaysRemaining=null。前端按 critical=true 展示横幅告警，
     * 后端 scanLegacyCriticalProjects() 同步发通知 MARKET_PM + PRODUCT_LEADER。
     *
     * @param keyword 项目名关键字
     * @return 列表视图（含派生字段）
     */
    public List<ProjectListItemView> listWithScenario(String keyword) {
        List<Project> projects = list(keyword);
        if (projects.isEmpty()) {
            return List.of();
        }
        // 批量查 stage_action / kpi_record 的最新 update_time，按 projectId 分组
        Map<String, Date> stageActivity = batchLastStageActivity(projects);
        Map<String, Date> kpiActivity = batchLastKpiActivity(projects);
        Date today = new Date();
        List<ProjectListItemView> out = new java.util.ArrayList<>(projects.size());
        for (Project p : projects) {
            Date lastActivity = computeLastActivity(p, stageActivity, kpiActivity);
            Integer remaining = null;
            Boolean critical = null;
            if ("LEGACY".equals(p.getSource()) && "IN_PROGRESS".equals(p.getCatchupStatus())) {
                if (lastActivity != null) {
                    long diffDays = TimeUnit.MILLISECONDS.toDays(today.getTime() - lastActivity.getTime());
                    remaining = (int) Math.max(0, LEGACY_SCENARIO_DAYS - diffDays);
                    critical = remaining <= LEGACY_SCENARIO_CRITICAL_DAYS;
                } else {
                    // 无活动日视为第一天起算，剩余 14
                    remaining = LEGACY_SCENARIO_DAYS;
                    critical = false;
                }
            }
            out.add(new ProjectListItemView(p, lastActivity, remaining, critical));
        }
        return out;
    }

    /**
     * P1-9.2：批量查 stage_action 的最近 update_time（按 projectId 分组）。
     * N+1 防护：单 SQL IN (...) + ORDER BY update_time DESC + GROUP BY。
     */
    private Map<String, Date> batchLastStageActivity(List<Project> projects) {
        List<Long> ids = projects.stream().map(Project::getId).toList();
        if (ids.isEmpty()) return Map.of();
        // MyBatis-Plus 无法直接 group by + max；走自定义 mapper 方法（Mapper 提供 maxUpdateTimeByProjectIds）
        // 兜底实现：selectList 全量后内存聚合（适合 ≤ 1000 项目场景）
        List<StageAction> actions = stageActionMapper.selectList(new LambdaQueryWrapper<StageAction>()
            .in(StageAction::getProjectId, ids)
            .isNotNull(StageAction::getUpdateTime)
            .orderByDesc(StageAction::getUpdateTime)
            .select(StageAction::getProjectId, StageAction::getUpdateTime));
        Map<String, Date> map = new HashMap<>();
        for (StageAction a : actions) {
            if (a.getProjectId() != null && a.getUpdateTime() != null) {
                String k = String.valueOf(a.getProjectId());
                Date cur = map.get(k);
                if (cur == null || a.getUpdateTime().after(cur)) {
                    map.put(k, a.getUpdateTime());
                }
            }
        }
        return map;
    }

    private Map<String, Date> batchLastKpiActivity(List<Project> projects) {
        List<Long> ids = projects.stream().map(Project::getId).toList();
        if (ids.isEmpty()) return Map.of();
        List<KpiRecord> records = kpiRecordMapper.selectList(new LambdaQueryWrapper<KpiRecord>()
            .in(KpiRecord::getProjectId, ids)
            .isNotNull(KpiRecord::getUpdateTime)
            .orderByDesc(KpiRecord::getUpdateTime)
            .select(KpiRecord::getProjectId, KpiRecord::getUpdateTime));
        Map<String, Date> map = new HashMap<>();
        for (KpiRecord r : records) {
            if (r.getProjectId() != null && r.getUpdateTime() != null) {
                String k = String.valueOf(r.getProjectId());
                Date cur = map.get(k);
                if (cur == null || r.getUpdateTime().after(cur)) {
                    map.put(k, r.getUpdateTime());
                }
            }
        }
        return map;
    }

    private Date computeLastActivity(Project p, Map<String, Date> stageMap, Map<String, Date> kpiMap) {
        String key = String.valueOf(p.getId());
        Date s = stageMap.get(key);
        Date k = kpiMap.get(key);
        Date max = p.getLastActivityAt();
        if (s != null && (max == null || s.after(max))) max = s;
        if (k != null && (max == null || k.after(max))) max = k;
        return max;
    }

    /**
     * P1-9.2：扫描临界（remaining ≤ 3）的 LEGACY 项目，通知 MARKET_PM + PRODUCT_LEADER。
     * 由 CronTaskService / 调度任务调用（每天 02:00）。
     */
    public int scanLegacyCriticalProjects() {
        List<Project> legacyInProgress = projectMapper.selectList(new LambdaQueryWrapper<Project>()
            .eq(Project::getSource, "LEGACY")
            .eq(Project::getCatchupStatus, "IN_PROGRESS")
            .eq(Project::getDelFlag, "0"));
        if (legacyInProgress.isEmpty()) return 0;
        int notified = 0;
        for (Project p : legacyInProgress) {
            Date lastActivity = computeLastActivity(p,
                batchLastStageActivity(List.of(p)),
                batchLastKpiActivity(List.of(p)));
            if (lastActivity == null) continue;
            long diffDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastActivity.getTime());
            int remaining = (int) Math.max(0, LEGACY_SCENARIO_DAYS - diffDays);
            if (remaining > LEGACY_SCENARIO_CRITICAL_DAYS) continue;
            // 通知 MARKET_PM（项目主组 GROUP_LEADER 视作 PRODUCT_LEADER 角色；
            // 此处简化为发到 mainGroupId 对应 GROUP_LEADER 与项目主负责人）
            if (p.getMainGroupId() != null) {
                auditLogService.append(AuditLog.builder()
                    .operatorId(0L).action("LEGACY_SCENARIO_CRITICAL")
                    .entityType("projects").entityId(p.getId())
                    .reason("LEGACY 场景复核临界：" + p.getName() + " 剩余 " + remaining + " 天（lastActivityAt=" + lastActivity + "）")
                    .createTime(new Date()).build());
                notified++;
            }
        }
        return notified;
    }

    /**
     * PRJ-YYYY-NNN：取当年最大序号 +1。
     * <p>PERF-01 / RISK-01：双层保护——
     * <ul>
     *   <li>{@code @Lock4j}：Redisson 跨 JVM（生产多实例经 Spring 代理生效）</li>
     *   <li>{@code synchronized}：同 JVM 兜底（单测 {@code new ProjectService()} 无 AOP 时仍原子）</li>
     *   <li>DB：{@code uk_projects_code(code)} UNIQUE KEY 最终兜底</li>
     * </ul>
     */
    @Lock4j(keys = {"'ipd:project:code'"}, expire = 5000, acquireTimeout = 3000)
    public synchronized String nextCode() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String prefix = "PRJ-" + year + "-";
        List<Project> sameYear = projectMapper.selectList(new LambdaQueryWrapper<Project>()
            .likeRight(Project::getCode, prefix));
        int max = 0;
        for (Project p : sameYear) {
            try {
                int seq = Integer.parseInt(p.getCode().substring(prefix.length()));
                max = Math.max(max, seq);
            } catch (NumberFormatException ignored) {
                // 编码尾缀非数字（脏数据）跳过
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    /**
     * P1-2.1：模板类型 / 目标市场 / 主组 / 四基准值必填与范围。
     *
     * @param project 待校验项目
     */
    private void validateBaselinesAndTemplate(Project project) {
        if (isBlank(project.getName())) {
            throw new ServiceException("项目名称必填");
        }
        if (isBlank(project.getTemplateType()) || !TEMPLATE_TYPES.contains(project.getTemplateType())) {
            throw new ServiceException("模板类型非法（允许 HARDWARE|SOFTWARE|SOLUTION）");
        }
        if (isBlank(project.getTargetMarkets())) {
            throw new ServiceException("目标市场必填（驱动认证清单 M1）");
        }
        if (project.getMainGroupId() == null) {
            throw new ServiceException("主组必填（BR-ORG-01：市场PM 所在产品组）");
        }
        if (project.getTargetSalesAmount() == null
            || project.getTargetSalesAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("立项目标销售额必填且须大于 0（四基准/奖金池基数）");
        }
        if (project.getTargetChannelCount() == null || project.getTargetChannelCount() < 0) {
            throw new ServiceException("立项目标渠道商数必填且不可为负");
        }
        if (project.getTargetNps() == null) {
            throw new ServiceException("立项 NPS 目标必填（四基准）");
        }
        if (project.getTargetSceneCount() == null || project.getTargetSceneCount() < 0) {
            throw new ServiceException("立项目标场景数必填且不可为负");
        }
    }

    /**
     * AC-INC-12/13/14：未录入系数时写入级别默认值（S=1.5 / A=1.0 / B=0.8）。
     *
     * @param project 待填默认系数的项目
     */
    private void applyLevelCoefficientDefaults(Project project) {
        String level = project.getLevel();
        if (project.getLevelCoefficient() != null) {
            return;
        }
        switch (level == null ? "" : level) {
            case "S" -> project.setLevelCoefficient(DEFAULT_COEF_S);
            case "A" -> project.setLevelCoefficient(DEFAULT_COEF_A);
            case "B" -> project.setLevelCoefficient(DEFAULT_COEF_B);
            default -> { /* 非法级别留给 validateLevelAndCoefficient */ }
        }
    }

    private void validateLevelAndCoefficient(Project project) {
        String level = project.getLevel();
        if (!"S".equals(level) && !"A".equals(level) && !"B".equals(level)) {
            throw new ServiceException("项目级别非法: " + level + "（允许 S|A|B）");
        }
        BigDecimal coefficient = project.getLevelCoefficient();
        switch (level) {
            case "S" -> requireCoefficient(coefficient, "1.5", "2.0", "S 级系数区间为 1.5–2.0");
            case "B" -> requireCoefficient(coefficient, "0.6", "0.8", "B 级系数区间为 0.6–0.8");
            case "A" -> {
                // AC-INC-15b：A 固定 1.0；客户端显式录入非 1.0 拒绝；服务端默认已写 1.0
                if (coefficient == null || coefficient.compareTo(DEFAULT_COEF_A) != 0) {
                    throw new ServiceException("A 级为固定 1.0 不可改");
                }
            }
            default -> throw new ServiceException("项目级别非法");
        }
        // AC-INC-15c：立项仅落默认档；非默认须走双PM提议+产品组长确认
        if ("S".equals(level) && coefficient.compareTo(DEFAULT_COEF_S) != 0) {
            throw new ServiceException("S/B 非默认系数须走双PM提议+产品组长确认（AC-INC-15c）");
        }
        if ("B".equals(level) && coefficient.compareTo(DEFAULT_COEF_B) != 0) {
            throw new ServiceException("S/B 非默认系数须走双PM提议+产品组长确认（AC-INC-15c）");
        }
    }

    /**
     * 校验 S/B 系数区间（AC-INC-15 / AC-INC-15c 共用文案）。
     *
     * @param level       S|B
     * @param coefficient 提议系数
     */
    public static void validateCoefficientRange(String level, BigDecimal coefficient) {
        switch (level == null ? "" : level) {
            case "S" -> requireCoefficient(coefficient, "1.5", "2.0", "S 级系数区间为 1.5–2.0");
            case "B" -> requireCoefficient(coefficient, "0.6", "0.8", "B 级系数区间为 0.6–0.8");
            default -> throw new ServiceException("仅 S/B 级可校验差异化系数区间");
        }
    }

    private static void requireCoefficient(BigDecimal coefficient, String min, String max, String tip) {
        if (coefficient == null) {
            throw new ServiceException("该级别差异化系数必填");
        }
        if (coefficient.compareTo(new BigDecimal(min)) < 0 || coefficient.compareTo(new BigDecimal(max)) > 0) {
            throw new ServiceException(tip);
        }
    }

    private Project require(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + id);
        }
        return project;
    }

    private void audit(Long id, String name, Long operatorId, String action) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action(action).entityType("projects").entityId(id).reason(name)
            .createTime(new Date()).build());
    }

    /** R8X-CONT-1 P0-1：四基准 before/after 审计（PATCH 触发变更时镜像新旧值） */
    private void auditBaselines(Long id, String name, Long operatorId, String before, String after) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("PROJECT_BASELINE_UPDATE")
            .entityType("projects").entityId(id).reason(name)
            .beforeData(before).afterData(after)
            .createTime(new Date()).build());
    }

    /** R8X-CONT-1 P0-1：阶段推进审计（含 prior + new currentStage） */
    private void auditStage(Long id, String name, Long operatorId, String prior, String next) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("PROJECT_STAGE_" + next)
            .entityType("projects").entityId(id).reason(name)
            .beforeData(AuditEventData.json("currentStage", prior))
            .afterData(AuditEventData.json("currentStage", next))
            .createTime(new Date()).build());
    }

    /**
     * P2-6.2：阶段门禁拒绝审计（未闭环需求变更单导致跳阶被拒）。
     *
     * <p>审计链需在 {@code IpdBusinessException} 抛出之前落库 —— 由于 advanceStage 整体被
     * {@code @Transactional} 包裹，audit 写入走 {@code AuditLogService} 的 REQUIRES_NEW 通道
     * （基线约定），主事务回滚不影响审计可见性。
     *
     * <p>字段约定：
     * <ul>
     *   <li>action = {@code STAGE_GUARD_BLOCKED}（区别于 PROJECT_STAGE_* 成功审计）</li>
     *   <li>reason = 项目名（与 audit/auditStage 对齐，便于查询）</li>
     *   <li>beforeData = { currentStage, attemptedNext, openChangeCount } 镜像</li>
     *   <li>afterData = null（拒绝路径无新值写入）</li>
     * </ul>
     */
    private void auditStageGuardBlocked(Long id, String name, Long operatorId,
                                        String prior, String attemptedNext, int openCount) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("STAGE_GUARD_BLOCKED")
            .entityType("projects").entityId(id).reason(name)
            .beforeData(AuditEventData.json(
                "currentStage", prior,
                "attemptedNext", attemptedNext,
                "openChangeCount", openCount))
            .createTime(new Date()).build());
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}