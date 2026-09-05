package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
    public Project create(Project project, Long operatorId) {
        validateLevelAndCoefficient(project);
        if (project.getProductId() == null) {
            throw new ServiceException("项目必须归属产品（产品:项目 = 1:1）");
        }
        Product product = productMapper.selectById(project.getProductId());
        if (product == null || "1".equals(product.getDelFlag())) {
            throw new ServiceException("归属产品不存在: " + project.getProductId());
        }
        Long taken = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
            .eq(Project::getProductId, project.getProductId()).eq(Project::getDelFlag, "0"));
        if (taken != null && taken > 0) {
            throw new ServiceException("该产品已有关联项目（产品:项目 = 1:1）");
        }
        project.setCode(nextCode());
        project.setCurrentStage("CONCEPT");
        if (isBlank(project.getStatus())) {
            project.setStatus("DRAFT");
        }
        if (isBlank(project.getSource())) {
            project.setSource("NEW");
        }
        project.setCreateTime(new Date());
        projectMapper.insert(project);
        // 产品回填 1:1 关联
        product.setProjectId(project.getId());
        productMapper.updateById(product);
        audit(project.getId(), project.getName(), operatorId, "PROJECT_CREATE");
        return project;
    }

    /** 状态机迁移（非法迁移拒绝） */
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

    /** 阶段推进：线性下一阶段；进入 LAUNCH 前置校验上市日期已录（BR-IPD-08 起算原点） */
    @Transactional(rollbackFor = Exception.class)
    public Project advanceStage(Long projectId, Long operatorId) {
        Project project = require(projectId);
        String next = NEXT_STAGE.get(project.getCurrentStage());
        if (next == null) {
            throw new ServiceException("已处于最终阶段 LIFECYCLE");
        }
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

    /** PRJ-YYYY-NNN：取当年最大序号 +1 */
    public String nextCode() {
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

    private void validateLevelAndCoefficient(Project project) {
        String level = project.getLevel();
        if (!"S".equals(level) && !"A".equals(level) && !"B".equals(level)) {
            throw new ServiceException("项目级别非法: " + level + "（允许 S|A|B）");
        }
        BigDecimal coefficient = project.getLevelCoefficient();
        switch (level) {
            case "S" -> requireCoefficient(coefficient, "1.5", "2.0");
            case "B" -> requireCoefficient(coefficient, "0.6", "0.8");
            case "A" -> {
                if (coefficient != null) {
                    throw new ServiceException("A 级差异化系数固定语义，不接受录入");
                }
            }
            default -> throw new ServiceException("项目级别非法");
        }
        if (("S".equals(level) || "B".equals(level)) && isBlank(project.getLevelCoefficientReason())) {
            throw new ServiceException("S/B 级系数定值理由必填（写审计）");
        }
    }

    private void requireCoefficient(BigDecimal coefficient, String min, String max) {
        if (coefficient == null) {
            throw new ServiceException("该级别差异化系数必填");
        }
        if (coefficient.compareTo(new BigDecimal(min)) < 0 || coefficient.compareTo(new BigDecimal(max)) > 0) {
            throw new ServiceException("差异化系数超出区间 [" + min + ", " + max + "]: " + coefficient);
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