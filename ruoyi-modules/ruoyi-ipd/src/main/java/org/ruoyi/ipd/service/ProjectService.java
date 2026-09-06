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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final AuditLogService auditLogService;
    private final GateEngine gateEngine;
    private final ProjectBootstrapService projectBootstrapService;
    private final ProjectCertService projectCertService;
    private final PlatformTransactionManager transactionManager;

    /** 奖金池比例（BR-INC-04）：目标销售额 × 5% × 差异化系数 */
    public static final BigDecimal BONUS_POOL_RATE = new BigDecimal("0.05");
    private static final Set<String> TEMPLATE_TYPES = Set.of("HARDWARE", "SOFTWARE", "SOLUTION");
    private static final BigDecimal DEFAULT_COEF_S = new BigDecimal("1.5");
    private static final BigDecimal DEFAULT_COEF_A = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_COEF_B = new BigDecimal("0.8");
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

    /** 状态机迁移（非法迁移拒绝）；归档不可再迁出 */
    @Transactional(rollbackFor = Exception.class)
    public Project changeStatus(Long projectId, String target, Long operatorId) {
        Project project = require(projectId);
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
     *
     * @param projectId  项目
     * @param patch      含四基准字段的补丁
     * @param operatorId 操作人
     * @return 更新后项目
     */
    @Transactional(rollbackFor = Exception.class)
    public Project updateBaselines(Long projectId, Project patch, Long operatorId) {
        Project project = require(projectId);
        if (!"DRAFT".equals(project.getStatus())) {
            throw new ServiceException("四基准在立项后锁定，不可直接修改（P1-2.2）");
        }
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
        projectMapper.updateById(project);
        audit(projectId, project.getName(), operatorId, "PROJECT_BASELINE_UPDATE");
        return project;
    }

    /** 阶段推进：门禁校验（BR-IPD-06，P1-5 GateEngine 接管）+ LAUNCH 前置上市日期（BR-IPD-08） */
    @Transactional(rollbackFor = Exception.class)
    public Project advanceStage(Long projectId, Long operatorId) {
        Project project = require(projectId);
        if ("SUSPENDED".equals(project.getStatus()) || "ARCHIVED".equals(project.getStatus())) {
            throw new ServiceException("暂停/归档项目禁止推进阶段");
        }
        String next = NEXT_STAGE.get(project.getCurrentStage());
        if (next == null) {
            throw new ServiceException("已处于最终阶段 LIFECYCLE");
        }
        gateEngine.check(project, project.getCurrentStage());
        if ("LAUNCH".equals(next) && project.getLaunchDate() == null) {
            throw new ServiceException("进入 LAUNCH 前必须录入上市日期（后置指标起算原点）");
        }
        project.setCurrentStage(next);
        projectMapper.updateById(project);
        audit(projectId, project.getName(), operatorId, "PROJECT_STAGE_" + next);
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
        return projectMapper.selectList(qw.orderByDesc(Project::getId));
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

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}