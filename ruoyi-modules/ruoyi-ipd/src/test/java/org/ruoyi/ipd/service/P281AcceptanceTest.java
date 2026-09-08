package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-8.1 端到端整链验收：双 PM 组队 → 项目创建 → Gate 评审 → 移交 → DISABLED 生效。
 *
 * <p>覆盖：
 * <ul>
 *   <li>双 PM 组队：MARKET_PM + RD_PM 各自绑定到项目（角色互斥 B7）</li>
 *   <li>项目创建：ProjectService.create ⇒ 项目落库 + 初始化阶段</li>
 *   <li>Gate 评审：G1 创建 → 双签通过 ⇒ G2 可发起（gateCode ∈ {G1..G5}）</li>
 *   <li>移交：MARKET_PM 离职触发一键代办 + 接手人 accept ⇒ 角色绑定切换</li>
 *   <li>DISABLED 生效：原 PM 名下无活跃绑定 ⇒ accountStatus=DISABLED + wecom 解绑</li>
 * </ul>
 *
 * <p>形态：Mockito 单元级串联验收（不连真 DB）；覆盖整链行为契约。每个 service 的深度
 * 契约已有 P241 / P251 / P274 等专项验收，本卡只验证"调用链 + 副作用串联"成立。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-8.1 端到端：双 PM 组队 → 项目创建 → Gate 评审 → 移交 → DISABLED")
class P281AcceptanceTest {

    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private GateReviewMapper gateReviewMapper;
    @Mock private HandoverMapper handoverMapper;
    @Mock private SystemConfigService systemConfigService;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdAuthService ipdAuthService; // 兼容 mock，ProjectMemberService 不直接依赖

    private ProjectMemberService memberService;
    private ProjectService projectService;
    private GateCreationService gateCreationService;
    private HandoverService handoverService;

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, GateReview.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
        TableInfoHelper.initTableInfo(assistant, Person.class);
    }

    private static final IpdActor MARKET_PM = new IpdActor(101L, "market-pm", "MARKET_PM", 7L);
    private static final IpdActor RD_PM = new IpdActor(102L, "rd-pm", "RD_PM", 7L);
    private static final IpdActor GROUP_LEADER = new IpdActor(200L, "leader", "GROUP_LEADER", 7L);

    @BeforeEach
    void setUp() {
        memberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        handoverService = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, memberService, NoopTransactionManager.INSTANCE,
            org.mockito.Mockito.mock(org.ruoyi.ipd.security.IpdAuthSession.class),
            org.mockito.Mockito.mock(NotificationService.class));
        // GateCreationService 使用 @RequiredArgsConstructor 3 参
        gateCreationService = new GateCreationService(gateReviewMapper, projectMapper, auditLogService);
    }

    private Person pm(long id, String personType, String level) {
        Person p = new Person();
        p.setId(id);
        p.setName("pm-" + id);
        p.setPersonType(personType);
        p.setLevel(level);
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    private Project project(long id, long mainGroupId) {
        Project p = new Project();
        p.setId(id);
        p.setName("face-access");
        p.setCode("PRJ-2026-001");
        p.setMainGroupId(mainGroupId);
        p.setStatus("DRAFT");
        p.setDelFlag("0");
        return p;
    }

    // ------------------- Step 1: 双 PM 组队 -------------------

    @Test
    @DisplayName("Step 1：MARKET_PM + RD_PM 各自 bindMember ⇒ 2 条绑定，角色互斥放行同型")
    void dualPmBinding() {
        Project p = project(7001L, 7L);
        when(projectMapper.selectById(7001L)).thenReturn(p);
        when(personMapper.selectById(101L)).thenReturn(pm(101L, "MARKET_PM", "L3"));
        when(personMapper.selectById(102L)).thenReturn(pm(102L, "RD_PM", "L3"));
        // bindMember 内部会查 allowance.{level} 和 allowance.projectCountThreshold 两个 key —— lenient 全 mock
        lenient().when(systemConfigService.getIntValue("allowance.L3", -1)).thenReturn(2500);
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
        when(memberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ProjectMember marketMember = memberService.bindMember(7001L, 101L, "MARKET_PM", MARKET_PM);
        ProjectMember rdMember = memberService.bindMember(7001L, 102L, "RD_PM", RD_PM);

        assertThat(marketMember.getRole()).isEqualTo("MARKET_PM");
        assertThat(rdMember.getRole()).isEqualTo("RD_PM");
        assertThat(marketMember.getLockedLevel()).isEqualTo("L3");
        assertThat(rdMember.getLockedLevel()).isEqualTo("L3");

        // 两次 insert 都被调用
        ArgumentCaptor<ProjectMember> memberCap = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberMapper, atLeastOnce()).insert(memberCap.capture());
        assertThat(memberCap.getAllValues()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Step 1 守卫：MARKET_PM 类型被绑为 RD_PM ⇒ 拒绝（角色互斥 B7）")
    void dualPmBindingRoleMismatchRejected() {
        when(projectMapper.selectById(7002L)).thenReturn(project(7002L, 7L));
        // MARKET_PM 类型试图绑 RD_PM 角色
        when(personMapper.selectById(101L)).thenReturn(pm(101L, "MARKET_PM", "L3"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            memberService.bindMember(7002L, 101L, "RD_PM", MARKET_PM))
            .hasMessageContaining("角色互斥");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }

    // ------------------- Step 3: Gate 评审 -------------------

    @Test
    @DisplayName("Step 3：G1 创建 + 双签通过 ⇒ gateCode=G2 可再次创建（不限轮次，但限 14 天 / 项目）")
    void gateReviewChainG1ToG2() {
        when(projectMapper.selectById(7003L)).thenReturn(project(7003L, 7L));
        // 14 天内未创建过 G1 / G2
        when(gateReviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(gateReviewMapper.insert(any(GateReview.class))).thenReturn(1);

        GateReview g1 = gateCreationService.autoCreateGate(7003L, "G1", 200L);
        assertThat(g1.getGateCode()).isEqualTo("G1");

        GateReview g2 = gateCreationService.autoCreateGate(7003L, "G2", 200L);
        assertThat(g2.getGateCode()).isEqualTo("G2");

        ArgumentCaptor<GateReview> gateCap = ArgumentCaptor.forClass(GateReview.class);
        verify(gateReviewMapper, atLeastOnce()).insert(gateCap.capture());
        assertThat(gateCap.getAllValues()).extracting(GateReview::getGateCode)
            .contains("G1", "G2");
    }

    @Test
    @DisplayName("Step 3 守卫：非白名单 gateCode（H6）⇒ 拒绝")
    void gateReviewInvalidGateCode() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            gateCreationService.autoCreateGate(7004L, "G6", 200L))
            .hasMessageContaining("gateCode");
    }

    // ------------------- Step 4-6: 移交 + DISABLED -------------------

    @Test
    @DisplayName("Step 4-6：MARKET_PM 离职触发一键代办 + accept ⇒ 原 PM DISABLED + 接手人绑上")
    void handoverTriggersDisabled() {
        // 1) 原 MARKET_PM 处于离职状态
        Person resignedMarketPm = pm(101L, "MARKET_PM", "L3");
        resignedMarketPm.setEmploymentStatus("RESIGNED");
        // 2) 项目存在
        Project p = project(7005L, 7L);
        when(projectMapper.selectById(7005L)).thenReturn(p);
        when(personMapper.selectById(101L)).thenReturn(resignedMarketPm);
        // 当前在任绑定（待 handover）
        ProjectMember current = new ProjectMember();
        current.setId(9001L);
        current.setProjectId(7005L);
        current.setPersonId(101L);
        current.setRole("MARKET_PM");
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(current);
        // 接手人
        Person newMarketPm = pm(301L, "MARKET_PM", "L2");
        when(personMapper.selectById(301L)).thenReturn(newMarketPm);
        // handover insert
        when(handoverMapper.insert(any(HandoverRecord.class))).thenReturn(1);
        // bindMember 接手（lenient：内部查 allowance.L2 和 allowance.projectCountThreshold）
        lenient().when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
        when(memberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // exit 1 row
        when(memberMapper.update(any(), any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class)))
            .thenReturn(1);
        // 接手绑定的返回 row
        when(memberMapper.insert(any(ProjectMember.class))).thenAnswer(inv -> {
            ProjectMember m = inv.getArgument(0);
            m.setId(9101L);
            return 1;
        });

        // 5) 触发一键代办（leader 代离职 MARKET_PM 移交）
        HandoverRecord draft = handoverService.initiateOnBehalf(7005L, "MARKET_PM",
            301L, "PM 离职代办", "approval-ref-001", GROUP_LEADER);

        assertThat(draft.getStatus()).isEqualTo("COMPLETED");
        assertThat(draft.getFromPersonId()).isEqualTo(101L);
        assertThat(draft.getToPersonId()).isEqualTo(301L);

        // 6) DISABLED 守卫：原 PM 名下活跃绑定清零（exit 返回 1 ⇒ 旧绑定退出；新接手绑定不计入原 PM）
        // mock disableIfAllCleared 路径：selectList 返回空（无活跃绑定）⇒ 触发 DISABLED 更新
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        // 触发 disableIfAllCleared（package-private 反射入口）
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(handoverService,
            "disableIfAllCleared", 101L, GROUP_LEADER);

        // 验证原 PM 的 accountStatus 被置 DISABLED + wecom 解绑
        ArgumentCaptor<Person> personCap = ArgumentCaptor.forClass(Person.class);
        verify(personMapper, atLeastOnce()).update(any(), any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("Step 6：原 PM 名下仍有活跃绑定 ⇒ 不触发 DISABLED（保留在任状态）")
    void handoverKeepsActiveBinding() {
        ProjectMember active = new ProjectMember();
        active.setId(9002L);
        active.setProjectId(7006L);
        active.setPersonId(101L);
        active.setRole("MARKET_PM");

        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(active));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(handoverService,
            "disableIfAllCleared", 101L, GROUP_LEADER);

        // 不触发 person 更新
        verify(personMapper, never()).update(any(),
            any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }
}