package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIGH-3.1：HandoverService.rollback 验收测试（5 测）。
 *
 * <p>覆盖：
 * <ol>
 *   <li>正常撤销（COMPLETED + 24h 内）→ ROLLED_BACK + 副作用反转</li>
 *   <li>24h 窗口外撤销 → HANDOVER_LOCKED</li>
 *   <li>重复撤销幂等 → HANDOVER_LOCKED（已撤销）</li>
 *   <li>非发起人 + 非组长 + 非超管 → FORBIDDEN</li>
 *   <li>副作用反转（接手人退出 + 发起人绑定恢复）</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverRollbackAcceptanceTest {

    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private HandoverMapper handoverMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMemberService projectMemberService;
    @Mock private IpdAuthSession ipdAuthSession;

    private HandoverService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            ipdAuthSession);
    }

    private IpdActor fromActor() {
        return new IpdActor(10L, "from-pm", "MARKET_PM", 7L);
    }

    private IpdActor toActor() {
        return new IpdActor(20L, "to-pm", "MARKET_PM", 7L);
    }

    private IpdActor superAdminActor() {
        return new IpdActor(1L, "admin", "SUPER_ADMIN", null);
    }

    private IpdActor groupLeaderSameGroup() {
        return new IpdActor(99L, "leader", "GROUP_LEADER", 7L);
    }

    private IpdActor unrelatedPm() {
        return new IpdActor(50L, "unrelated", "MARKET_PM", 8L);
    }

    private HandoverRecord completedHandover() {
        Date recent = new Date(System.currentTimeMillis() - 60_000L); // 1 分钟前
        return HandoverRecord.builder()
            .id(1001L)
            .handoverType("PROJECT")
            .fromPersonId(10L)
            .toPersonId(20L)
            .projectId(200L)
            .handoverRole("MARKET_PM")
            .status("COMPLETED")
            .completedAt(recent)
            .build();
    }

    private HandoverRecord oldCompletedHandover() {
        Date tooOld = new Date(System.currentTimeMillis() - 25L * 3_600_000L); // 25h 前
        return HandoverRecord.builder()
            .id(1002L)
            .handoverType("PROJECT")
            .fromPersonId(10L)
            .toPersonId(20L)
            .projectId(200L)
            .handoverRole("MARKET_PM")
            .status("COMPLETED")
            .completedAt(tooOld)
            .build();
    }

    private HandoverRecord alreadyRolledBack() {
        return HandoverRecord.builder()
            .id(1003L)
            .handoverType("PROJECT")
            .fromPersonId(10L)
            .toPersonId(20L)
            .projectId(200L)
            .handoverRole("MARKET_PM")
            .status("ROLLED_BACK")
            .completedAt(new Date(System.currentTimeMillis() - 60_000L))
            .rollbackReason("已撤销")
            .rollbackAt(new Date())
            .build();
    }

    private Project projectInGroup(Long mainGroupId) {
        return Project.builder().id(200L).name("Test-Project").mainGroupId(mainGroupId).build();
    }

    @Test
    @DisplayName("Bug#HIGH-3.1#1: 正常撤销（COMPLETED + 24h 内）→ ROLLED_BACK + 副作用反转 + 审计 HANDOVER_ROLLBACK")
    void rollback_normalCompleted_rollsBack() {
        HandoverRecord rec = completedHandover();
        when(handoverMapper.selectById(1001L)).thenReturn(rec);
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        HandoverRecord result = service.rollback(1001L, "误操作需要重做", fromActor());

        assertThat(result.getStatus()).isEqualTo("ROLLED_BACK");
        assertThat(result.getRollbackReason()).isEqualTo("误操作需要重做");
        assertThat(result.getRollbackAt()).isNotNull();

        // 副作用反转：接手人 exit + 发起人恢复 各一次 update
        verify(memberMapper, times(2)).update(any(), any(LambdaUpdateWrapper.class));
        // handover 状态写回
        verify(handoverMapper, times(1)).updateById(rec);
        // 审计 HANDOVER_ROLLBACK
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("HANDOVER_ROLLBACK");
        assertThat(auditCap.getValue().getEntityId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("Bug#HIGH-3.1#2: 24h 窗口外撤销 → 拒绝（业务码 HANDOVER_LOCKED）")
    void rollback_beyond24h_rejected() {
        HandoverRecord rec = oldCompletedHandover();
        when(handoverMapper.selectById(1002L)).thenReturn(rec);

        assertThatThrownBy(() -> service.rollback(1002L, "撤销", fromActor()))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.HANDOVER_LOCKED);

        verify(memberMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(handoverMapper, never()).updateById(any(HandoverRecord.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("Bug#HIGH-3.1#3: 重复撤销幂等 → 拒绝（业务码 HANDOVER_LOCKED）")
    void rollback_alreadyRolledBack_rejected() {
        HandoverRecord rec = alreadyRolledBack();
        when(handoverMapper.selectById(1003L)).thenReturn(rec);

        assertThatThrownBy(() -> service.rollback(1003L, "再撤一次", fromActor()))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.HANDOVER_LOCKED);

        verify(memberMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(handoverMapper, never()).updateById(any(HandoverRecord.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("Bug#HIGH-3.1#4: 非发起人 + 非组长 + 非超管 → 拒绝（FORBIDDEN）")
    void rollback_unauthorizedActor_rejected() {
        HandoverRecord rec = completedHandover();
        when(handoverMapper.selectById(1001L)).thenReturn(rec);
        when(projectMapper.selectById(200L)).thenReturn(projectInGroup(7L));

        // unrelatedPm 既不是发起人(from=10L, to=20L)，也不是超管，也不是项目主组(7L)的组长
        assertThatThrownBy(() -> service.rollback(1001L, "撤销", unrelatedPm()))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        verify(memberMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(handoverMapper, never()).updateById(any(HandoverRecord.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("Bug#HIGH-3.1#5: 副作用反转——接手人 exit + 发起人绑定恢复 + 审计 HANDOVER_ROLLBACK")
    void rollback_sideEffectReversal() {
        HandoverRecord rec = completedHandover();
        when(handoverMapper.selectById(1001L)).thenReturn(rec);
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.rollback(1001L, "误操作需要重做", fromActor());

        // 两次 update：第一次接手人 exit，第二次发起人恢复
        ArgumentCaptor<LambdaUpdateWrapper<ProjectMember>> memberCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(memberMapper, times(2)).update(isNull(), memberCap.capture());

        // handover 状态写入 ROLLED_BACK + 原因 + 时间
        verify(handoverMapper).updateById(rec);
        assertThat(rec.getStatus()).isEqualTo("ROLLED_BACK");
        assertThat(rec.getRollbackReason()).isEqualTo("误操作需要重做");
        assertThat(rec.getRollbackAt()).isNotNull();

        // 审计 HANDOVER_ROLLBACK
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("HANDOVER_ROLLBACK");
        assertThat(auditCap.getValue().getOperatorId()).isEqualTo(10L);
    }
}