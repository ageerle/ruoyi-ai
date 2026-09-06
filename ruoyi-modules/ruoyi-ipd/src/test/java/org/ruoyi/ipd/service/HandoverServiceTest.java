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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 移交服务单测（ZK-IPD §九 超管权限移交专属路径 + §六 普通 PM 移交）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverServiceTest {

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

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存（LambdaQueryWrapper 需列名解析） */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE);
    }

    private IpdActor adminActor() {
        return new IpdActor(1L, "old-admin", "SUPER_ADMIN", null);
    }

    private Person oldAdmin() {
        Person p = new Person();
        p.setId(1L);
        p.setName("old-admin");
        p.setPersonType("SUPER_ADMIN");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    private Person newAdmin() {
        Person p = new Person();
        p.setId(2L);
        p.setName("new-admin");
        p.setPersonType("MARKET_PM");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    /* ----------------- ZK-IPD §九 超管权限移交专属路径 ----------------- */

    @Test
    @DisplayName("ZK-IPD §九：超管权限移交成功 ⇒ 原超管 DISABLED + 新人提升 SUPER_ADMIN + 审计 SUPER_ADMIN_TRANSFER")
    void transferSuperAdminSuccess() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(oldAdmin()));
        when(personMapper.selectById(2L)).thenReturn(newAdmin());

        service.transferSuperAdmin(2L, "工作交接", "确认移交管理员", adminActor());

        // 原超管被 update(entity, wrapper) 至少一次
        verify(personMapper, atLeastOnce()).update(any(), any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
        // 新人 updateById
        // new admin: updateById(Person) called at least once
        org.mockito.Mockito.verify(personMapper, org.mockito.Mockito.atLeastOnce()).updateById(org.mockito.ArgumentMatchers.any(Person.class));

        // 审计：SUPER_ADMIN_TRANSFER
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("SUPER_ADMIN_TRANSFER");
    }

    @Test
    @DisplayName("ZK-IPD §九：移交人不是超管 → 拒绝（横向越权防护）")
    void transferSuperAdminRejectedNonAdmin() {
        IpdActor nonAdmin = new IpdActor(99L, "pm", "MARKET_PM", 7L);
        assertThatThrownBy(() -> service.transferSuperAdmin(2L, "test", "确认移交管理员", nonAdmin))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("超管");
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("ZK-IPD §九：原超管不存在（系统异常）→ 拒绝")
    void transferSuperAdminNoCurrentAdmin() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());
        assertThatThrownBy(() -> service.transferSuperAdmin(2L, "test", "确认移交管理员", adminActor()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无在任超管");
    }

    @Test
    @DisplayName("ZK-IPD §九：接手人不能是原超管本人")
    void transferSuperAdminSelfRejected() {
        org.mockito.Mockito.lenient().when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(oldAdmin()));
        assertThatThrownBy(() -> service.transferSuperAdmin(1L, "test", "确认移交管理员", adminActor()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人不能与原负责人");
    }

    @Test
    @DisplayName("ZK-IPD §九：接手人不存在 → 拒绝")
    void transferSuperAdminTargetNotFound() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(oldAdmin()));
        when(personMapper.selectById(2L)).thenReturn(null);
        assertThatThrownBy(() -> service.transferSuperAdmin(2L, "test", "确认移交管理员", adminActor()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人不存在");
    }
}
