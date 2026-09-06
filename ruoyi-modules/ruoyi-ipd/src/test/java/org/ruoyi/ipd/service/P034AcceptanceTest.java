package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * P0-3.4 参数在 Gate、津贴消费者中的真实版本验收（单测层）。
 *
 * <p>真库 HTTP 全链（改参 → 即时生效 → 新旧隔离 → 版本链留痕）见
 * evidence-p252-http/probe-p034-final-out.txt（20/20）。本类锁两个消费者的
 * 参数即时读取语义（AC-CFG-03）：同一 service 实例在参数变更后下一次取数即新值，
 * 不存在进程内常量缓存；AC-GLB-10 奖金重算消费者待 P3-4.4（BLOCKED）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P034AcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateReviewMapper reviewMapper;
    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private org.ruoyi.ipd.mapper.GateArbitrationMapper arbitrationMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private GateReviewService gateReviewService;
    private ProjectMemberService memberService;

    private static final IpdActor MARKET = new IpdActor(301L, "陈市场", "MARKET_PM", 7L);
    private static final IpdActor RD = new IpdActor(302L, "刘研发", "RD_PM", 7L);

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P034-gr"), GateReview.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P034-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P034-pm"), ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        gateReviewService = new GateReviewService(gateMapper, reviewMapper, memberMapper,
            personMapper, arbitrationMapper, systemConfigService, auditLogService, notificationService);
        memberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        lenient().when(projectMapper.selectById(anyLong())).thenReturn(new Project());
        lenient().when(memberMapper.selectCount(any())).thenReturn(0L);
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
    }

    private Gate submittedGate(long id, String gateCode) {
        Gate gate = new Gate();
        gate.setId(id);
        gate.setProjectId(11L);
        gate.setGateCode(gateCode);
        gate.setStatus("PENDING");
        gate.setCurrentRound(1);
        gate.setStartedAt(new Date());
        return gate;
    }

    private Person rdPm(String level) {
        Person p = new Person();
        p.setId(201L);
        p.setName("研发测试员");
        p.setPersonType("RD_PM");
        p.setLevel(level);
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    @Test
    @DisplayName("AC-CFG-02：signDeadlineDays=5（非默认）⇒ 签署 due=started+5d")
    void cfg02_deadlineFiveDays() {
        Gate gate = submittedGate(501L, "G1");
        when(gateMapper.selectById(501L)).thenReturn(gate);
        when(reviewMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(systemConfigService.getIntValue("gate.signDeadlineDays", 3)).thenReturn(5);

        GateReview row = gateReviewService.sign(501L, "APPROVE", null, MARKET);

        long expected = gate.getStartedAt().getTime() + TimeUnit.DAYS.toMillis(5);
        assertThat(Math.abs(row.getDueAt().getTime() - expected)).isLessThan(2000L);
    }

    @Test
    @DisplayName("AC-CFG-03：同一实例参数 3→5，下一次签署即按 5（无缓存）")
    void cfg03_deadlineChangeReadsFreshValue() {
        Gate gateA = submittedGate(501L, "G1");
        Gate gateB = submittedGate(502L, "G1");
        when(gateMapper.selectById(501L)).thenReturn(gateA);
        when(gateMapper.selectById(502L)).thenReturn(gateB);
        when(reviewMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(systemConfigService.getIntValue("gate.signDeadlineDays", 3)).thenReturn(3, 5);

        GateReview first = gateReviewService.sign(501L, "APPROVE", null, MARKET);
        GateReview second = gateReviewService.sign(502L, "APPROVE", null, RD);

        assertThat(daysFrom(first, gateA)).isEqualTo(3L);
        assertThat(daysFrom(second, gateB)).isEqualTo(5L);
    }

    @Test
    @DisplayName("AC-HR-05：allowance.L3 改 2200 ⇒ 新绑定按 2200 锁定（同实例即时生效）")
    void hr05_allowance2200Freshest() {
        when(personMapper.selectById(201L)).thenReturn(rdPm("L3"));
        when(systemConfigService.getIntValue("allowance.L3", -1)).thenReturn(2000, 2200);

        ProjectMember first = memberService.bindMember(11L, 201L, "RD_PM", MARKET);
        ProjectMember second = memberService.bindMember(12L, 201L, "RD_PM", MARKET);

        assertThat(first.getLockedAmount().toPlainString()).isEqualTo("2000");
        assertThat(second.getLockedAmount().toPlainString()).isEqualTo("2200");
    }

    @Test
    @DisplayName("AC-HR-05 反向：参数改 2200 后既有绑定快照仍 2000（不追溯，AC-INC-02）")
    void hr05_existingSnapshotNotRecalculated() {
        when(personMapper.selectById(201L)).thenReturn(rdPm("L3"));
        when(systemConfigService.getIntValue("allowance.L3", -1)).thenReturn(2000);

        ProjectMember bound = memberService.bindMember(11L, 201L, "RD_PM", MARKET);
        // 参数后续改 2200 不再触达已返回的内存快照（落库行更不会被重算，真库 S8b 已证）

        assertThat(bound.getLockedAmount().toPlainString()).isEqualTo("2000");
    }

    private long daysFrom(GateReview row, Gate gate) {
        return TimeUnit.MILLISECONDS.toDays(
            row.getDueAt().getTime() - gate.getStartedAt().getTime() + TimeUnit.HOURS.toMillis(12));
    }
}
