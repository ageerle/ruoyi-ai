package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P1-3.1真实Bootstrap算法验收；Mapper按项目参数读取插入后的内存状态。
 * 本类证明图构造、拒绝和同进程重复调用；MANDATORY仅作声明检查，不宣称真实事务回滚或并发幂等。
 */
@Tag("dev")
class P131AcceptanceTest {
    private Fixture f;

    @BeforeEach
    void setup() { f = new Fixture(); }

    @Test
    void bootstrapInsertsExactlySixStagesInCanonicalOrder() {
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isEqualTo(6);
        assertThat(f.stages).extracting(ProjectStage::getStageCode)
            .containsExactly("CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE");
        assertThat(f.stages).extracting(ProjectStage::getSortOrder).containsExactly(10, 20, 30, 40, 50, 60);
        assertThat(f.stages).extracting(ProjectStage::getId).doesNotHaveDuplicates();
        assertThat(f.stages).allSatisfy(stage -> {
            assertThat(stage.getId()).isPositive();
            assertThat(stage.getProjectId()).isEqualTo(13L);
            assertThat(stage.getTenantId()).isEqualTo("000000");
            assertThat(stage.getDelFlag()).isEqualTo("0");
            assertThat(stage.getStatus()).isEqualTo("NOT_STARTED");
            assertThat(stage.getCreateBy()).isEqualTo(7L);
            assertThat(stage.getUpdateBy()).isEqualTo(7L);
        });
        verify(f.stageMapper).insertBatch(anyList(), anyInt());
    }

    @Test
    void bootstrapInsertsExactlySixtyNineActionsAcrossAllStages() {
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isEqualTo(6);
        assertThat(f.actions).hasSize(69);
        assertThat(f.actions).extracting(StageAction::getActionCode)
            .containsExactlyInAnyOrderElementsOf(ActionCatalog.ALL.stream().map(ActionDef::code).toList());
        assertThat(f.actions).extracting(StageAction::getId).doesNotHaveDuplicates();
        assertThat(f.actions).filteredOn(a -> "DEEP".equals(a.getDepth())).hasSize(42);
        assertThat(f.actions).filteredOn(a -> "LIGHT".equals(a.getDepth())).hasSize(27);
        assertThat(f.actions).allSatisfy(action -> {
            assertThat(action.getId()).isPositive();
            assertThat(action.getProjectId()).isEqualTo(13L);
            ActionDef def = ActionCatalog.byCode(action.getActionCode());
            boolean applicable = ActionCatalog.applicableTo(def, "HARDWARE", null);
            assertThat(action.getStatus()).isEqualTo(applicable ? "NOT_STARTED" : "NA");
            assertThat(action.getCreateBy()).isEqualTo(7L);
            assertThat(action.getUpdateBy()).isEqualTo(7L);
        });
        // HARDWARE + 无海外市场：SW / SOL(除V11) / OVERSEAS → NA；硬件专属仍 NOT_STARTED
        assertThat(f.actions).filteredOn(a -> "NA".equals(a.getStatus()))
            .extracting(StageAction::getActionCode)
            .contains("P05", "D04", "V04", "P06", "D10", "V09", "C04", "V12")
            .doesNotContain("D03", "D07", "V05", "V11");
        verify(f.actionMapper).insertBatch(anyList(), anyInt());
    }

    @Test
    void bootstrapAssignsEachActionToTheCorrectStage() {
        f.bootstrap.bootstrap(13L, 7L);
        Map<Long, ProjectStage> byId = new LinkedHashMap<>();
        f.stages.forEach(stage -> byId.put(stage.getId(), stage));
        Map<String, Integer> stageSizes = new LinkedHashMap<>();
        for (StageAction action : f.actions) {
            ProjectStage stage = byId.get(action.getStageId());
            assertThat(stage).as(action.getActionCode()).isNotNull();
            assertThat(stage.getProjectId()).isEqualTo(action.getProjectId());
            assertThat(stage.getStageCode()).isEqualTo(ActionCatalog.byCode(action.getActionCode()).stage());
            stageSizes.merge(stage.getStageCode(), 1, Integer::sum);
        }
        assertThat(stageSizes).containsExactlyInAnyOrderEntriesOf(Map.of(
            "CONCEPT", 12, "PLAN", 13, "DEV", 11, "VALID", 12, "LAUNCH", 8, "LIFECYCLE", 13));
    }

    @Test
    void blockingAndBioFeatureFlagsArePropagatedFromCatalog() {
        f.bootstrap.bootstrap(13L, 7L);
        for (StageAction action : f.actions) {
            ActionDef def = ActionCatalog.byCode(action.getActionCode());
            assertThat(action.getOwnerRole()).isEqualTo(def.ownerRole());
            assertThat(action.getIsBlocking()).isEqualTo(def.blocking() ? "1" : "0");
            assertThat(action.getIsBioFeature()).isEqualTo(def.bioFeature() ? "1" : "0");
        }
        assertThat(f.actions).filteredOn(a -> "1".equals(a.getIsBlocking())).hasSize(38);
        assertThat(f.actions).filteredOn(a -> "1".equals(a.getIsBioFeature()))
            .extracting(StageAction::getActionCode).containsExactlyInAnyOrder("C12", "D11", "V10");
    }

    @Test
    void bootstrapPropagatesStageMapperFailureWithoutSwallowingIt() {
        RuntimeException failure = new RuntimeException("stage DB constraint");
        f.failure = failure;
        f.stageThrowAt = 1;
        assertThatThrownBy(() -> f.bootstrap.bootstrap(13L, 7L)).isSameAs(failure);
        assertThat(f.stageAttempts).isEqualTo(1);
        verify(f.actionMapper, never()).insertBatch(anyList(), anyInt());
    }

    @Test
    void bootstrapOnDifferentProjectsIsIndependent() {
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isEqualTo(6);
        assertThat(f.bootstrap.bootstrap(99L, 7L)).isEqualTo(6);
        assertThat(f.stages).filteredOn(s -> s.getProjectId().equals(13L)).hasSize(6);
        assertThat(f.stages).filteredOn(s -> s.getProjectId().equals(99L)).hasSize(6);
        assertThat(f.actions).filteredOn(a -> a.getProjectId().equals(13L)).hasSize(69);
        assertThat(f.actions).filteredOn(a -> a.getProjectId().equals(99L)).hasSize(69);
        verify(f.stageMapper, times(2)).insertBatch(anyList(), anyInt());
        verify(f.actionMapper, times(2)).insertBatch(anyList(), anyInt());
    }

    @Test
    void completeGraphIsReadBackAndRepeatedBootstrapAddsNothing() {
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isEqualTo(6);
        List<Long> stageIds = f.stages.stream().map(ProjectStage::getId).toList();
        List<Long> actionIds = f.actions.stream().map(StageAction::getId).toList();
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isZero();
        assertThat(f.stages).extracting(ProjectStage::getId).containsExactlyElementsOf(stageIds);
        assertThat(f.actions).extracting(StageAction::getId).containsExactlyElementsOf(actionIds);
        verify(f.stageMapper, times(2)).selectProjectForBootstrap(13L);
        verify(f.stageMapper, times(2)).selectLiveByProject(13L);
        verify(f.stageMapper, times(2)).selectActionsForBootstrap(13L);
        verify(f.stageMapper, times(2)).selectAllStageIdsForBootstrap(13L);
        verify(f.stageMapper, times(2)).selectAllActionIdsForBootstrap(13L);
        verify(f.stageMapper).insertBatch(anyList(), anyInt());
        verify(f.actionMapper).insertBatch(anyList(), anyInt());
    }

    @Test
    void independentlySeededCompleteGraphIsAlsoANoOp() {
        f.seedComplete(13L);
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isZero();
        f.assertNoWrites();
    }

    @Test
    void solutionTemplateDeepensV11WithoutChangingOtherCatalogDepths() {
        f.projects.get(13L).setTemplateType("SOLUTION");
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isEqualTo(6);
        assertThat(f.actions).hasSize(69);
        assertThat(f.actions).filteredOn(a -> "DEEP".equals(a.getDepth())).hasSize(43);
        assertThat(f.actions).filteredOn(a -> "LIGHT".equals(a.getDepth())).hasSize(26);
        for (StageAction action : f.actions) {
            ActionDef def = ActionCatalog.byCode(action.getActionCode());
            assertThat(action.getDepth()).as(action.getActionCode())
                .isEqualTo("V11".equals(def.code()) ? "DEEP" : def.depth());
        }
        assertThat(f.bootstrap.bootstrap(13L, 7L)).isZero();
        verify(f.actionMapper).insertBatch(anyList(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZERO_ROWS", "MULTIPLE_ROWS", "MISSING_ID", "ZERO_ID", "NEGATIVE_ID", "DUPLICATE_ID"})
    void invalidStageInsertResultOrGeneratedIdIsAPersistenceFailure(String fault) {
        f.stageFault = fault;
        failureCode(() -> f.bootstrap.bootstrap(13L, 7L), ApiV1ErrorCode.INTERNAL_ERROR);
        // 批量契约下，批量返回值与坏主键回填都在同一次（且仅一次）阶段批量写入中暴露；动作批量不应被触达
        verify(f.stageMapper, times(1)).insertBatch(anyList(), anyInt());
        verify(f.actionMapper, never()).insertBatch(anyList(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZERO_ROWS", "MULTIPLE_ROWS", "MISSING_ID", "ZERO_ID", "NEGATIVE_ID", "DUPLICATE_ID"})
    void invalidActionInsertResultOrGeneratedIdIsAPersistenceFailure(String fault) {
        f.actionFault = fault;
        failureCode(() -> f.bootstrap.bootstrap(13L, 7L), ApiV1ErrorCode.INTERNAL_ERROR);
        // 阶段批量已成功一次；动作批量同样只发生一次，返回值伪回执/坏回填即整体失败
        verify(f.stageMapper, times(1)).insertBatch(anyList(), anyInt());
        verify(f.actionMapper, times(1)).insertBatch(anyList(), anyInt());
    }

    @Test
    void middleStageFailureEscapesAndStopsFurtherInserts() {
        RuntimeException failure = new RuntimeException("third stage failure");
        f.failure = failure;
        f.stageThrowAt = 3;
        assertThatThrownBy(() -> f.bootstrap.bootstrap(13L, 7L)).isSameAs(failure);
        assertThat(f.stageAttempts).isEqualTo(3);
        // Mapper替身保留已写内存行；实际事务回滚须由独立真实数据库验收证明。
        assertThat(f.stages).hasSize(2);
    }

    @Test
    void middleActionFailureEscapesAndStopsFurtherInserts() {
        RuntimeException failure = new RuntimeException("thirty-fifth action failure");
        f.failure = failure;
        f.actionThrowAt = 35;
        assertThatThrownBy(() -> f.bootstrap.bootstrap(13L, 7L)).isSameAs(failure);
        assertThat(f.actionAttempts).isEqualTo(35);
        assertThat(f.actions).hasSize(34);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_STAGE", "MISSING_ACTION", "STAGES_ONLY", "ACTIONS_ONLY",
        "OWNER", "DEPTH", "STAGE_BINDING", "DUPLICATE_STAGE_CODE", "DUPLICATE_ACTION_CODE", "SOLUTION_LIGHT",
        "HIDDEN_STAGE", "HIDDEN_ACTION"})
    void partialOrCorruptExistingGraphIsAConflictWithNoAdditionalWrites(String corruption) {
        f.seedComplete(13L);
        switch (corruption) {
            case "MISSING_STAGE" -> f.stages.remove(0);
            case "MISSING_ACTION" -> f.actions.remove(0);
            case "STAGES_ONLY" -> f.actions.clear();
            case "ACTIONS_ONLY" -> f.stages.clear();
            case "OWNER" -> f.actions.get(0).setOwnerRole("RD_PM");
            case "DEPTH" -> f.actions.get(0).setDepth("LIGHT");
            case "STAGE_BINDING" -> f.actions.get(0).setStageId(f.stages.get(1).getId());
            case "DUPLICATE_STAGE_CODE" -> f.stages.get(1).setStageCode("CONCEPT");
            case "DUPLICATE_ACTION_CODE" -> f.actions.get(1).setActionCode("C01");
            case "SOLUTION_LIGHT" -> f.projects.get(13L).setTemplateType("SOLUTION");
            case "HIDDEN_STAGE" -> f.stages.get(0).setTenantId("999999");
            case "HIDDEN_ACTION" -> f.hiddenActionIds.add(f.actions.get(0).getId());
            default -> throw new IllegalArgumentException(corruption);
        }
        failureCode(() -> f.bootstrap.bootstrap(13L, 7L), ApiV1ErrorCode.STATE_CONFLICT);
        f.assertNoWrites();
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    void invalidProjectOrOperatorIdIsRejectedBeforeMapperAccess(Long projectId, Long operatorId) {
        failureCode(() -> f.bootstrap.bootstrap(projectId, operatorId), ApiV1ErrorCode.PARAM_INVALID);
        verifyNoInteractions(f.stageMapper, f.actionMapper);
    }

    static Stream<Arguments> invalidParameters() {
        return Stream.of(Arguments.of(null, 7L), Arguments.of(0L, 7L), Arguments.of(-1L, 7L),
            Arguments.of(13L, null), Arguments.of(13L, 0L), Arguments.of(13L, -1L));
    }

    @Test
    void missingProjectIsNotFoundAndDoesNotCreateAnOrphanGraph() {
        failureCode(() -> f.bootstrap.bootstrap(404L, 7L), ApiV1ErrorCode.NOT_FOUND);
        verify(f.stageMapper).selectProjectForBootstrap(404L);
        verify(f.stageMapper, never()).selectLiveByProject(anyLong());
        verifyNoInteractions(f.actionMapper);
        f.assertNoWrites();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "UNKNOWN"})
    void invalidProjectTemplateIsRejectedBeforeAnyChildGraphQuery(String template) {
        f.projects.get(13L).setTemplateType(template);
        failureCode(() -> f.bootstrap.bootstrap(13L, 7L), ApiV1ErrorCode.STATE_CONFLICT);
        verify(f.stageMapper).selectProjectForBootstrap(13L);
        verify(f.stageMapper, never()).selectLiveByProject(anyLong());
        verify(f.stageMapper, never()).selectActionsForBootstrap(anyLong());
        verify(f.stageMapper, never()).selectAllStageIdsForBootstrap(anyLong());
        verify(f.stageMapper, never()).selectAllActionIdsForBootstrap(anyLong());
        verifyNoInteractions(f.actionMapper);
        f.assertNoWrites();
    }

    @Test
    void bootstrapDeclaresMandatoryTransactionWithoutClaimingRuntimeRollback() throws Exception {
        Transactional boundary = ProjectBootstrapService.class.getMethod("bootstrap", Long.class, Long.class)
            .getAnnotation(Transactional.class);
        assertThat(boundary).isNotNull();
        assertThat(boundary.propagation()).isEqualTo(Propagation.MANDATORY);
    }

    static void failureCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ApiV1ErrorCode code) {
        ServiceException failure = catchThrowableOfType(action, ServiceException.class);
        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo(code.getCode());
    }

    static final class Fixture {
        final ProjectStageMapper stageMapper = mock(ProjectStageMapper.class);
        final StageActionMapper actionMapper = mock(StageActionMapper.class);
        final ProjectBootstrapService bootstrap = new ProjectBootstrapService(stageMapper, actionMapper);
        final Map<Long, Project> projects = new LinkedHashMap<>();
        final List<ProjectStage> stages = new ArrayList<>();
        final List<StageAction> actions = new ArrayList<>();
        final Set<Long> hiddenActionIds = new HashSet<>();
        long nextStageId = 1000, nextActionId = 10000;
        int stageAttempts, actionAttempts, stageThrowAt, actionThrowAt;
        String stageFault = "", actionFault = "";
        RuntimeException failure;

        Fixture() {
            projects.put(13L, project(13L));
            projects.put(99L, project(99L));
            when(stageMapper.selectProjectForBootstrap(anyLong())).thenAnswer(inv -> {
                Project project = projects.get(inv.<Long>getArgument(0));
                return project != null && "000000".equals(project.getTenantId()) && "0".equals(project.getDelFlag())
                    ? project : null;
            });
            when(stageMapper.selectLiveByProject(anyLong())).thenAnswer(inv -> {
                Long projectId = inv.getArgument(0);
                return stages.stream().filter(s -> projectId.equals(s.getProjectId()))
                    .filter(s -> "000000".equals(s.getTenantId()) && "0".equals(s.getDelFlag())).toList();
            });
            when(stageMapper.selectActionsForBootstrap(anyLong())).thenAnswer(inv -> {
                Long projectId = inv.getArgument(0);
                return actions.stream().filter(a -> projectId.equals(a.getProjectId()))
                    .filter(a -> !hiddenActionIds.contains(a.getId())).toList();
            });
            when(stageMapper.selectAllStageIdsForBootstrap(anyLong())).thenAnswer(inv -> {
                Long projectId = inv.getArgument(0);
                return stages.stream().filter(s -> projectId.equals(s.getProjectId())).map(ProjectStage::getId).toList();
            });
            when(stageMapper.selectAllActionIdsForBootstrap(anyLong())).thenAnswer(inv -> {
                Long projectId = inv.getArgument(0);
                return actions.stream().filter(a -> projectId.equals(a.getProjectId())).map(StageAction::getId).toList();
            });
            // R8X-CONT-1 P0-3 后契约：insertBatch(List, batchSize) 单次批量写入 + ASSIGN_ID 主键回填。
            // 行级 throwAt 模拟批量写中途失败（前 N-1 行已落内存，真实回滚由真库验收证明）。
            when(stageMapper.insertBatch(anyList(), anyInt())).thenAnswer(inv -> {
                List<ProjectStage> batch = inv.getArgument(0);
                for (ProjectStage stage : batch) {
                    if (++stageAttempts == stageThrowAt) throw failure;
                    stages.add(stage);
                }
                if ("ZERO_ROWS".equals(stageFault) || "MULTIPLE_ROWS".equals(stageFault)) return false;
                Long firstId = nextStageId + 1;
                for (ProjectStage stage : batch) {
                    stage.setId(generatedId(stageFault, ++nextStageId, firstId));
                }
                return true;
            });
            when(actionMapper.insertBatch(anyList(), anyInt())).thenAnswer(inv -> {
                List<StageAction> batch = inv.getArgument(0);
                for (StageAction action : batch) {
                    if (++actionAttempts == actionThrowAt) throw failure;
                    actions.add(action);
                }
                if ("ZERO_ROWS".equals(actionFault) || "MULTIPLE_ROWS".equals(actionFault)) return false;
                Long firstId = nextActionId + 1;
                for (StageAction action : batch) {
                    action.setId(generatedId(actionFault, ++nextActionId, firstId));
                }
                return true;
            });
            clearInvocations(stageMapper, actionMapper);
        }

        private static Long generatedId(String fault, long ordinaryId, Long firstId) {
            return switch (fault) {
                case "MISSING_ID" -> null;
                case "ZERO_ID" -> 0L;
                case "NEGATIVE_ID" -> -1L;
                case "DUPLICATE_ID" -> firstId;
                default -> ordinaryId;
            };
        }

        private static Project project(long id) {
            return Project.builder().id(id).templateType("HARDWARE").tenantId("000000").delFlag("0").build();
        }

        void seedComplete(long projectId) {
            String[] codes = {"CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE"};
            Map<String, Long> stageIds = new LinkedHashMap<>();
            for (int i = 0; i < codes.length; i++) {
                ProjectStage stage = ProjectStage.builder().id(++nextStageId).projectId(projectId)
                    .stageCode(codes[i]).stageName(ProjectBootstrapService.STAGES[i][1]).sortOrder((i + 1) * 10)
                    .status("NOT_STARTED").tenantId("000000").delFlag("0").build();
                stage.setCreateBy(7L);
                stage.setUpdateBy(7L);
                stages.add(stage);
                stageIds.put(codes[i], stage.getId());
            }
            for (ActionDef def : ActionCatalog.ALL) {
                Project project = projects.get(projectId);
                String depth = ActionCatalog.expectedDepth(def, project.getTemplateType());
                boolean applicable = ActionCatalog.applicableTo(
                    def, project.getTemplateType(), project.getTargetMarkets());
                StageAction action = StageAction.builder().id(++nextActionId).projectId(projectId)
                    .stageId(stageIds.get(def.stage())).actionCode(def.code()).actionName(def.name())
                    .ownerRole(def.ownerRole()).depth(depth).status(applicable ? "NOT_STARTED" : "NA")
                    .isBlocking(def.blocking() ? "1" : "0").isBioFeature(def.bioFeature() ? "1" : "0").build();
                action.setCreateBy(7L);
                action.setUpdateBy(7L);
                actions.add(action);
            }
        }

        void assertNoWrites() {
            for (Object mapper : List.of(stageMapper, actionMapper)) {
                assertThat(mockingDetails(mapper).getInvocations()).noneMatch(inv ->
                    inv.getMethod().getName().startsWith("insert") || inv.getMethod().getName().startsWith("update")
                        || inv.getMethod().getName().startsWith("delete"));
            }
        }
    }
}
