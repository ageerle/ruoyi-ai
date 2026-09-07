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
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-REV-HANDOVER-02（P2-7.3 复核修正版）：disableIfAllCleared 语义与并发防护。
 *
 * <p>演进：selectCount+update（无锁，TOCTOU）→ 原子 UPDATE 版（行锁完备，但会把余留绑定
 * 一并置退出并禁用，名下多项目时违反 AC-HAND-01d 及 P2-7.2「不得提前禁用仍有待移交人员」）→
 * 本版：FOR UPDATE 锁行计数（保留 TOCTOU 防护，串行化并发移交/新增绑定）
 * + 仅真全清（活跃绑定计数=0）才禁用
 * + person 侧 accountStatus=ACTIVE 条件守卫（并发双过计数窗口仅一人生效，后到者 affected=0 不重复审计）。
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
    @Mock
    private IpdAuthSession ipdAuthSession;
    @Mock
    private NotificationService notificationService;

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
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            ipdAuthSession, notificationService);
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
    @DisplayName("真全清（FOR UPDATE 计数=0）→ person DISABLED + ACCOUNT_DISABLED_AFTER_HANDOVER 审计")
    void disableIfAllCleared_allCleared_disablesPersonAndAudits() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(personMapper.selectById(50L)).thenReturn(activePerson(50L));
        when(personMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.disableIfAllCleared(50L, operator());

        // 计数版不再原子退出绑定（绑定退出由 exitForHandover 负责），只读锁定计数
        verify(memberMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        ArgumentCaptor<LambdaUpdateWrapper<Person>> personCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(personMapper, times(1)).update(isNull(), personCap.capture());

        ArgumentCaptor<org.ruoyi.ipd.domain.AuditLog> auditCap =
            ArgumentCaptor.forClass(org.ruoyi.ipd.domain.AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    @Test
    @DisplayName("仍有余留绑定（FOR UPDATE 计数>0）→ 直接返回：不 selectById、不 person update、不审计（不得提前禁用）")
    void disableIfAllCleared_hasRemaining_noDisableNoAudit() {
        ProjectMember binding = new ProjectMember();
        binding.setPersonId(50L);
        binding.setProjectId(7L);
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding));

        service.disableIfAllCleared(50L, operator());

        verify(personMapper, never()).selectById(any());
        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("并发守卫：真全清但 person update affected=0（并发已禁）→ 不写审计")
    void disableIfAllCleared_concurrentGuard_noDuplicateAudit() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(personMapper.selectById(50L)).thenReturn(activePerson(50L));
        when(personMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        service.disableIfAllCleared(50L, operator());

        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("真全清但 person 已 DISABLED → 不再 update + 不写审计")
    void disableIfAllCleared_activeClearedButPersonAlreadyDisabled_noUpdateNoAudit() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(personMapper.selectById(50L)).thenReturn(alreadyDisabledPerson(50L));

        service.disableIfAllCleared(50L, operator());

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("真全清但 person 不存在 → 不再 update + 不写审计")
    void disableIfAllCleared_activeClearedButPersonNotFound_noUpdateNoAudit() {
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(personMapper.selectById(50L)).thenReturn(null);

        service.disableIfAllCleared(50L, operator());

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }
}
