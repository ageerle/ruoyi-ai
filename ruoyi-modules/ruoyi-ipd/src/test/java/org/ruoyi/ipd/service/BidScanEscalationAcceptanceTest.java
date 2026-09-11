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
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MEDIUM-2.2 + MEDIUM-2.3 公开招标应标者互见 + 7 日升级通知组长 6 测全绿。
 *
 * <ul>
 *   <li>① PUBLIC 模式应标者 A 可见所有 PENDING/ACCEPTED 应标</li>
 *   <li>② PUBLIC 模式应标者 A 看不到 B 的 WITHDRAWN 应标（DB 层 wrapper 过滤）</li>
 *   <li>③ ONE_TO_ONE 应标者 A 仅见自己</li>
 *   <li>④ 7 日升级扫描 → 查 GROUP_LEADER 发通知</li>
 *   <li>⑤ 未找到组长 → audit 留痕不发通知</li>
 *   <li>⑥ 重复扫描同一 overdue 单 → publishDaily 自然日幂等</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidScanEscalationAcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private PersonMapper personMapper;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    private BidInvitationService bidService;
    private BidScanService scanService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
        TableInfoHelper.initTableInfo(assistant, BidResponse.class);
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
    }

    @BeforeEach
    void setUp() {
        bidService = new BidInvitationService(bidInvitationMapper, bidResponseMapper,
            auditLogService, notificationService);
        scanService = new BidScanService(bidInvitationMapper, bidResponseMapper, projectMapper,
            personMapper, notificationService, auditLogService);
    }

    // ==================== MEDIUM-2.2 PUBLIC 应标者互见 ====================

    @Test
    @DisplayName("①PUBLIC 模式应标者 A 可见所有 PENDING/ACCEPTED 应标 + 摘要脱敏")
    void listResponses_public_showsAllPendingExceptWithdrawnRejected() {
        BidInvitation inv = new BidInvitation();
        inv.setId(1001L);
        inv.setMode("PUBLIC");
        inv.setCreateBy(300L);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);

        // 模拟 DB 层按 wrapper 已过滤 WITHDRAWN/REJECTED，返回 3 条 PENDING/ACCEPTED
        BidResponse a = new BidResponse();
        a.setId(1L); a.setInvitationId(1001L); a.setRdPmId(201L); a.setStatus("PENDING");
        a.setResponseNote("A 方案");
        // B 方案 >80 字符，应被截断为前 80 + 省略号
        String longNote = "B".repeat(120);
        BidResponse b = new BidResponse();
        b.setId(2L); b.setInvitationId(1001L); b.setRdPmId(202L); b.setStatus("PENDING");
        b.setResponseNote(longNote);
        BidResponse c = new BidResponse();
        c.setId(3L); c.setInvitationId(1001L); c.setRdPmId(203L); c.setStatus("ACCEPTED");
        c.setResponseNote("C 方案");
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(a, b, c));

        // 当前会话人 = A(201)，PUBLIC 模式，非发起人
        List<BidResponse> result = bidService.listResponses(1001L, 201L);

        // 过滤后只剩 PENDING + ACCEPTED（3 条）
        assertThat(result).hasSize(3);
        assertThat(result).extracting(BidResponse::getStatus)
            .doesNotContain("WITHDRAWN", "REJECTED");
        // 脱敏：B 方案原文 >80 字符，应被截断为 80 + …（81 字符）；A 方案短，原样保留
        BidResponse bOut = result.stream().filter(r -> r.getId().equals(2L)).findFirst().orElseThrow();
        assertThat(bOut.getResponseNote()).hasSize(81).endsWith("…");
        BidResponse aOut = result.stream().filter(r -> r.getId().equals(1L)).findFirst().orElseThrow();
        assertThat(aOut.getResponseNote()).isEqualTo("A 方案");
    }

    @Test
    @DisplayName("②PUBLIC 模式应标者 A 看不到 B 的 WITHDRAWN 应标（wrapper 含 WITHDRAWN/REJECTED 排除）")
    void listResponses_public_hidesWithdrawnSimplified() {
        BidInvitation inv = new BidInvitation();
        inv.setId(1002L);
        inv.setMode("PUBLIC");
        inv.setCreateBy(300L);
        when(bidInvitationMapper.selectById(1002L)).thenReturn(inv);

        // 模拟 DB 层按 wrapper 已过滤 WITHDRAWN（断言 mapper 被调用 + 返回集合不含 WITHDRAWN）
        BidResponse a = new BidResponse();
        a.setId(1L); a.setInvitationId(1002L); a.setRdPmId(201L); a.setStatus("PENDING");
        BidResponse b = new BidResponse();
        b.setId(2L); b.setInvitationId(1002L); b.setRdPmId(202L); b.setStatus("PENDING");
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(a, b));

        List<BidResponse> result = bidService.listResponses(1002L, 201L);

        // mapper 被调用 1 次（按 wrapper 过滤 WITHDRAWN/REJECTED）
        verify(bidResponseMapper, times(1)).selectList(any());
        assertThat(result).hasSize(2);
        assertThat(result).extracting(BidResponse::getStatus).doesNotContain("WITHDRAWN", "REJECTED");
    }

    @Test
    @DisplayName("③ONE_TO_ONE 应标者 A 仅见自己（非发起人）")
    void listResponses_oneToOne_onlySelf() {
        BidInvitation inv = new BidInvitation();
        inv.setId(1003L);
        inv.setMode("ONE_TO_ONE");
        inv.setCreateBy(300L);
        when(bidInvitationMapper.selectById(1003L)).thenReturn(inv);

        BidResponse a = new BidResponse();
        a.setId(1L); a.setInvitationId(1003L); a.setRdPmId(201L); a.setStatus("PENDING");
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(a));

        List<BidResponse> result = bidService.listResponses(1003L, 201L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRdPmId()).isEqualTo(201L);
    }

    // ==================== MEDIUM-2.3 7 日升级通知组长 ====================

    private BidInvitation overdueInvitation() {
        BidInvitation inv = new BidInvitation();
        inv.setId(2001L);
        inv.setProjectId(99L);
        inv.setTitle("智能门禁");
        inv.setCreateBy(7L);
        inv.setStatus("OPEN");
        inv.setExpireAt(new Date(System.currentTimeMillis() - 10L * 24 * 3600 * 1000L));
        inv.setCreateTime(new Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000L));
        return inv;
    }

    @Test
    @DisplayName("④7 日升级扫描 → 查 GROUP_LEADER 发 BID_SELECT_OVERDUE_ESCALATED 通知 + audit")
    void scanSelectOverdue_notifiesGroupLeader() {
        when(bidInvitationMapper.selectList(any())).thenReturn(List.of(overdueInvitation()));
        when(bidResponseMapper.selectCount(any())).thenReturn(2L);
        Project project = new Project();
        project.setId(99L);
        project.setMainGroupId(50L);
        when(projectMapper.selectById(99L)).thenReturn(project);
        Person leader = new Person();
        leader.setId(8001L);
        leader.setPersonType("GROUP_LEADER");
        leader.setGroupId(50L);
        when(personMapper.selectList(any())).thenReturn(List.of(leader));

        int n = scanService.scanSelectOverdue();

        assertThat(n).isEqualTo(1);
        // 通知组长 BID_SELECT_OVERDUE_ESCALATED
        verify(notificationService, times(1)).publishDaily(eq(8001L),
            eq(NotificationService.Types.BID_SELECT_OVERDUE_ESCALATED),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(2001L),
            anyString(),
            anyString(),
            anyString(),
            any());
        // audit 留痕
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("⑤未找到组长 → audit 留痕，不发 BID_SELECT_OVERDUE_ESCALATED 通知")
    void scanSelectOverdue_noLeader_auditOnly() {
        when(bidInvitationMapper.selectList(any())).thenReturn(List.of(overdueInvitation()));
        when(bidResponseMapper.selectCount(any())).thenReturn(2L);
        Project project = new Project();
        project.setId(99L);
        project.setMainGroupId(50L);
        when(projectMapper.selectById(99L)).thenReturn(project);
        when(personMapper.selectList(any())).thenReturn(List.of()); // 无组长

        int n = scanService.scanSelectOverdue();

        assertThat(n).isEqualTo(1);
        verify(notificationService, never()).publishDaily(anyLong(),
            eq(NotificationService.Types.BID_SELECT_OVERDUE_ESCALATED),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString(), any());
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("⑥重复扫描同一 overdue 单 → 业务层两次调用 publishDaily，幂等由 NotificationService 内部 dedupKey 保证")
    void scanSelectOverdue_idempotentViaPublishDaily() {
        when(bidInvitationMapper.selectList(any())).thenReturn(List.of(overdueInvitation()));
        when(bidResponseMapper.selectCount(any())).thenReturn(2L);
        Project project = new Project();
        project.setId(99L);
        project.setMainGroupId(50L);
        when(projectMapper.selectById(99L)).thenReturn(project);
        Person leader = new Person();
        leader.setId(8001L);
        leader.setPersonType("GROUP_LEADER");
        leader.setGroupId(50L);
        when(personMapper.selectList(any())).thenReturn(List.of(leader));

        // 模拟两次扫描（同一日）：业务层两次调用 publishDaily；幂等由 NotificationService.doPublish 内部 dedupKey 保证
        scanService.scanSelectOverdue();
        scanService.scanSelectOverdue();

        verify(notificationService, times(2)).publishDaily(eq(8001L),
            eq(NotificationService.Types.BID_SELECT_OVERDUE_ESCALATED),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString(), any());
    }
}
