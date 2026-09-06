package org.ruoyi.ipd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.advice.IpdServiceExceptionAdvice;
import org.ruoyi.ipd.controller.WorkbenchController;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.security.IpdPermissionExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4-3.1 验收：Workbench 聚合端点（GET /api/v1/workbench/summary）真 HTTP 链路。
 *
 * <p>覆盖 6 维度：
 * ① 正常路径：MARKET_PM actor → stats + tasks + deletionPending + currentAdvance
 * ② 边界：无可见项目 → stats 全 0，currentAdvance=null
 * ③ 异常：requireInternal 拒绝 → IpdPermissionExceptionHandler 转 403 ApiV1Response
 * ④ 权限责任链：MARKET_PM 看不到 RD_PM 任务
 * ⑤ 删除审批分流：GROUP_LEADER 命中 LEADER_REVIEW 数
 * ⑥ 当前推进：取首个 isMine+OPEN 动作
 *
 * <p>MockMvc standaloneSetup 走完整 Spring MVC 请求分发 → Controller →
 * 全局异常处理链 → JSON 响应体，断言客户端真收到的状态码与业务码，
 * 堵住「service 测试通过即认为 HTTP 关卡通过」的假绿。
 *
 * <p>Service 6 维 Mockito 单测见 {@link WorkbenchServiceTest}。
 * @Tag("dev") 必须——surefire groups=${profiles.active} 过滤。
 */
@Tag("dev")
class P431AcceptanceTest {

    private static final String URL = "/api/v1/workbench/summary";
    private static final int CODE_OK = 0;

    private ProjectMapper projectMapper;
    private ProjectMemberMapper projectMemberMapper;
    private StageActionMapper stageActionMapper;
    private DeletionRequestMapper deletionRequestMapper;
    private NotificationService notificationService;
    private IpdPermission ipdPermission;
    private WorkbenchService workbenchService;
    private WorkbenchController controller;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        projectMapper = mock(ProjectMapper.class);
        projectMemberMapper = mock(ProjectMemberMapper.class);
        stageActionMapper = mock(StageActionMapper.class);
        deletionRequestMapper = mock(DeletionRequestMapper.class);
        notificationService = mock(NotificationService.class);
        ipdPermission = mock(IpdPermission.class);
        workbenchService = new WorkbenchService(
            projectMapper, projectMemberMapper, stageActionMapper,
            deletionRequestMapper, notificationService);
        controller = new WorkbenchController(ipdPermission, workbenchService);
        mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .setControllerAdvice(new IpdServiceExceptionAdvice(), new IpdPermissionExceptionHandler())
            .build();
    }

    private Project project(long id, String code, String name, String currentStage) {
        return Project.builder()
            .id(id).code(code).name(name).currentStage(currentStage).status("ACTIVE")
            .build();
    }

    private StageAction action(long id, long projectId, String code, String name,
                               String ownerRole, String status) {
        return StageAction.builder()
            .id(id).projectId(projectId).actionCode(code).actionName(name)
            .ownerRole(ownerRole).status(status).isBlocking("N")
            .build();
    }

    private ProjectMember member(long projectId, long personId) {
        return ProjectMember.builder().projectId(projectId).personId(personId).build();
    }

    @Test
    @DisplayName("正常路径：MARKET_PM → 200 + code=0，stats/tasks/deletionPending/currentAdvance 全部就位")
    void summary_returnsAllFields() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any())).thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any())).thenReturn(List.of(
            action(101L, 10L, "CDP-01", "立项", "MARKET_PM", "IN_PROGRESS"),
            action(102L, 10L, "CDP-02", "研发", "RD_PM", "NOT_STARTED"),
            action(103L, 10L, "CDP-03", "共担", "BOTH", "DONE")));
        when(notificationService.unreadCount(1L)).thenReturn(5L);
        when(deletionRequestMapper.selectCount(any())).thenReturn(0L);

        mvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.stats.pending").value(1))
            .andExpect(jsonPath("$.data.stats.completed").value(1))
            .andExpect(jsonPath("$.data.stats.unread").value(5))
            .andExpect(jsonPath("$.data.tasks.length()").value(1))
            .andExpect(jsonPath("$.data.tasks[0].actionCode").value("CDP-01"))
            .andExpect(jsonPath("$.data.tasks[0].projectCode").value("P-001"))
            .andExpect(jsonPath("$.data.currentAdvance.actionId").value(101))
            .andExpect(jsonPath("$.data.currentAdvance.currentStage").value("CDP"))
            .andExpect(jsonPath("$.data.deletionPending").value(0));
    }

    @Test
    @DisplayName("边界：actor 无可见项目 → 200 + code=0，stats 全 0，currentAdvance=null")
    void summary_emptyScope() throws Exception {
        IpdActor actor = new IpdActor(2L, "bob", "RD_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of());

        mvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.stats.pending").value(0))
            .andExpect(jsonPath("$.data.stats.overdue").value(0))
            .andExpect(jsonPath("$.data.stats.unread").value(0))
            .andExpect(jsonPath("$.data.stats.completed").value(0))
            .andExpect(jsonPath("$.data.tasks.length()").value(0))
            .andExpect(jsonPath("$.data.currentAdvance").doesNotExist())
            .andExpect(jsonPath("$.data.deletionPending").value(0));
    }

    @Test
    @DisplayName("权限责任链：MARKET_PM 看不到 RD_PM 任务 → tasks 仅含 MARKET_PM/BOTH")
    void summary_filtersByOwnerRole() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any())).thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any())).thenReturn(List.of(
            action(101L, 10L, "CDP-01", "市场", "MARKET_PM", "IN_PROGRESS"),
            action(102L, 10L, "CDP-02", "研发", "RD_PM", "IN_PROGRESS"),
            action(103L, 10L, "CDP-03", "共担", "BOTH", "IN_PROGRESS")));
        when(notificationService.unreadCount(any())).thenReturn(0L);
        when(deletionRequestMapper.selectCount(any())).thenReturn(0L);

        mvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tasks.length()").value(2))
            .andExpect(jsonPath("$.data.tasks[?(@.actionCode=='CDP-02')]").isEmpty());
    }

    @Test
    @DisplayName("删除审批分流：GROUP_LEADER actor → deletionPending == LEADER_REVIEW 数")
    void summary_deletionPending_forGroupLeader() throws Exception {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(ipdPermission.requireInternal()).thenReturn(leader);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of());
        when(deletionRequestMapper.selectCount(any())).thenReturn(7L);

        mvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.deletionPending").value(7));
    }

    @Test
    @DisplayName("异常：requireInternal 拒绝 → 403 + FORBIDDEN 业务码（真 advice 转 IpdPermissionException）")
    void summary_permissionDenied_returnsForbidden() throws Exception {
        org.ruoyi.ipd.common.ApiV1ErrorCode forbidden = org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN;
        IpdPermissionException denied = new IpdPermissionException(forbidden.getHttpStatus(), forbidden);
        when(ipdPermission.requireInternal()).thenThrow(denied);

        mvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(forbidden.getCode()));
    }
}
