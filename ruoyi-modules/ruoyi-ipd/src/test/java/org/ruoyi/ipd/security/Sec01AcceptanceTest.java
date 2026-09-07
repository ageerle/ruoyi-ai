package org.ruoyi.ipd.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.controller.CertTemplateController;
import org.ruoyi.ipd.controller.CoefficientChangeController;
import org.ruoyi.ipd.controller.DeletionRequestController;
import org.ruoyi.ipd.controller.GateElementController;
import org.ruoyi.ipd.controller.IpdAuthController;
import org.ruoyi.ipd.controller.ProductController;
import org.ruoyi.ipd.controller.ProjectController;
import org.ruoyi.ipd.controller.StageActionController;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.vo.CertTemplateVO;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateElementCreateReq;
import org.ruoyi.ipd.dto.ProjectCreateReq;
import org.ruoyi.ipd.service.CertTemplateService;
import org.ruoyi.ipd.service.GateElementService;
import org.ruoyi.ipd.service.GateEngine;
import org.ruoyi.ipd.service.IpdAuthService;
import org.ruoyi.ipd.service.LegacyImportService;
import org.ruoyi.ipd.service.ProjectCertService;
import org.ruoyi.ipd.service.ProjectService;
import org.ruoyi.ipd.service.StageActionService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-01 命名验收：绑定会话操作人与四角色接口权限（@Tag dev）。
 * <p>覆盖 QA 点名缺口（2026-09-05 复核）：
 * <ul>
 *   <li>AC-AUTH-08：普通PM 调超管接口 → 403 且零写入</li>
 *   <li>审计身份来自会话（IpdActor），客户端无伪造入口</li>
 *   <li>AC-HR-07：不存在「代理组长」设置入口（A3 决策不做）</li>
 *   <li>AC-HR-08：角色固定不可跨（市场PM/研发PM 互斥）</li>
 *   <li>AC-GLB-11：仅四角色体系，无评审上级/代理组长/代审人</li>
 * </ul>
 * 注：历史 untracked 的 59 项版本已随蜂群清扫丢失，本版按 QA 缺口聚焦重建。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class Sec01AcceptanceTest {

    private static final Long ADMIN_ID = 900101L;
    private static final Long MARKET_PM_ID = 900102L;
    private static final Long RD_PM_ID = 900103L;
    private static final Long GROUP_ID = 7L;

    @Mock
    private IpdAuthSession session;
    @Mock
    private IpdAuthService authService;
    @Mock
    private ProjectService projectService;
    @Mock
    private StageActionService stageActionService;
    @Mock
    private GateEngine gateEngine;
    @Mock
    private ProjectCertService projectCertService;
    @Mock
    private LegacyImportService legacyImportService;
    @Mock private org.ruoyi.ipd.service.LaunchDateChangeService launchDateChangeService;
    @Mock private org.ruoyi.ipd.service.GateCreationService gateCreationService;
    @Mock
    private CertTemplateService certTemplateService;
    @Mock
    private GateElementService gateElementService;

    private IpdPermission ipdPermission;
    private ProjectController projectController;
    private StageActionController stageActionController;
    private CertTemplateController certTemplateController;
    private GateElementController gateElementController;

    @BeforeEach
    void setUp() {
        ipdPermission = new IpdPermission(session, authService);
        projectController = new ProjectController(projectService, gateEngine, projectCertService, legacyImportService, launchDateChangeService, gateCreationService, ipdPermission);
        stageActionController = new StageActionController(stageActionService, ipdPermission);
        certTemplateController = new CertTemplateController(certTemplateService, ipdPermission);
        gateElementController = new GateElementController(gateElementService, ipdPermission);
    }

    /** 以指定角色建立会话（SEC-01 边界：角色只会来自 IpdAuthSession.currentPerson）。 */
    private void loginAs(long personId, String personType) {
        Person person = new Person();
        person.setId(personId);
        person.setName("U" + personId);
        person.setPersonType(personType);
        person.setGroupId(GROUP_ID);
        lenient().when(session.currentPerson()).thenReturn(person);
        lenient().when(authService.scopeOf(person)).thenReturn(IpdAuthService.Scope.FULL);
    }

    private static void assertForbidden(Throwable ex) {
        assertThat(ex).isInstanceOf(IpdPermissionException.class);
        IpdPermissionException pex = (IpdPermissionException) ex;
        assertThat(pex.getHttpStatus()).isEqualTo(403);
        assertThat(pex.getErrorCode()).isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    // ===== AC-AUTH-08：普通PM 调超管接口 → 403 且零写入 =====

    @Test
    @DisplayName("AC-AUTH-08 普通PM 创建认证模板（超管接口）→ 403 且零写入")
    void marketPmCannotCreateCertTemplate() {
        loginAs(MARKET_PM_ID, "MARKET_PM");

        assertThatThrownBy(() -> certTemplateController.create(new CertTemplate()))
            .satisfies(Sec01AcceptanceTest::assertForbidden);
        verify(certTemplateService, never()).create(any(), any());
    }

    @Test
    @DisplayName("AC-AUTH-08 普通PM 走 remove 旁路删除认证模板 → 403 且零写入")
    void marketPmCannotRemoveCertTemplate() {
        loginAs(MARKET_PM_ID, "MARKET_PM");

        assertThatThrownBy(() -> certTemplateController.remove(1L))
            .satisfies(Sec01AcceptanceTest::assertForbidden);
        verify(certTemplateService, never()).remove(anyLong(), any());
    }

    @Test
    @DisplayName("AC-AUTH-08 普通PM 停用 Gate 评审要素（超管接口）→ 403 且零写入")
    void marketPmCannotDisableGateElement() {
        loginAs(MARKET_PM_ID, "MARKET_PM");

        assertThatThrownBy(() -> gateElementController.disable(1L))
            .satisfies(Sec01AcceptanceTest::assertForbidden);
        verify(gateElementService, never()).disable(anyLong(), any(IpdActor.class));
    }

    @Test
    @DisplayName("AC-AUTH-08 正例：超管创建认证模板成功，审计 operator=会话身份")
    void superAdminCreatesCertTemplateWithSessionIdentity() {
        loginAs(ADMIN_ID, "SUPER_ADMIN");
        CertTemplate template = new CertTemplate();
        when(certTemplateService.create(any(), any())).thenReturn(template);

        ApiV1Response<CertTemplateVO> response = certTemplateController.create(template);

        // 控制器返回 VO（CODE-01：不再把内部字段 delFlag/tenantId/createBy 等透出给客户端）
        assertThat(response.getData()).isEqualTo(CertTemplateVO.from(template));
        ArgumentCaptor<Long> operator = ArgumentCaptor.forClass(Long.class);
        verify(certTemplateService).create(any(), operator.capture());
        assertThat(operator.getValue()).isEqualTo(ADMIN_ID);
    }

    // ===== 角色来自会话：审计身份不可伪造 =====

    @Test
    @DisplayName("项目状态变更审计 operator 取会话身份（客户端无 operatorId 入口）")
    void projectChangeStatusAuditIdComesFromSession() {
        loginAs(MARKET_PM_ID, "MARKET_PM");
        when(projectService.changeStatus(anyLong(), anyString(), any(), any(), any())).thenReturn(new Project());

        projectController.changeStatus(11L, "TEAMING");

        ArgumentCaptor<Long> operator = ArgumentCaptor.forClass(Long.class);
        verify(projectService).changeStatus(eq(11L), eq("TEAMING"), operator.capture(), any(), any());
        assertThat(operator.getValue()).isEqualTo(MARKET_PM_ID);
    }

    @Test
    @DisplayName("阶段动作 transit 审计 operator 取会话身份字符串")
    void stageActionTransitOperatorComesFromSession() {
        loginAs(MARKET_PM_ID, "MARKET_PM");
        StageAction action = new StageAction();
        action.setOwnerRole("MARKET_PM");
        when(stageActionService.getById(5L)).thenReturn(action);
        when(stageActionService.transit(anyLong(), anyString(), any(), anyString())).thenReturn(action);

        stageActionController.transit(5L, "DONE", null);

        ArgumentCaptor<String> operator = ArgumentCaptor.forClass(String.class);
        verify(stageActionService).transit(eq(5L), eq("DONE"), any(), operator.capture());
        assertThat(operator.getValue()).isEqualTo(String.valueOf(MARKET_PM_ID));
    }

    // ===== AC-HR-08：角色固定不可跨 =====

    @Test
    @DisplayName("AC-HR-08 市场PM 执行研发PM 专属动作 → 409 ROLE_LOCKED 且零写入")
    void marketPmCannotTransitRdPmOwnedAction() {
        loginAs(MARKET_PM_ID, "MARKET_PM");
        StageAction action = new StageAction();
        action.setOwnerRole("RD_PM");
        when(stageActionService.getById(6L)).thenReturn(action);

        assertThatThrownBy(() -> stageActionController.transit(6L, "DONE", null))
            .isInstanceOf(IpdPermissionException.class)
            .satisfies(ex -> {
                IpdPermissionException pex = (IpdPermissionException) ex;
                assertThat(pex.getHttpStatus()).isEqualTo(409);
                assertThat(pex.getErrorCode()).isEqualTo(ApiV1ErrorCode.ROLE_LOCKED);
            });
        verify(stageActionService, never()).transit(anyLong(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("AC-HR-08 研发PM 尝试创建项目（市场侧发起人专属）→ 403 且零写入")
    void rdPmCannotCreateProject() {
        loginAs(RD_PM_ID, "RD_PM");
        ProjectCreateReq req = new ProjectCreateReq(
            null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> projectController.create(req))
            .satisfies(Sec01AcceptanceTest::assertForbidden);
        verify(projectService, never()).create(any(Project.class), any());
    }

    // ===== AC-HR-07：不存在「代理组长」设置入口（A3 决策不做） =====

    @Test
    @DisplayName("AC-HR-07 全部控制器无 deputy/delegate/proxy/grant 代理授权端点")
    void noDeputyOrDelegateEndpointInControllers() {
        List<Class<?>> controllers = List.of(
            ProjectController.class, ProductController.class, StageActionController.class,
            CertTemplateController.class, GateElementController.class,
            IpdAuthController.class, DeletionRequestController.class, CoefficientChangeController.class);
        List<String> offenders = new ArrayList<>();
        for (Class<?> type : controllers) {
            RequestMapping root = type.getAnnotation(RequestMapping.class);
            String basePath = root == null ? "" : String.join(",", root.value());            for (Method method : type.getDeclaredMethods()) {
                String path = basePath + "|" + mappingPath(method) + "|" + method.getName();
                for (String keyword : new String[] {"deputy", "delegate", "proxy", "grant", "actas", "impersonat"}) {
                    if (path.toLowerCase().contains(keyword)) {
                        offenders.add(type.getSimpleName() + "#" + method.getName() + " 命中 " + keyword);
                    }
                }
            }
        }
        assertThat(offenders).as("疑似代理/代审授权入口").isEmpty();
    }

    @Test
    @DisplayName("AC-HR-07 安全层无角色授予/变更方法（角色固定在 personType）")
    void securityLayerHasNoRoleGrantMethod() {
        List<String> offenders = new ArrayList<>();
        for (Method method : IpdPermission.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase();
            if (name.startsWith("grant") || name.startsWith("setrole") || name.startsWith("actas")
                || name.startsWith("deputy") || name.startsWith("impersonat")) {
                offenders.add("IpdPermission#" + method.getName());
            }
        }
        assertThat(offenders).as("运行期角色授予入口").isEmpty();
    }

    // ===== AC-GLB-11：四角色体系，无评审上级/代理组长/代审人 =====

    @Test
    @DisplayName("AC-GLB-11 权限目录恰好覆盖四角色（G-09）")
    void permissionCatalogContainsExactlyFourRoles() {
        Set<String> expected = Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER", "SUPER_ADMIN");
        for (String role : expected) {
            assertThat(IpdRolePermissionCatalog.rolesOf(role)).as(role).containsExactly(role);
            assertThat(IpdRolePermissionCatalog.permissionsOf(role)).as(role + " 权限").isNotEmpty();
        }
        assertThat(expected).as("角色全集数量（G-09 四角色）").hasSize(4);
    }

    @Test
    @DisplayName("AC-GLB-11 评审上级/代理组长/代审人/游客等未知角色无任何权限")
    void unknownRolesGetNoPermissions() {
        for (String probe : new String[] {"REVIEW_SUPERIOR", "DEPUTY_LEADER", "PROXY_REVIEWER",
            "GUEST", "ADMIN", "LEADER", "", "   "}) {
            assertThat(IpdRolePermissionCatalog.rolesOf(probe)).as(probe).isEmpty();
            assertThat(IpdRolePermissionCatalog.permissionsOf(probe)).as(probe + " 权限").isEmpty();
        }
        assertThat(IpdRolePermissionCatalog.permissionsOf(null)).isEmpty();
    }

    @Test
    @DisplayName("游客（GUEST personType）不可通过内部接口角色门禁 → 403")
    void guestDeniedInternalAccess() {
        loginAs(999901L, "GUEST");

        assertThatThrownBy(() -> projectController.list(null))
            .satisfies(Sec01AcceptanceTest::assertForbidden);
        verify(projectService, never()).list(anyString());
    }

    /** 拼接方法级 mapping 路径（@GetMapping 等经元注解合并后读取）。 */
    private static String mappingPath(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) {
            return "";
        }
        return String.join(",", mapping.path()) + String.join(",", mapping.value());
    }
}
