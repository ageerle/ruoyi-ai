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
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-REV-HANDOVER-02：disableIfAllCleared 原子 UPDATE 替代 selectCount + person update
 * （避免 TOCTOU 并发竞态）。
 *
 * <p>原 bug：selectCount 与 personMapper.update 之间无锁；
 * 多个并发请求都可能观察到 remaining==0 然后都执行 person disable，
 * 但 member exitDate 的关闭在多次调用间存在重复/丢失风险。
 *
 * <p>修复语义：单条 atomic UPDATE 一举关闭该 person 下所有 exitDate IS NULL 的
 * project_member 行，affected rows 数即"原本有多少活跃绑定"——
 * 0 ⇒ 之前就没人，不需要 disable；>0 ⇒ 原本有，现在都关掉了，应该 disable。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverDisableConcurrencyTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private HandoverMapper handoverMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ProjectMemberService projectMemberService;

    private HandoverService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService);
    }

    private IpdActor operator() {
        return new IpdActor(99L, "operator", "GROUP_LEADER", 10L);
    }

    private Person activePerson(Long id) {
        Person p = new Person();
        p.setId(id);
        p.setName("p-" + id);
        p.setPersonType("MARKET_PM");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    private Person alreadyDisabledPerson(Long id) {
        Person p = new Person();
        p.setId(id);
        p.setName("p-" + id);
        p.setPersonType("MARKET_PM");
        p.setAccountStatus("DISABLED");
        p.setEmploymentStatus("RESIGNED");
        return p;
    }

    @Test
    @DisplayName("Bug#2: 原子 UPDATE 返回 0（原无活跃绑定）→ 不调 personMapper.update + 不写审计")
    void disableIfAllCleared_zeroActive_noPersonUpdateNoAudit() {
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        service.disableIfAllCleared(50L, operator());

        // 不应 selectCount（已删的旧实现路径）—— 仅靠原子 update
        verify(memberMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        // 0 行受影响 ⇒ 不需要 disable
        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("Bug#2: 原子 UPDATE 返回 >0（原活跃绑定）→ 调 personMapper DISABLED + 写 ACCOUNT_DISABLED_AFTER_HANDOVER 审计")
    void disableIfAllCleared_activeCleared_disablesPersonAndAudits() {
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(3);
        when(personMapper.selectById(50L)).thenReturn(activePerson(50L));

        service.disableIfAllCleared(50L, operator());

        // 原子 update 调一次（行锁临界区）
        ArgumentCaptor<LambdaUpdateWrapper<ProjectMember>> memberCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(memberMapper, atMostOnce()).update(any(), memberCap.capture());

        // personMapper.update DISABLED
        ArgumentCaptor<LambdaUpdateWrapper<Person>> personCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(personMapper, atMostOnce()).update(any(), personCap.capture());

        // 审计调用一次：ACCOUNT_DISABLED_AFTER_HANDOVER
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("Bug#2: 原子 UPDATE 关闭活跃成员，但 person 已是 DISABLED → 不再 update + 不写审计")
    void disableIfAllCleared_activeClearedButPersonAlreadyDisabled_noUpdateNoAudit() {
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(2);
        when(personMapper.selectById(50L)).thenReturn(alreadyDisabledPerson(50L));

        service.disableIfAllCleared(50L, operator());

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("Bug#2: 原子 UPDATE 关闭活跃成员，但 person 不存在 → 不再 update + 不写审计")
    void disableIfAllCleared_activeClearedButPersonNotFound_noUpdateNoAudit() {
        when(memberMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(2);
        when(personMapper.selectById(50L)).thenReturn(null);

        service.disableIfAllCleared(50L, operator());

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }
}
