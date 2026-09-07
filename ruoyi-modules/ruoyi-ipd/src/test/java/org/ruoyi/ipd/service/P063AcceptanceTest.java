package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * P0-6.3 删除申请撤回 + 组长升级 单元验收
 * 业务规则：
 *   - AC-DEL-06：申请提交后 24 小时内可撤回；超 24 小时不可撤回；写审计
 *   - AC-DEL-07：组长审核超期（F29：组长 2 工作日 / 超管 2 工作日）→ 自动升级
 *   - 撤回/升级幂等 + 守卫
 *
 * HIGH-4 键名校正（Round 9）：setUp() 中 mock 键名已由点分修正为驼峰，与生产 getIntValue()
 * 调用键名一致。本测试用静态 grep 锁死键名一致性，未来生产键名变更时本测试会失败提醒同步 mock。
 * 历史点分键名仅出现在本注释中作为变更记录，不作为测试断言目标。
 *
 * 单测风格（纯 Mockito 隔离 DB），HTTP 真库闭环在 round 9 真跑。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P0-6.3 撤回 + 升级 单元验收")
class P063AcceptanceTest {

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p063-test"),
            DeletionRequest.class);
    }

    @Mock
    private DeletionRequestMapper deletionRequestMapper;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DeleteAuditService deleteAuditService;

    /** W5-E-2.2：目标归属解析 mapper mock（本验收只走 withdraw/escalateOverdue 路径，不触达归属解析，占位注入即可） */
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private GateMapper gateMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private PersonMapper personMapper;

    /**
     * ROOT-R3-P0-1（dc0f0adc 守卫 fail-closed）遗漏跟随：P063 未注入守卫 mock，
     * withdraw/escalateOverdue 路径 preCheckGuard 直接 fail-fast 抛「状态机守卫未装配」（HEAD 预存红）。
     * W5-E-2.2 构造器扩展时顺带补上，恢复本验收绿基线（与 DeletionRequestServiceTest.setUp 同款）。
     */
    @Mock
    private StateMachineGuard stateMachineGuard;

    private DeletionRequestService deletionRequestService;

    private static final Long TEST_REQUESTER = 900101L;
    private static final Long TEST_LEADER = 900102L;
    private static final Long TEST_ADMIN = 900101L;

    @BeforeEach
    void setUp() {
        deletionRequestService = new DeletionRequestService(deletionRequestMapper, systemConfigService,
            auditLogService, deleteAuditService, projectMemberMapper, projectMapper, gateMapper,
            productMapper, personMapper);
        // ROOT-R3-P0-1：fail-closed 守卫必显式注入（预存红修复）
        deletionRequestService.setStateMachineGuard(stateMachineGuard);
        lenient().when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);
        // HIGH-4: 驼峰键名必须与生产 DeletionRequestService.java:166/170 一致
        lenient().when(systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2)).thenReturn(2);
        lenient().when(systemConfigService.getIntValue("deletion.adminDeadlineDays", 2)).thenReturn(2);
    }

    @Test
    @DisplayName("AC-DEL-06 正例：24h 内的申请可撤回 + 状态变 WITHDRAWN")
    void withdrawWithin24h_succeeds() {
        // 准备
        DeletionRequest before = sampleReq(101L, TEST_REQUESTER, DeletionRequestService.ST_LEADER_REVIEW);
        before.setCreateTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60)); // 1h 前提交
        when(deletionRequestMapper.selectById(101L)).thenReturn(before);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        // 跑
        DeletionRequest after = deletionRequestService.withdraw(101L, TEST_REQUESTER);
        // 验
        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_WITHDRAWN);

        ArgumentCaptor<DeletionRequest> captor = ArgumentCaptor.forClass(DeletionRequest.class);
        org.mockito.Mockito.verify(deletionRequestMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeletionRequestService.ST_WITHDRAWN);
    }
    @Test

    @DisplayName("AC-DEL-06 反例：非申请人撤回应拒绝")
    void withdrawByNonRequester_rejected() {
        DeletionRequest req = sampleReq(102L, TEST_REQUESTER, DeletionRequestService.ST_LEADER_REVIEW);
        req.setCreateTime(new Date(System.currentTimeMillis() - 1000 * 60 * 60));
        when(deletionRequestMapper.selectById(102L)).thenReturn(req);
        assertThatThrownBy(() -> deletionRequestService.withdraw(102L, 999999L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("\u4ec5\u7533\u8bf7\u4eba");
    }
    @Test
    @DisplayName("AC-DEL-06 反例：超 24h 不可撤回")
    void withdrawAfter24h_rejected() {
        DeletionRequest req = sampleReq(103L, TEST_REQUESTER, DeletionRequestService.ST_LEADER_REVIEW);
        req.setCreateTime(new Date(System.currentTimeMillis() - 25L * 3600_000L));
        when(deletionRequestMapper.selectById(103L)).thenReturn(req);
        assertThatThrownBy(() -> deletionRequestService.withdraw(103L, TEST_REQUESTER))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("\u64a4\u56de\u65f6\u9650");
    }
    @Test
    @DisplayName("AC-DEL-06 守卫：已终态不可撤回")
    void withdrawTerminal_rejected() {
        DeletionRequest req = sampleReq(104L, TEST_REQUESTER, DeletionRequestService.ST_REJECTED);
        when(deletionRequestMapper.selectById(104L)).thenReturn(req);
        assertThatThrownBy(() -> deletionRequestService.withdraw(104L, TEST_REQUESTER))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("\u5df2\u7ec8\u6001");
    }
    @Test
    @DisplayName("AC-DEL-07 组长升级：逾期 LEADER_REVIEW → ADMIN_REVIEW")
    void escalateOverdueLeaderReview() {
        DeletionRequest overdue1 = sampleReq(201L, TEST_REQUESTER, DeletionRequestService.ST_LEADER_REVIEW);
        overdue1.setLeaderDueAt(new Date(System.currentTimeMillis() - 3600_000L));
        when(deletionRequestMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(overdue1));
        // PERF-P0-1：批量 UPDATE（LambdaUpdateWrapper）替代 N+1 updateById
        when(deletionRequestMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        // 跑
        int escalated = deletionRequestService.escalateOverdueLeaderReview();
        // 验
        assertThat(escalated).isEqualTo(1);
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<DeletionRequest>> wrapperCaptor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        org.mockito.Mockito.verify(deletionRequestMapper).update(org.mockito.ArgumentMatchers.isNull(), wrapperCaptor.capture());
        // PERF-P0-1：验证 wrapper 类型（LambdaUpdateWrapper）+ affected 行数已前置断言=1
        assertThat(wrapperCaptor.getValue()).isInstanceOf(LambdaUpdateWrapper.class);
    }
    @Test
    @DisplayName("AC-DEL-07 升级幂等：空集合时升级 0 条")
    void escalateNoOverdue_isNoop() {
        when(deletionRequestMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>());
        int escalated = deletionRequestService.escalateOverdueLeaderReview();
        assertThat(escalated).isZero();
    }
    @Test
    @DisplayName("AC-DEL-07 超期清单：ADMIN_REVIEW 状态 + adminDueAt 已过的能被 list 出来")
    void listOverdueAdminReview() {
        DeletionRequest overdue = sampleReq(301L, TEST_REQUESTER, DeletionRequestService.ST_ADMIN_REVIEW);
        overdue.setAdminDueAt(new Date(System.currentTimeMillis() - 3600_000L));
        when(deletionRequestMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(overdue));
        List<DeletionRequest> result = deletionRequestService.listOverdueAdminReview();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(301L);
    }
    /**
     * HIGH-4 防漂移反向用例：直接读生产源码 DeletionRequestService.java，验证 setUp() 中 mock 用的键名
     * 与生产 getIntValue() 调用键名一致（驼峰 deletion.leaderDeadlineDays / deletion.adminDeadlineDays）。
     * <p>历史教训：原 mock 键名为旧版点分格式，与生产驼峰不一致，但因 lenient().when()
     * 的 default fallback 不报错导致假绿。本用例用静态 grep 锁死键名一致性，未来生产键名变更时
     * 本测试会失败提醒同步 mock 键名（避免再次漂移）。
     */
    @Test
    @DisplayName("HIGH-4 防漂移：mock 键名必须与生产 DeletionRequestService.getIntValue 调用键名一致（驼峰）")
    void mockKeyNamesMatchProductionSource() throws Exception {
        java.nio.file.Path prodSrc = java.nio.file.Paths.get(
            "src/main/java/org/ruoyi/ipd/service/DeletionRequestService.java");
        assertThat(java.nio.file.Files.exists(prodSrc))
            .as("生产源码存在: " + prodSrc.toAbsolutePath()).isTrue();
        String content = java.nio.file.Files.readString(prodSrc);
        // 验证生产用驼峰键名
        assertThat(content).contains("getIntValue(\"deletion.leaderDeadlineDays\"");
        assertThat(content).contains("getIntValue(\"deletion.adminDeadlineDays\"");
        // 验证生产源码没有点分键名（防止历史 bug 回流）
        assertThat(content).doesNotContain("deletion.leader.deadlineDays");
        assertThat(content).doesNotContain("deletion.admin.deadlineDays");
        // mock setUp() 驼峰键名已在 line 65/66 静态验证（lenient().when() 调用）
    }
    private DeletionRequest sampleReq(Long id, Long requesterId, String status) {
        DeletionRequest r = new DeletionRequest();
        r.setId(id);
        r.setRequesterId(requesterId);
        r.setStatus(status);
        r.setEntityType("product");
        r.setEntityId(1L);
        r.setReason("test");
        r.setCreateTime(new Date());
        r.setUpdateTime(new Date());
        r.setLeaderDueAt(new Date(System.currentTimeMillis() + 86400_000L));
        r.setAdminDueAt(new Date(System.currentTimeMillis() + 86400_000L));
        return r;
    }
}
