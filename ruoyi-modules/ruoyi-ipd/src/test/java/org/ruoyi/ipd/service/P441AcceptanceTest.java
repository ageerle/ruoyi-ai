package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.advice.IpdServiceExceptionAdvice;
import org.ruoyi.ipd.controller.IpdReportController;
import org.ruoyi.ipd.domain.*;
import org.ruoyi.ipd.dto.ReportExportResult;
import org.ruoyi.ipd.dto.ReportSummaryRow;
import org.ruoyi.ipd.mapper.*;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.security.IpdPermissionExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
 * P4-4.1 验收：项目组织绩效汇总与导出契约（AC-INC-34 / BR-INC-15）真 HTTP 链路。
 *
 * <p>覆盖 6 维度：
 * ① 正常路径：MARKET_PM actor → 200 + code=0，summary 列表 + 金额单位 2 位 + 三表聚合
 * ② 边界：actor 无可见项目 → 200 + code=0，total=0（不报错）
 * ③ 异常：requireInternal 拒绝 → 403 + FORBIDDEN 业务码（真 advice 转 IpdPermissionException）
 * ④ 列表与导出同范围同筛选（共用 month+projectId 参数）
 * ⑤ 大数据分页：pageSize 上限 MAX_PAGE_SIZE=500；导出上限 MAX_EXPORT_SIZE=5000
 * ⑥ 脱敏与导出审计：导出动作落 EXPORT_REPORT 审计（独立事务）
 *
 * <p>MockMvc standaloneSetup 走完整 Spring MVC 请求分发 → Controller →
 * 全局异常处理链 → JSON 响应体，断言客户端真收到的状态码与业务码。
 *
 * @Tag("dev") 必须——surefire groups=${profiles.active} 过滤。
 */
@Tag("dev")
class P441AcceptanceTest {

    private static final String URL_LIST = "/api/v1/report/project-summary";
    private static final String URL_EXPORT_ALLOWANCE = "/api/v1/report/export/allowance";
    private static final String URL_EXPORT_BONUS = "/api/v1/report/export/bonus";
    private static final String URL_EXPORT_PROJECT = "/api/v1/report/export/project";
    private static final int CODE_OK = 0;

    private AllowanceLedgerMapper allowanceLedgerMapper;
    private BonusPoolMapper bonusPoolMapper;
    private ProjectScoreMapper projectScoreMapper;
    private ProjectMapper projectMapper;
    private ProjectMemberMapper projectMemberMapper;
    private PersonMapper personMapper;
    private AuditLogService auditLogService;
    private IpdPermission ipdPermission;
    private IpdReportService ipdReportService;
    private IpdReportController controller;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        allowanceLedgerMapper = mock(AllowanceLedgerMapper.class);
        bonusPoolMapper = mock(BonusPoolMapper.class);
        projectScoreMapper = mock(ProjectScoreMapper.class);
        projectMapper = mock(ProjectMapper.class);
        projectMemberMapper = mock(ProjectMemberMapper.class);
        personMapper = mock(PersonMapper.class);
        auditLogService = mock(AuditLogService.class);
        ipdPermission = mock(IpdPermission.class);
        ipdReportService = new IpdReportService(
            allowanceLedgerMapper, bonusPoolMapper, projectScoreMapper,
            projectMapper, projectMemberMapper, personMapper,
            auditLogService, ipdPermission);
        controller = new IpdReportController(ipdPermission, ipdReportService);
        mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .setControllerAdvice(new IpdServiceExceptionAdvice(), new IpdPermissionExceptionHandler())
            .build();
    }

    private Project project(long id, String code, String name) {
        return Project.builder().id(id).code(code).name(name).status("ACTIVE").build();
    }

    private AllowanceLedger ledger(long id, long projectId, long personId, String month, String amount) {
        return AllowanceLedger.builder()
            .id(id).projectId(projectId).personId(personId).month(month)
            .lockedLevel("L3").baseAmount(new BigDecimal(amount))
            .finalAmount(new BigDecimal(amount)).capApplied("0").delFlag("0")
            .build();
    }

    private BonusPool pool(long id, long projectId, String amount, String status) {
        return BonusPool.builder()
            .id(id).projectId(projectId).status(status)
            .finalPool(new BigDecimal(amount)).poolRate(new BigDecimal("0.05"))
            .tierCoefficient(new BigDecimal("1.00")).coefficient(new BigDecimal("1.00"))
            .achievementRate(new BigDecimal("1.00")).basePool(new BigDecimal(amount))
            .targetSales(new BigDecimal("1000000"))
            .build();
    }

    private ProjectScore score(long id, long projectId, long personId, String pmRole, String ws) {
        return ProjectScore.builder()
            .id(id).projectId(projectId).personId(personId).pmRole(pmRole)
            .weightedScore(new BigDecimal(ws)).status("CONFIRMED").delFlag("0")
            .build();
    }

    private Person person(long id, String name, String role, Long groupId) {
        return Person.builder().id(id).name(name).personType(role).groupId(groupId).build();
    }

    private ProjectMember member(long projectId, long personId) {
        return ProjectMember.builder().projectId(projectId).personId(personId).build();
    }

    // ========================================================================
    // ① 正常路径：MARKET_PM actor → 200 + code=0
    // ========================================================================

    @Test
    @DisplayName("正常路径：MARKET_PM → 200 + code=0，summary 列表 + 金额单位 2 位 + 三表聚合")
    void list_returnsAggregatedSummary() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        // actor 可见项目：10, 11
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any())).thenReturn(List.of(
            project(10L, "P-001", "项目A"),
            project(11L, "P-002", "项目B")));
        // 项目 10：allowance SUM=12000 (6000+6000)
        when(allowanceLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            ledger(101L, 10L, 1L, "2026-09", "6000.00"),
            ledger(102L, 10L, 2L, "2026-09", "6000.00")));
        when(allowanceLedgerMapper.selectCount(any(Wrapper.class))).thenReturn(2L, 0L);
        // 项目 10：bonus SUM=5000；项目 11：bonus SUM=3000
        when(bonusPoolMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            pool(201L, 10L, "5000.00", "DISTRIBUTED"),
            pool(202L, 11L, "3000.00", "CONFIRMED")));
        when(bonusPoolMapper.selectCount(any(Wrapper.class))).thenReturn(1L, 1L);
        // 项目 10：score AVG=80.00 (70+90)/2；项目 11：无 score
        when(projectScoreMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            score(301L, 10L, 1L, "MARKET_PM", "70"),
            score(302L, 10L, 2L, "RD_PM", "90")));
        when(projectScoreMapper.selectCount(any(Wrapper.class))).thenReturn(2L, 0L);

        mvc.perform(get(URL_LIST)
                .param("month", "2026-09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.records[0].projectCode").value("P-001"))
            .andExpect(jsonPath("$.data.records[0].allowanceFinalAmount").value(12000.00))
            .andExpect(jsonPath("$.data.records[0].bonusFinalPool").value(5000.00))
            .andExpect(jsonPath("$.data.records[0].avgWeightedScore").value(80.00))
            .andExpect(jsonPath("$.data.records[0].allowanceRowCount").value(2))
            .andExpect(jsonPath("$.data.records[1].projectCode").value("P-002"))
            .andExpect(jsonPath("$.data.records[1].avgWeightedScore").doesNotExist());
    }

    // ========================================================================
    // ② 边界：actor 无可见项目 → 200 + code=0，total=0
    // ========================================================================

    @Test
    @DisplayName("边界：MARKET_PM 无可见项目 → 200 + code=0，total=0，records 空")
    void list_emptyScope() throws Exception {
        IpdActor actor = new IpdActor(2L, "bob", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of());

        mvc.perform(get(URL_LIST)
                .param("month", "2026-09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    // ========================================================================
    // ③ 异常：requireInternal 拒绝 → 403 + FORBIDDEN
    // ========================================================================

    @Test
    @DisplayName("异常：requireInternal 拒绝 → 403 + FORBIDDEN 业务码（真 advice 转 IpdPermissionException）")
    void list_permissionDenied_returnsForbidden() throws Exception {
        org.ruoyi.ipd.common.ApiV1ErrorCode forbidden = org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN;
        when(ipdPermission.requireInternal())
            .thenThrow(new IpdPermissionException(forbidden.getHttpStatus(), forbidden));

        mvc.perform(get(URL_LIST)
                .param("month", "2026-09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(forbidden.getCode()));
    }

    // ========================================================================
    // ④ 列表与导出同范围同筛选（共用 month+projectId）
    // ========================================================================

    @Test
    @DisplayName("④ 津贴导出：month + projectId + actor 可见范围 → 200 + rows + headers 15 列")
    void exportAllowance_returnsHeadersAndRows() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(10L, 1L)));
        when(allowanceLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            ledger(101L, 10L, 1L, "2026-09", "6000.00")));
        when(personMapper.selectBatchIds(any())).thenReturn(List.of(person(1L, "alice", "MARKET_PM", 10L)));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project(10L, "P-001", "项目A")));

        mvc.perform(get(URL_EXPORT_ALLOWANCE)
                .param("month", "2026-09")
                .param("projectId", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.exportType").value("allowance"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.headers.length()").value(15))
            .andExpect(jsonPath("$.data.headers[0]").value("月份"))
            .andExpect(jsonPath("$.data.headers[8]").value("基础额(元)"))
            .andExpect(jsonPath("$.data.headers[9]").value("终额(元)"))
            .andExpect(jsonPath("$.data.rows[0]['月份']").value("2026-09"))
            .andExpect(jsonPath("$.data.rows[0]['终额(元)']").value(6000.00))
            .andExpect(jsonPath("$.data.filters.month").value("2026-09"))
            .andExpect(jsonPath("$.data.filters.projectId").value(10));
    }

    @Test
    @DisplayName("④ 奖金导出：requireLeaderOrAdmin 拒绝 MARKET_PM → 403 FORBIDDEN")
    void exportBonus_marketPmForbidden() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireLeaderOrAdmin())
            .thenThrow(new IpdPermissionException(403, org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN));

        mvc.perform(get(URL_EXPORT_BONUS)
                .param("projectId", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("④ 奖金导出：GROUP_LEADER actor → 200 + bonus headers 14 列")
    void exportBonus_returnsHeadersAndRows() throws Exception {
        IpdActor actor = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(actor);
        // 组长可见集：personMapper(组员展开) + projectMemberMapper 两层 stub
        when(personMapper.selectList(any())).thenReturn(
            List.of(person(3L, "leader", "GROUP_LEADER", 10L)));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(10L, 3L)));
        when(bonusPoolMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            pool(201L, 10L, "5000.00", "CONFIRMED")));
        when(projectMapper.selectById(eq(10L))).thenReturn(project(10L, "P-001", "项目A"));

        mvc.perform(get(URL_EXPORT_BONUS)
                .param("projectId", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK))
            .andExpect(jsonPath("$.data.exportType").value("bonus"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.headers.length()").value(14))
            .andExpect(jsonPath("$.data.headers[0]").value("奖金池ID"))
            .andExpect(jsonPath("$.data.headers[10]").value("终池(元)"))
            .andExpect(jsonPath("$.data.rows[0]['终池(元)']").value(5000.00))
            .andExpect(jsonPath("$.data.rows[0]['状态']").value("CONFIRMED"));
    }

    // ========================================================================
    // ⑤ 大数据分页：pageSize 上限 MAX_PAGE_SIZE=500
    // ========================================================================

    @Test
    @DisplayName("⑤ 大数据分页：pageSize>500 截断到 500；totalCount 反映聚合数")
    void list_pageSizeCapped() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "SUPER_ADMIN", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        // SUPER_ADMIN 可见所有：visibleProjectIds=null
        when(projectMapper.selectList(any())).thenReturn(List.of(
            project(10L, "P-001", "项目A"),
            project(11L, "P-002", "项目B")));
        when(allowanceLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(allowanceLedgerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(bonusPoolMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(bonusPoolMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(projectScoreMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(projectScoreMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        mvc.perform(get(URL_LIST)
                .param("month", "2026-09")
                .param("pageSize", "1000")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(500))
            .andExpect(jsonPath("$.data.total").value(2));
    }

    // ========================================================================
    // ⑥ 脱敏与导出审计：导出动作落 EXPORT_REPORT 审计
    // ========================================================================

    @Test
    @DisplayName("⑥ 导出审计：津贴导出后 AuditLogService.append 被调用（EXPORT_REPORT + entityType=ALLOWANCE_LEDGER）")
    void exportAllowance_auditAppended() throws Exception {
        IpdActor actor = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(allowanceLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            ledger(101L, 10L, 1L, "2026-09", "6000.00")));
        when(personMapper.selectBatchIds(any())).thenReturn(List.of(person(1L, "alice", "MARKET_PM", 10L)));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project(10L, "P-001", "项目A")));

        mvc.perform(get(URL_EXPORT_ALLOWANCE)
                .param("month", "2026-09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<AuditLog> captor =
            org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogService, org.mockito.Mockito.atLeastOnce()).append(captor.capture());
        boolean foundExportReport = captor.getAllValues().stream()
            .anyMatch(a -> "EXPORT_REPORT".equals(a.getAction())
                && "ALLOWANCE_LEDGER".equals(a.getEntityType()));
        if (!foundExportReport) {
            throw new AssertionError("未找到 EXPORT_REPORT / ALLOWANCE_LEDGER 审计记录，实际="
                + captor.getAllValues());
        }
    }

    // ========================================================================
    // ⑥ 脱敏：列头固定中文 + 单位标识 + 行 Map 含中文 key
    // ========================================================================

    @Test
    @DisplayName("⑥ 脱敏：导出行不出现 apiKey / jwt / password 字段")
    void exportAllowance_noSensitiveFields() throws Exception {
        IpdActor actor = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);
        when(allowanceLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            ledger(101L, 10L, 1L, "2026-09", "6000.00")));
        when(personMapper.selectBatchIds(any())).thenReturn(List.of(person(1L, "alice", "MARKET_PM", 10L)));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project(10L, "P-001", "项目A")));

        mvc.perform(get(URL_EXPORT_ALLOWANCE)
                .param("month", "2026-09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rows[0].apiKey").doesNotExist())
            .andExpect(jsonPath("$.data.rows[0].jwt").doesNotExist())
            .andExpect(jsonPath("$.data.rows[0].password").doesNotExist())
            .andExpect(jsonPath("$.data.rows[0].secret").doesNotExist());
    }

    // ========================================================================
    //  ⑦ 月份格式校验：month != YYYY-MM → 400 PARAM_INVALID
    // ========================================================================

    @Test
    @DisplayName("⑦ 异常：month 格式非法 → 走 IpdBusinessException → advice 转 400 PARAM_INVALID")
    void list_invalidMonthFormat() throws Exception {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(ipdPermission.requireInternal()).thenReturn(actor);

        mvc.perform(get(URL_LIST)
                .param("month", "2026/09")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
