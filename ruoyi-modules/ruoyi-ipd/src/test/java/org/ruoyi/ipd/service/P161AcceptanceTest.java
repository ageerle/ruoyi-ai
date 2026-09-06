package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.advice.IpdServiceExceptionAdvice;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.GateElementController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-6.1 验收：Gate 要素定义管理 + 版本生命周期（页47 draft/publish/archive）。
 *
 * <p>覆盖缺口：①生命周期状态机 ②copy/revert 历史恢复 ③已发布编辑 409
 * ④vetoDualRequired 校验 ⑤thresholdJson 校验。DB 唯一索引（缺口6）由
 * 真库 DDL 探针另证（uk_gate_element_code；Mock Mapper 无法证明 DB 约束），
 * 在途评审冻结效果归 P1-6.2。Mapper/Audit 为 Mock，非网络/DB 验收。
 */
@Tag("dev")
class P161AcceptanceTest {

    private static final IpdActor ACTOR = new IpdActor(161L, "Gate管理验收", "SUPER_ADMIN", 11L);
    private static final ObjectMapper JSON = new ObjectMapper();

    private GateElementMapper mapper;
    private AuditLogService audit;
    private AuditLogMapper auditLogMapper;
    private GateElementService service;

    @BeforeAll
    static void initializeMapperMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P161"), GateElement.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(GateElementMapper.class);
        audit = mock(AuditLogService.class);
        auditLogMapper = mock(AuditLogMapper.class);
        service = new GateElementService(mapper, audit, auditLogMapper);
    }

    // ---------- 缺口⑤：thresholdJson 校验 ----------

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "[]", "null", "3", "{\"a\":\"字符串\"}", "{\"a\":1.5}", "{\"a\":true}"})
    void invalidThresholdJsonRejectedOnCreate(String raw) {
        assertInvalid(() -> service.create(draft().setThresholdJson(raw), ACTOR));
        verifyNoInteractions(mapper, audit);
    }

    @Test
    void thresholdJsonValueAndSizeBoundsEnforced() {
        assertInvalid(() -> service.create(draft().setThresholdJson("{\"k\":\"" + "字".repeat(300) + "\"}"), ACTOR));
        assertInvalid(() -> service.create(draft().setThresholdJson("{\"k\":" + "1".repeat(520) + "}"), ACTOR));
        verifyNoInteractions(mapper, audit);
    }

    @Test
    void validThresholdJsonKeptAndBlankNormalizedToNull() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement e = inv.getArgument(0);
            e.setId(31L);
            return 1;
        });
        assertThat(service.create(draft().setThresholdJson("{\"minCustomerVerifications\":3}"), ACTOR)
            .getThresholdJson()).contains("minCustomerVerifications");
        assertThat(service.create(draft().setThresholdJson("   "), ACTOR).getThresholdJson()).isNull();
    }

    // ---------- 缺口④：vetoDualRequired 校验 ----------

    @Test
    void dualVetoFlagRequiresVetoElement() {
        assertInvalid(() -> service.create(draft().setIsVeto("0").setVetoDualRequired("1"), ACTOR));
        verifyNoInteractions(mapper, audit);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(GateElement.class))).thenReturn(1);
        GateElement ok = service.create(draft().setIsVeto("1").setVetoDualRequired("1"), ACTOR);
        assertThat(ok.getIsVeto()).isEqualTo("1");
        assertThat(ok.getVetoDualRequired()).isEqualTo("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "true", "yes"})
    void invalidDualVetoFlagRejected(String flag) {
        when(mapper.selectById(1L)).thenReturn(
            draft().setId(1L).setStatus("draft").setEnabled("0").setIsVeto("1"));
        assertInvalid(() -> service.create(draft().setVetoDualRequired(flag), ACTOR));
        assertInvalid(() -> service.update(GateElement.builder().id(1L).vetoDualRequired(flag).build(), ACTOR));
        verify(mapper, never()).insert(any(GateElement.class));
        verify(mapper, never()).updateById(any(GateElement.class));
        verifyNoInteractions(audit);
    }

    // ---------- 基础校验（编码/枚举/长度，防止生命周期改动破坏原契约） ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abcdefghijklmnopq"})
    void invalidCodesRejectWithoutWriting(String code) {
        assertInvalid(() -> service.create(draft().setElementCode(code), ACTOR));
        verifyNoInteractions(mapper, audit);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"G0", "G6", "g1", " G1 "})
    void invalidCreateGatesRejectWithoutWriting(String gate) {
        assertInvalid(() -> service.create(draft().setGateCode(gate), ACTOR));
        verifyNoInteractions(mapper, audit);
    }

    @Test
    void storageLengthsAndSortOrderBoundsEnforcedBeforeMapperWrites() {
        assertInvalid(() -> service.create(draft().setElementName("字".repeat(129)), ACTOR));
        assertInvalid(() -> service.create(draft().setPassStandard("中".repeat(21846)), ACTOR));
        assertInvalid(() -> service.create(draft().setSortOrder(-1), ACTOR));
        assertInvalid(() -> service.create(draft().setSortOrder(10000), ACTOR));
        verifyNoInteractions(mapper, audit);
    }

    // ---------- 缺口①：生命周期状态机 draft/publish/archive ----------

    @Test
    void createAlwaysLandsAsInvisibleDraftWithZeroVersion() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement e = inv.getArgument(0);
            e.setId(32L);
            return 1;
        });
        GateElement created = service.create(draft().setIsVeto("1").setEnabled("1"), ACTOR);
        assertThat(created.getStatus()).isEqualTo("draft");
        assertThat(created.getVersion()).isZero();
        assertThat(created.getEnabled()).isEqualTo("0");
        ArgumentCaptor<AuditLog> event = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("CREATE");
        assertThat(event.getValue().getOperatorId()).isEqualTo(ACTOR.id());
        assertThat(event.getValue().getOperatorRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void duplicateCodeConflictsWithoutWrite() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertBusinessError(() -> service.create(draft(), ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        verify(mapper, never()).insert(any(GateElement.class));
        verifyNoInteractions(audit);
    }

    @Test
    void publishTurnsDraftVisibleAndBumpsVersion() {
        GateElement exist = draft().setId(1L).setStatus("draft").setVersion(0).setEnabled("0");
        when(mapper.selectById(1L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        GateElement out = service.publish(1L, ACTOR);
        assertThat(out.getStatus()).isEqualTo("published");
        assertThat(out.getEnabled()).isEqualTo("1");
        assertThat(out.getVersion()).isEqualTo(1);
        ArgumentCaptor<AuditLog> event = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("PUBLISH");
        assertThat(readPath(event.getValue().getBeforeData(), "enabled")).isEqualTo("0");
        assertThat(readPath(event.getValue().getAfterData(), "enabled")).isEqualTo("1");
    }

    @Test
    void publishOnlyFromDraft() {
        when(mapper.selectById(9L)).thenReturn(draft().setId(9L).setStatus("published").setEnabled("1"));
        assertBusinessError(() -> service.publish(9L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        when(mapper.selectById(10L)).thenReturn(draft().setId(10L).setStatus("archived").setEnabled("0"));
        assertBusinessError(() -> service.publish(10L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        when(mapper.selectById(404L)).thenReturn(null);
        assertBusinessError(() -> service.publish(404L, ACTOR), ApiV1ErrorCode.NOT_FOUND);
        verify(mapper, never()).updateById(any(GateElement.class));
        verifyNoInteractions(audit);
    }

    @Test
    void archiveIsTerminalAndDisablesVisibility() {
        GateElement exist = draft().setId(1L).setStatus("published").setEnabled("1").setVersion(2);
        when(mapper.selectById(1L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        GateElement out = service.archive(1L, ACTOR);
        assertThat(out.getStatus()).isEqualTo("archived");
        assertThat(out.getEnabled()).isEqualTo("0");
        assertBusinessError(() -> service.archive(1L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        assertBusinessError(() -> service.publish(1L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    void listByGateFiltersEnabledWithStableOrderAndValidatesGate() {
        assertInvalid(() -> service.listByGate("G9"));
        verify(mapper, never()).selectList(any(LambdaQueryWrapper.class));
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertThat(service.listByGate("G1")).isEmpty();
        ArgumentCaptor<LambdaQueryWrapper<GateElement>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("enabled", "sort_order ASC");
    }

    // ---------- 缺口③：已发布编辑 409（定义 vs 启停分界） ----------

    @Test
    void publishedDefinitionEditConflictsWhileEnabledToggleAllowed() {
        GateElement exist = draft().setId(1L).setStatus("published").setEnabled("1").setVersion(1)
            .setPassStandard("原标准").setIsVeto("1");
        when(mapper.selectById(1L)).thenReturn(exist);
        for (GateElement patch : new GateElement[]{
            GateElement.builder().id(1L).elementName("改名").build(),
            GateElement.builder().id(1L).passStandard("改标准").build(),
            GateElement.builder().id(1L).isVeto("0").build(),
            GateElement.builder().id(1L).sortOrder(9).build(),
            GateElement.builder().id(1L).vetoDualRequired("1").build(),
            GateElement.builder().id(1L).thresholdJson("{\"k\":1}").build()}) {
            assertBusinessError(() -> service.update(patch, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        }
        verify(mapper, never()).updateById(any(GateElement.class));
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        assertThat(service.update(GateElement.builder().id(1L).enabled("0").build(), ACTOR).getEnabled())
            .isEqualTo("0");
        assertThat(exist.getElementName()).isEqualTo("验收标准名称");
    }

    @Test
    void archivedRejectsAnyUpdateAndDraftRejectsEnabledPatch() {
        when(mapper.selectById(1L)).thenReturn(draft().setId(1L).setStatus("archived").setEnabled("0"));
        assertBusinessError(() -> service.update(GateElement.builder().id(1L).elementName("x").build(), ACTOR),
            ApiV1ErrorCode.STATE_CONFLICT);
        when(mapper.selectById(2L)).thenReturn(draft().setId(2L).setStatus("draft").setEnabled("0"));
        assertBusinessError(() -> service.update(GateElement.builder().id(2L).enabled("1").build(), ACTOR),
            ApiV1ErrorCode.STATE_CONFLICT);
        verify(mapper, never()).updateById(any(GateElement.class));
    }

    @Test
    void identityFieldsImmutableOnUpdate() {
        GateElement exist = draft().setId(1L).setStatus("draft").setEnabled("0");
        when(mapper.selectById(1L)).thenReturn(exist);
        assertBusinessError(() -> service.update(
            GateElement.builder().id(1L).gateCode("G2").elementName("不应写入").build(), ACTOR),
            ApiV1ErrorCode.STATE_CONFLICT);
        assertBusinessError(() -> service.update(
            GateElement.builder().id(1L).elementCode("other").build(), ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        assertThat(exist.getGateCode()).isEqualTo("G1");
        assertThat(exist.getElementName()).isEqualTo("验收标准名称");
        verify(mapper, never()).updateById(any(GateElement.class));
    }

    @Test
    void invalidPatchRejectedWithoutMutatingReadDefinitionOrAuditing() {
        GateElement exist = draft().setId(1L).setStatus("draft").setEnabled("0").setIsVeto("1")
            .setPassStandard("原标准");
        when(mapper.selectById(1L)).thenReturn(exist);
        // 生命周期守卫（409）优先于值校验（400）是刻意设计：先判可改性再判值；
        // 本用例只隔离值校验，故补丁不带 enabled，避免提前落入草稿启停 409
        assertInvalid(() -> service.update(
            GateElement.builder().id(1L).elementName(" ").build(), ACTOR));
        assertInvalid(() -> service.update(
            GateElement.builder().id(1L).elementName("新名").thresholdJson("bad").build(), ACTOR));
        assertThat(exist.getElementName()).isEqualTo("验收标准名称");
        assertThat(exist.getThresholdJson()).isNull();
        verify(mapper, never()).updateById(any(GateElement.class));
        verifyNoInteractions(audit);
    }

    @Test
    void missingOrInvalidIdsUseNotFoundOrParamErrorWithoutWriting() {
        when(mapper.selectById(404L)).thenReturn(null);
        assertBusinessError(() -> service.update(GateElement.builder().id(404L).enabled("0").build(), ACTOR),
            ApiV1ErrorCode.NOT_FOUND);
        assertBusinessError(() -> service.disable(404L, ACTOR), ApiV1ErrorCode.NOT_FOUND);
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertInvalid(() -> service.update(GateElement.builder().id(id).enabled("0").build(), ACTOR));
            assertInvalid(() -> service.disable(id, ACTOR));
            assertInvalid(() -> service.publish(id, ACTOR));
        }
        verify(mapper, never()).updateById(any(GateElement.class));
        verify(mapper, never()).insert(any(GateElement.class));
        verifyNoInteractions(audit);
    }

    @Test
    void disableRemainsAvailableForPublishedAndDraftButNotArchived() {
        GateElement published = draft().setId(1L).setStatus("published").setEnabled("1");
        when(mapper.selectById(1L)).thenReturn(published);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        assertThat(service.disable(1L, ACTOR).getEnabled()).isEqualTo("0");
        when(mapper.selectById(2L)).thenReturn(draft().setId(2L).setStatus("archived").setEnabled("0"));
        assertBusinessError(() -> service.disable(2L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
    }

    // ---------- 缺口②：copy / revert 历史恢复 ----------

    @Test
    void copyClonesDefinitionIntoNewDraftEvenFromArchived() {
        GateElement source = draft().setId(1L).setStatus("archived").setEnabled("0").setIsVeto("1")
            .setVetoDualRequired("1").setSortOrder(4)
            .setPassStandard("原标准").setThresholdJson("{\"minCustomerVerifications\":3}");
        when(mapper.selectById(1L)).thenReturn(source);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement e = inv.getArgument(0);
            e.setId(50L);
            return 1;
        });
        GateElement clone = service.copy(1L, "G1-1b", ACTOR);
        assertThat(clone.getStatus()).isEqualTo("draft");
        assertThat(clone.getVersion()).isZero();
        assertThat(clone.getEnabled()).isEqualTo("0");
        assertThat(clone.getElementCode()).isEqualTo("G1-1b");
        assertThat(clone.getPassStandard()).isEqualTo("原标准");
        assertThat(clone.getIsVeto()).isEqualTo("1");
        assertThat(clone.getVetoDualRequired()).isEqualTo("1");
        assertThat(clone.getThresholdJson()).contains("minCustomerVerifications");
        ArgumentCaptor<AuditLog> event = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("COPY");
    }

    @Test
    void copyRejectsMissingSourceAndDuplicateTargetCode() {
        when(mapper.selectById(404L)).thenReturn(null);
        assertBusinessError(() -> service.copy(404L, "X", ACTOR), ApiV1ErrorCode.NOT_FOUND);
        when(mapper.selectById(1L)).thenReturn(draft().setId(1L).setStatus("published"));
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertBusinessError(() -> service.copy(1L, "G1-1", ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        assertInvalid(() -> service.copy(1L, " ", ACTOR));
        verify(mapper, never()).insert(any(GateElement.class));
    }

    @Test
    void revertRestoresDraftDefinitionFromAuditBeforeSnapshot() {
        GateElement exist = draft().setId(1L).setStatus("draft").setEnabled("0")
            .setElementName("改坏的名字").setIsVeto("0");
        when(mapper.selectById(1L)).thenReturn(exist);
        AuditLog history = AuditLog.builder().id(88L).entityType("GATE_ELEMENT").entityId(1L)
            .action("UPDATE")
            .beforeData(AuditEventData.json(
                "gateCode", "G1", "elementCode", "custom-1", "elementName", "验收标准名称",
                "passStandard", "原标准", "isVeto", "1", "sortOrder", 4, "enabled", "0",
                "status", "draft", "version", 0, "vetoDualRequired", "1",
                "thresholdJson", "{\"minCustomerVerifications\":3}"))
            .build();
        when(auditLogMapper.selectById(88L)).thenReturn(history);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        GateElement out = service.revert(1L, 88L, ACTOR);
        assertThat(out.getElementName()).isEqualTo("验收标准名称");
        assertThat(out.getPassStandard()).isEqualTo("原标准");
        assertThat(out.getIsVeto()).isEqualTo("1");
        assertThat(out.getSortOrder()).isEqualTo(4);
        assertThat(out.getVetoDualRequired()).isEqualTo("1");
        assertThat(out.getThresholdJson()).contains("minCustomerVerifications");
        ArgumentCaptor<AuditLog> event = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).append(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("REVERT");
        assertThat(readPath(event.getValue().getBeforeData(), "elementName")).isEqualTo("改坏的名字");
    }

    @Test
    void revertOnlyAppliesToDraftsWithValidMatchingAuditSnapshot() {
        when(mapper.selectById(1L)).thenReturn(draft().setId(1L).setStatus("published").setEnabled("1"));
        assertBusinessError(() -> service.revert(1L, 88L, ACTOR), ApiV1ErrorCode.STATE_CONFLICT);
        when(mapper.selectById(2L)).thenReturn(draft().setId(2L).setStatus("draft").setEnabled("0"));
        when(auditLogMapper.selectById(404L)).thenReturn(null);
        assertBusinessError(() -> service.revert(2L, 404L, ACTOR), ApiV1ErrorCode.NOT_FOUND);
        when(auditLogMapper.selectById(89L)).thenReturn(AuditLog.builder().id(89L)
            .entityType("PERSON").entityId(2L).beforeData("{}").build());
        assertBusinessError(() -> service.revert(2L, 89L, ACTOR), ApiV1ErrorCode.PARAM_INVALID);
        when(auditLogMapper.selectById(90L)).thenReturn(AuditLog.builder().id(90L)
            .entityType("GATE_ELEMENT").entityId(2L)
            .beforeData(AuditEventData.json("detail", "legacy-row-without-snapshot")).build());
        assertBusinessError(() -> service.revert(2L, 90L, ACTOR), ApiV1ErrorCode.PARAM_INVALID);
        assertInvalid(() -> service.revert(2L, null, ACTOR));
        verify(mapper, never()).updateById(any(GateElement.class));
    }

    // ---------- Controller 层（真实 Service/权限/DTO/Advice；仅会话与 Mapper 为 Mock） ----------

    @Test
    void controllerLifecycleHappyPathThroughApiEnvelope() throws Exception {
        GateElement element = draft().setId(1L).setStatus("draft").setVersion(0).setEnabled("0");
        when(mapper.selectById(1L)).thenReturn(element);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(call -> {
            GateElement self = mapper.selectById(1L);
            return "published".equals(self.getStatus()) && "1".equals(self.getEnabled())
                ? List.of(self) : List.of();
        });
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement e = inv.getArgument(0);
            e.setId(77L);
            return 1;
        });
        MockMvc mvc = controller("SUPER_ADMIN");
        mvc.perform(post("/api/v1/gate-elements/1/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("published"))
            .andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(get("/api/v1/gate-elements").param("gate", "G1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].elementCode").value("custom-1"));
        mvc.perform(post("/api/v1/gate-elements/1/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"elementName\":\"改名\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(50002));
        mvc.perform(post("/api/v1/gate-elements/1/disable"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value("0"));
        mvc.perform(post("/api/v1/gate-elements/1/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":\"1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value("1"));
        mvc.perform(post("/api/v1/gate-elements/1/archive"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("archived"));
        mvc.perform(get("/api/v1/gate-elements").param("gate", "G1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(post("/api/v1/gate-elements/1/copy").param("newElementCode", "custom-1b"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("draft"))
            .andExpect(jsonPath("$.data.elementCode").value("custom-1b"));
        mvc.perform(post("/api/v1/gate-elements/1/publish"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(50002));
    }

    @Test
    void controllerCreateInvalidFlagReturns400AndWriteFailureReturns500WithoutSuccessAudit() throws Exception {
        when(mapper.insert(any(GateElement.class))).thenReturn(0);
        controller("SUPER_ADMIN").perform(post("/api/v1/gate-elements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gateCode\":\"G1\",\"elementCode\":\"custom-1\",\"elementName\":\"验收标准名称\",\"isVeto\":\"true\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(10001));
        verifyNoInteractions(audit);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        controller("SUPER_ADMIN").perform(post("/api/v1/gate-elements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gateCode\":\"G1\",\"elementCode\":\"custom-1\",\"elementName\":\"验收标准名称\"}"))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(90001));
        verifyNoInteractions(audit);
    }

    @Test
    void controllerRevertMissingAuditReturns404() throws Exception {
        when(mapper.selectById(1L)).thenReturn(draft().setId(1L).setStatus("draft").setEnabled("0"));
        when(auditLogMapper.selectById(404L)).thenReturn(null);
        controller("SUPER_ADMIN").perform(post("/api/v1/gate-elements/1/revert")
                .param("auditLogId", "404"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(50001));
        verify(mapper, never()).updateById(any(GateElement.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MARKET_PM", "RD_PM", "GROUP_LEADER"})
    void nonAdministratorsGet403OnEveryLifecycleEndpointWithoutTouchingMapper(String role) throws Exception {
        // GET 列表是内部角色可读（requireInternal），超管限定由生产侧 Sa-Token 注解实现，
        // standalone MockMvc 不解析注解，故此处只验收写端点的 requireAdmin 403。
        MockMvc mvc = controller(role);
        mvc.perform(post("/api/v1/gate-elements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gateCode\":\"G1\",\"elementCode\":\"x1\",\"elementName\":\"名称\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        mvc.perform(post("/api/v1/gate-elements/1/publish"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        mvc.perform(post("/api/v1/gate-elements/1/archive"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        mvc.perform(post("/api/v1/gate-elements/1/copy").param("newElementCode", "x2"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        mvc.perform(post("/api/v1/gate-elements/1/revert").param("auditLogId", "1"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        mvc.perform(post("/api/v1/gate-elements/1/disable"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        verifyNoInteractions(mapper, audit);
    }

    // ---------- helpers ----------

    private MockMvc controller(String role) {
        Person person = Person.builder().id(ACTOR.id()).name(ACTOR.name()).personType(role)
            .groupId(ACTOR.groupId()).accountStatus("ACTIVE").employmentStatus("ACTIVE")
            .mustChangePwd("0").delFlag("0").build();
        IpdAuthSession session = mock(IpdAuthSession.class);
        when(session.currentPerson()).thenReturn(person);
        IpdPermission permission = new IpdPermission(session,
            new IpdAuthService(mock(PersonMapper.class), audit));
        return MockMvcBuilders.standaloneSetup(new GateElementController(service, permission))
            .setControllerAdvice(new IpdPermissionExceptionHandler(), new IpdServiceExceptionAdvice())
            .build();
    }

    private static GateElement draft() {
        return GateElement.builder().gateCode("G1").elementCode("custom-1").elementName("验收标准名称").build();
    }

    private static String readPath(String payload, String field) {
        try {
            return JSON.readTree(payload).path(field).asText();
        } catch (Exception e) {
            throw new AssertionError("审计载荷非合法 JSON", e);
        }
    }

    private static void assertInvalid(Runnable call) {
        assertBusinessError(call, ApiV1ErrorCode.PARAM_INVALID);
    }

    private static void assertBusinessError(Runnable call, ApiV1ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(code);
    }
}
