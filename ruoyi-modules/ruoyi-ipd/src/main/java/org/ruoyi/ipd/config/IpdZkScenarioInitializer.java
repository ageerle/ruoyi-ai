package org.ruoyi.ipd.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * ZK-IPD 场景种子（G4 数据门；对齐原型 scenario.mjs + 前端 workbench 原硬编码 demo 数据）。
 *
 * <p>内容（幂等，按 employee_no/group_name/code/project+code 判存在即跳过）：
 * <ul>
 *   <li>6 产品组（市场组×3 + 研发线×3）+ 13 人（傅志谦超管 + 6 组长 + 6 PM），对齐原型 PEOPLE；</li>
 *   <li>3 个已完结项目（ENT-AC-100 / ZK-IAT-ATT / VIS-RD-100，原型 PROJECTS）：ARCHIVED + 六阶段 DONE + DONE 动作；</li>
 *   <li>2 个运行态项目（如门禁测试 / 熵基互联+智能锁联动）：ACTIVE + 未完成动作（含超期 IN_PROGRESS/DELAYED、
 *       临期 NOT_STARTED），供工作台待办/临期超期/责任过滤真实测试；</li>
 *   <li>相对日期（now±N 天）保证超期/临期场景不随时间失效。</li>
 * </ul>
 *
 * <p>密码：BCrypt（cost 10）运行时生成，来源 {@code ipd.security.initial-password}（SEC-HIGH-2，源码无字面量），
 * 首登强制改密 must_change_pwd=1。仅 dev profile。
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class IpdZkScenarioInitializer implements ApplicationRunner {

    @Value("${ipd.security.initial-password}")
    private String runtimeInitialPwd;

    private final ProductGroupMapper productGroupMapper;
    private final PersonMapper personMapper;
    private final ProductMapper productMapper;
    private final ProjectMapper projectMapper;
    private final ProjectStageMapper projectStageMapper;
    private final StageActionMapper stageActionMapper;
    private final ProjectMemberMapper projectMemberMapper;

    private static final List<String> SIX_STAGES = List.of("CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Long gMarketHw = ensureGroup("时间安全管理产品组");
        Long gMarketSw = ensureGroup("云应用产品组");
        Long gMarketIt = ensureGroup("项目集成产品组");
        Long gRdHw = ensureGroup("时间安全管理产品线");
        Long gRdSw = ensureGroup("云应用产品线");
        Long gRdIt = ensureGroup("项目集成产品线");

        Long fuZhiqian = ensurePerson("傅志谦", "GMP-001", "SUPER_ADMIN", null, "L5", "超级管理员（ZK-IPD 场景）");
        Long yangBo = ensurePerson("杨波", "MHW-L01", "GROUP_LEADER", gMarketHw, "L5", "时间安全管理产品组组长");
        Long duanJinke = ensurePerson("段进科", "MHW-P01", "MARKET_PM", gMarketHw, "L3", "市场PM（ZK-IPD 场景）");
        Long wenYuanbiao = ensurePerson("文元彪", "MSW-L01", "GROUP_LEADER", gMarketSw, "L4", "云应用产品组组长");
        Long huJiaolu = ensurePerson("胡蛟露", "MSW-P01", "MARKET_PM", gMarketSw, "L4", "市场PM（ZK-IPD 场景）");
        Long chenZepeng = ensurePerson("陈泽鹏", "MIT-L01", "GROUP_LEADER", gMarketIt, "L4", "项目集成产品组组长");
        Long chenBiqin = ensurePerson("陈必勤", "MIT-P01", "MARKET_PM", gMarketIt, "L3", "市场PM（ZK-IPD 场景）");
        Long xiaoJinglong = ensurePerson("肖敬龙", "RHW-L01", "GROUP_LEADER", gRdHw, "L5", "时间安全管理产品线组长");
        Long chengLong = ensurePerson("程龙", "RHW-P01", "RD_PM", gRdHw, "L4", "研发PM（ZK-IPD 场景）");
        ensurePerson("杨志君", "RSW-L01", "GROUP_LEADER", gRdSw, "L4", "云应用产品线组长");
        Long linLijie = ensurePerson("林立杰", "RSW-P01", "RD_PM", gRdSw, "L3", "研发PM（ZK-IPD 场景）");
        Long shangguanZhichang = ensurePerson("上官志昌", "RIT-L01", "GROUP_LEADER", gRdIt, "L4", "项目集成产品线组长");
        ensurePerson("方武略", "RIT-P01", "RD_PM", gRdIt, "L4", "研发PM（ZK-IPD 场景）");

        // ---- 3 个已完结项目（原型 PROJECTS；ARCHIVED + 全阶段 DONE + DONE 动作）----
        seedCompletedProject("ENT-AC-100", "入门级门禁产品", "HARDWARE", "A",
            new BigDecimal("8000000"), duanJinke, chengLong, gMarketHw);
        seedCompletedProject("ZK-IAT-ATT", "熵基互联考勤模块", "SOFTWARE", "S",
            new BigDecimal("12000000"), huJiaolu, linLijie, gMarketSw);
        seedCompletedProject("VIS-RD-100", "访客机＋万傲瑞达", "SOLUTION", "A",
            new BigDecimal("6000000"), chenBiqin, shangguanZhichang, gMarketIt);

        // ---- 2 个运行态项目（原 workbench 硬编码 demo；供待办/超期/责任过滤真实测试）----
        Project gateTest = seedActiveProject("ZK-GATE-TEST", "如门禁测试", "HARDWARE", "B",
            "PLAN", duanJinke, chengLong, gMarketHw, null);
        action(gateTest, "PLAN", "P01", "市场准入合规清单梳理", "MARKET_PM", "IN_PROGRESS", daysFromNow(-5));
        action(gateTest, "PLAN", "P02", "渠道商对接名单确认", "MARKET_PM", "DELAYED", daysFromNow(-10));
        action(gateTest, "PLAN", "P03", "门禁协议对接联调", "RD_PM", "NOT_STARTED", daysFromNow(14));
        action(gateTest, "PLAN", "P04", "认证送样准备", "BOTH", "NOT_STARTED", daysFromNow(30));
        action(gateTest, "CONCEPT", "C01", "概念立项评审", "BOTH", "DONE", null);
        action(gateTest, "CONCEPT", "C02", "目标市场与竞品分析", "MARKET_PM", "DONE", null);

        Project iatLock = seedActiveProject("ZK-IAT-LOCK", "熵基互联+智能锁联动", "SOFTWARE", "A",
            "DEV", huJiaolu, linLijie, gMarketSw, null);
        action(iatLock, "DEV", "D01", "智能锁通信协议评审", "RD_PM", "IN_PROGRESS", daysFromNow(7));
        action(iatLock, "DEV", "D02", "联动场景用例设计", "MARKET_PM", "NOT_STARTED", daysFromNow(10));
        action(iatLock, "DEV", "D03", "固件联调计划", "RD_PM", "NOT_STARTED", daysFromNow(-2));
        action(iatLock, "PLAN", "P01", "项目计划评审", "BOTH", "DONE", null);
        action(iatLock, "CONCEPT", "C01", "概念立项评审", "BOTH", "DONE", null);

        log.info("[IPD] ZK-IPD 场景种子完成（6组/13人/3完结项目/2运行态项目；幂等跳过已存在）");
    }

    /** 已完结项目：ARCHIVED + 六阶段 DONE + 每阶段 2 个 DONE 动作（completed 统计可测）。 */
    private void seedCompletedProject(String code, String name, String templateType, String level,
                                      BigDecimal targetSales, Long marketPmId, Long rdPmId, Long mainGroupId) {
        Project project = seedActiveProject(code, name, templateType, level, "LIFECYCLE", marketPmId, rdPmId, mainGroupId, targetSales);
        project.setStatus("ARCHIVED");
        project.setLifecycleStatus("ARCHIVED");
        // 治理豁免：ZK 场景种数据，demo 性质不入业务守卫；登记 LaunchDateDualSignGuardAcceptanceTest 白名单
        project.setLaunchDate(daysFromNow(-180));
        projectMapper.updateById(project);
        for (String stage : SIX_STAGES) {
            action(project, stage, "A01", stageNameOf(stage) + "阶段评审", "BOTH", "DONE", null);
            action(project, stage, "A02", stageNameOf(stage) + "阶段交付物归档", "BOTH", "DONE", null);
        }
    }

    /** 运行态项目：product 1:1 + 六阶段实例（当前阶段前 DONE/当前 IN_PROGRESS）+ 双PM成员。 */
    private Project seedActiveProject(String code, String name, String templateType, String level,
                                      String currentStage, Long marketPmId, Long rdPmId, Long mainGroupId,
                                      BigDecimal targetSales) {
        Project exist = projectMapper.selectOne(new LambdaQueryWrapper<Project>()
            .eq(Project::getCode, code).last("limit 1"));
        if (exist != null) {
            return exist;
        }
        Product product = ensureProduct(name, mainGroupId);
        Project project = Project.builder()
            .code(code).name(name)
            .productId(product.getId())
            .templateType(templateType)
            .level(level)
            .targetSalesAmount(targetSales != null ? targetSales : BigDecimal.ZERO)
            .currentStage(currentStage)
            .source("NEW")
            .status("ACTIVE")
            .mainGroupId(mainGroupId)
            .build();
        project.setCreateTime(new Date());
        projectMapper.insert(project);
        product.setProjectId(project.getId());
        productMapper.updateById(product);

        for (int i = 0; i < SIX_STAGES.size(); i++) {
            String stageCode = SIX_STAGES.get(i);
            int order = SIX_STAGES.indexOf(currentStage);
            String stageStatus = i < order ? "DONE" : i == order ? "IN_PROGRESS" : "NOT_STARTED";
            projectStageMapper.insert(ProjectStage.builder()
                .projectId(project.getId())
                .stageCode(stageCode)
                .stageName(stageNameOf(stageCode))
                .sortOrder(i + 1)
                .status(stageStatus)
                .build());
        }
        member(project.getId(), marketPmId, "MARKET_PM");
        member(project.getId(), rdPmId, "RD_PM");
        return project;
    }

    private void member(Long projectId, Long personId, String role) {
        Long exists = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getPersonId, personId));
        if (exists > 0) {
            return;
        }
        ProjectMember m = ProjectMember.builder()
            .projectId(projectId).personId(personId).role(role)
            .memberType("PRIMARY").joinDate(new Date()).bonusEligible("1")
            .build();
        m.setCreateTime(new Date());
        projectMemberMapper.insert(m);
    }

    /** 阶段动作（幂等：project+actionCode 已存在即跳过）。 */
    private void action(Project project, String stageCode, String actionCode, String actionName,
                        String ownerRole, String status, Date dueDate) {
        Long exists = stageActionMapper.selectCount(new LambdaQueryWrapper<StageAction>()
            .eq(StageAction::getProjectId, project.getId())
            .eq(StageAction::getActionCode, actionCode));
        if (exists > 0) {
            return;
        }
        ProjectStage stage = projectStageMapper.selectOne(new LambdaQueryWrapper<ProjectStage>()
            .eq(ProjectStage::getProjectId, project.getId())
            .eq(ProjectStage::getStageCode, stageCode).last("limit 1"));
        StageAction a = StageAction.builder()
            .projectId(project.getId())
            .stageId(stage != null ? stage.getId() : null)
            .actionCode(actionCode)
            .actionName(actionName)
            .ownerRole(ownerRole)
            .depth("DEEP")
            .status(status)
            .isBlocking("0")
            .dueDate(dueDate)
            .actualDoneAt("DONE".equals(status) ? new Date() : null)
            .build();
        a.setCreateTime(new Date());
        stageActionMapper.insert(a);
    }

    private Product ensureProduct(String productName, Long groupId) {
        Product exist = productMapper.selectOne(new LambdaQueryWrapper<Product>()
            .eq(Product::getProductName, productName).last("limit 1"));
        if (exist != null) {
            return exist;
        }
        Product fresh = Product.builder()
            .productName(productName)
            .source(Product.SRC_PM_NEW)
            .groupId(groupId)
            .status(Product.ST_ACTIVE)
            .build();
        fresh.setCreateTime(new Date());
        productMapper.insert(fresh);
        return fresh;
    }

    private Long ensureGroup(String name) {
        ProductGroup g = productGroupMapper.selectOne(new LambdaQueryWrapper<ProductGroup>()
            .eq(ProductGroup::getGroupName, name).last("limit 1"));
        if (g != null) {
            return g.getId();
        }
        ProductGroup fresh = ProductGroup.builder()
            .groupName(name).description("ZK-IPD 场景种子")
            .build();
        fresh.setCreateTime(new Date());
        productGroupMapper.insert(fresh);
        return fresh.getId();
    }

    private Long ensurePerson(String name, String employeeNo, String personType, Long groupId, String level, String remark) {
        Person exist = personMapper.selectOne(new LambdaQueryWrapper<Person>()
            .eq(Person::getEmployeeNo, employeeNo).last("limit 1"));
        if (exist != null) {
            return exist.getId();
        }
        Person p = Person.builder()
            .name(name)
            .employeeNo(employeeNo)
            .personType(personType)
            .groupId(groupId)
            .level(level)
            .levelSource("MOCK")
            .accountStatus("ACTIVE")
            .employmentStatus("ACTIVE")
            .username(name)
            // SEC-HIGH-1/2：BCrypt cost 10；密码走 ipd.security.initial-password 注入，源码无字面量
            .passwordHash(BCrypt.hashpw(runtimeInitialPwd, BCrypt.gensalt(10)))
            .mustChangePwd("1")
            .remark(remark)
            .build();
        p.setCreateTime(new Date());
        personMapper.insert(p);
        return p.getId();
    }

    private String stageNameOf(String stageCode) {
        return switch (stageCode) {
            case "CONCEPT" -> "概念";
            case "PLAN" -> "计划";
            case "DEV" -> "开发";
            case "VALID" -> "验证";
            case "LAUNCH" -> "发布";
            case "LIFECYCLE" -> "生命周期";
            default -> stageCode;
        };
    }

    private Date daysFromNow(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }
}
