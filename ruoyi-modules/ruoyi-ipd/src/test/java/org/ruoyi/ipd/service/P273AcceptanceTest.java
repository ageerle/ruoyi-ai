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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-7.3 超管权限移交验收（AC-HAND-07）。
 *
 * <p>口径：
 * <ul>
 *   <li>仅超管本人可发起：非 SUPER_ADMIN 直接拒绝</li>
 *   <li>二次确认：confirmation 必须与页49 原型确认短语一致，否则拒绝（后端强制防误触）</li>
 *   <li>原超管 → DISABLED + 企微解绑（作废失效，旧会话由 scopeOf→NONE 每请求 401 兕底）</li>
 *   <li>新人 → SUPER_ADMIN（personType 切换，待办/收件箱按角色动态查询即等效转移）</li>
 *   <li>接手人不能与原超管相同；接手人不能 RESIGNED/DISABLED</li>
 *   <li>审计：SUPER_ADMIN_TRANSFER，含 from/to/原状态/企微解绑</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P273AcceptanceTest {

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
    private IpdAuthSession ipdAuthSession;
    @Mock
    private NotificationService notificationService;

    private ProjectMemberService projectMemberService;
    private HandoverService handoverService;

    private static final IpdActor CURRENT_ADMIN = new IpdActor(900L, "现任超管", "SUPER_ADMIN", null);
    private static final long NEW_ADMIN_ID = 901L;
    private static final long RESIGNED_ID = 902L;
    private static final long DISABLED_ID = 903L;
    private static final long SAME_AS_CURRENT = 900L;

    /** 页49 原型确认短语（HandoverService.CONFIRM_PHRASE 同源）。 */
    private static final String CONFIRM = "确认移交管理员";

    private Person currentAdminPerson;
    private Person newAdminPerson;

    @BeforeAll
    static void initMybatisMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "P273");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            org.mockito.Mockito.mock(SystemConfigService.class), auditLogService);
        handoverService = new HandoverService(memberMapper, personMapper, projectMapper, handoverMapper,
            auditLogService, projectMemberService, NoopTransactionManager.INSTANCE, ipdAuthSession,
            notificationService);

        currentAdminPerson = personOf(900L, "现任超管", "SUPER_ADMIN", "ACTIVE", "ACTIVE", "wecom-current");
        newAdminPerson = personOf(NEW_ADMIN_ID, "新晋超管", "GROUP_LEADER", "ACTIVE", "ACTIVE", "wecom-new");
    }

    // ---------- AC-HAND-07 正反例 ----------

    @Test
    @DisplayName("正常移交：原超管 DISABLED+解绑，新人 SUPER_ADMIN，落审计")
    void transferSuperAdmin_normal() {
        stubCurrentAdmin(currentAdminPerson);
        when(personMapper.selectById(NEW_ADMIN_ID)).thenReturn(newAdminPerson);

        handoverService.transferSuperAdmin(NEW_ADMIN_ID, "业务调整", CONFIRM, CURRENT_ADMIN);

        // 1) 原超管 → DISABLED + 企微解绑（update(null, wrapper) 而非 updateById）
        ArgumentCaptor<LambdaUpdateWrapper<Person>> origCap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(personMapper, times(1)).update(isNull(), origCap.capture());
        var origPairs = origCap.getValue().getParamNameValuePairs();
        assertThat(origPairs.toString()).contains("DISABLED");
        // 2) 新人 → SUPER_ADMIN（updateById）
        verify(personMapper, times(1)).updateById(any(Person.class));
        // 3) 审计：SUPER_ADMIN_TRANSFER
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCap.capture());
        AuditLog logged = auditCap.getValue();
        assertThat(logged.getAction()).isEqualTo("SUPER_ADMIN_TRANSFER");
        assertThat(logged.getEntityType()).isEqualTo("person");
        assertThat(logged.getEntityId()).isEqualTo(900L);
        assertThat(logged.getReason()).contains("现任超管").contains("新晋超管").contains("业务调整");
    }

    @Test
    @DisplayName("二次确认短语错误拒绝：confirmation 与页49 确认短语不符 ⇒ 无写库")
    void transferSuperAdmin_wrongConfirmation_rejected() {
        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, "确认移交", CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("确认短语不匹配");

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("二次确认缺失拒绝：confirmation=null ⇒ 无写库")
    void transferSuperAdmin_missingConfirmation_rejected() {
        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, null, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("确认短语不匹配");

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("非超管本人拒绝：MARKET_PM 移交 ⇒ 抛异常，无写库")
    void transferSuperAdmin_nonSuperAdmin_rejected() {
        IpdActor notAdmin = new IpdActor(700L, "普通PM", "MARKET_PM", 7L);

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, CONFIRM, notAdmin))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅超管本人");

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("接手人与原超管相同拒绝：toPersonId == currentAdmin.id")
    void transferSuperAdmin_sameAsCurrent_rejected() {
        stubCurrentAdmin(currentAdminPerson);

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(SAME_AS_CURRENT, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人不能与原负责人相同");

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(personMapper, never()).updateById(any(Person.class));
    }

    @Test
    @DisplayName("接手人 RESIGNED 拒绝：不可承接超管权限")
    void transferSuperAdmin_resigned_rejected() {
        stubCurrentAdmin(currentAdminPerson);
        Person resigned = personOf(RESIGNED_ID, "已离职", "GROUP_LEADER", "ACTIVE", "RESIGNED", null);
        when(personMapper.selectById(RESIGNED_ID)).thenReturn(resigned);

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(RESIGNED_ID, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已离职/禁用");

        verify(personMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("接手人 DISABLED 拒绝：不可承接超管权限")
    void transferSuperAdmin_disabled_rejected() {
        stubCurrentAdmin(currentAdminPerson);
        Person disabled = personOf(DISABLED_ID, "已禁用", "GROUP_LEADER", "DISABLED", "ACTIVE", null);
        when(personMapper.selectById(DISABLED_ID)).thenReturn(disabled);

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(DISABLED_ID, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已离职/禁用");
    }

    @Test
    @DisplayName("无在任超管拒绝：selectList 返回空 ⇒ 系统异常")
    void transferSuperAdmin_noCurrentAdmin_rejected() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("当前无在任超管");
    }

    @Test
    @DisplayName("多名在任超管拒绝：真库现状 3 名超管（违反单超管不变式）须先收敛，不得静默 LIMIT 1")
    void transferSuperAdmin_multipleAdmins_rejected() {
        Person a = personOf(900L, "现任超管", "SUPER_ADMIN", "ACTIVE", "ACTIVE", "wecom-a");
        Person b = personOf(901L, "另一超管", "SUPER_ADMIN", "ACTIVE", "ACTIVE", "wecom-b");
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("在任超管")
            .hasMessageContaining("收敛");
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("接手人不存在拒绝：selectById 返回 null")
    void transferSuperAdmin_personNotFound_rejected() {
        stubCurrentAdmin(currentAdminPerson);
        when(personMapper.selectById(NEW_ADMIN_ID)).thenReturn(null);

        assertThatThrownBy(() -> handoverService.transferSuperAdmin(NEW_ADMIN_ID, null, CONFIRM, CURRENT_ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人不存在");
    }

    // ---------- 造数 ----------

    private void stubCurrentAdmin(Person admin) {
        // sameAsCurrent 用例在查库前即被 operator.id() 对比拦截，stub 不被消费，须 lenient
        lenient().when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(admin));
    }

    private Person personOf(long id, String name, String type, String accountStatus,
                            String employmentStatus, String wecomUserId) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPersonType(type);
        p.setAccountStatus(accountStatus);
        p.setEmploymentStatus(employmentStatus);
        p.setWecomUserId(wecomUserId);
        return p;
    }
}