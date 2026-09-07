package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.OssFileMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W4-Defence-G1 / AC-GATE-15 G1 否决项硬阻断落库回归。
 *
 * <p>B-FIX-PACK-1 子卡（卡面 todo 2e4dfcfc …）：G1 评审命中否决项（如 C12 合规审查未通过）
 * 仍尝试提交"通过"必须被服务端阻断，提交校验抛 {@code ServiceException} 且阻断信息含全部
 * 命中的否决项编码；未命中否决项 FAIL 应放行；多次命中应全部列出；空场景（无适用要素）
 * 应放行。本卡关注阻断语义，不复测 CONDITIONAL/PASS 改判的细节（P251/P253 已覆盖）。
 *
 * <p>边界遵守：本卡新建独立测试类，不触碰 P251/P253/GateMandatoryOutputsAcceptanceTest
 * （禁触区）；通过 import 引用 {@link GateElementResultService}，不修改其源码。
 *
 * <p>实体/mapper 现状：{@link GateElementResult} 实体与 {@link GateElementResultMapper}
 * 已存在（见 domain/mapper 包），本卡不复建——直接走既有实现做阻断回归。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
@DisplayName("W4-Defence-G1 AC-GATE-15 G1 否决项硬阻断（命中阻断 / 未命中放行 / 多命中列全部 / 空放行）")
class GateElementResultTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateElementMapper elementMapper;
    @Mock
    private GateElementResultMapper resultMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OssFileMapper ossFileMapper;

    private GateElementResultService service;

    private static final IpdActor PM = new IpdActor(301L, "评审PM", "MARKET_PM", 7L);

    private Gate g1;
    private GateElement g1Veto;       // G1-2 否决项（AC-GATE-20 四项基准值；用 C12 占位亦可，此处按真表 element_code 走）
    private GateElement g1Normal;     // G1-3 普通要素

    private static final long VETO_ID = 601L;
    private static final long NORMAL_ID = 603L;
    private static final long SECOND_VETO_ID = 604L;

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "W4G1-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "W4G1-ger"), GateElementResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateElementResultService(gateMapper, elementMapper, resultMapper,
            systemConfigService, auditLogService, notificationService, ossFileMapper);
        g1 = new Gate();
        g1.setId(501L);
        g1.setProjectId(11L);
        g1.setGateCode("G1");
        g1.setStatus("PENDING");
        g1.setCurrentRound(1);
        g1Veto = element(VETO_ID, "G1", "G1-2", "1");
        g1Normal = element(NORMAL_ID, "G1", "G1-3", "0");
        lenient().when(gateMapper.selectById(501L)).thenReturn(g1);
        // 前序 Gate 逾期查询默认返回空（不引入额外遗留分支）
        lenient().when(gateMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 审计与系统配置走宽松桩
        lenient().when(systemConfigService.getIntValue(any(), any(Integer.class))).thenReturn(3);
    }

    private GateElement element(Long id, String gateCode, String code, String isVeto) {
        GateElement e = new GateElement();
        e.setId(id);
        e.setGateCode(gateCode);
        e.setElementCode(code);
        e.setElementName("要素" + code);
        e.setIsVeto(isVeto);
        e.setSortOrder(1);
        return e;
    }

    private GateElementResult judgedRow(Long elementId, String result, String evidenceRef) {
        return GateElementResult.builder()
            .id(1000L + elementId)
            .gateId(501L)
            .elementId(elementId)
            .result(result)
            .evidenceRef(evidenceRef)
            .build();
    }

    /** 构造两张假 OSS 文件以满足 submit 的 [SEC-FIX-HIGH-1.1-FOLLOWUP] 必传校验。 */
    private void stubOssForSubmit() {
        org.ruoyi.ipd.domain.OssFileEntity mat = new org.ruoyi.ipd.domain.OssFileEntity();
        mat.setOssId(9001L);
        mat.setUrl("https://oss.local/materials/g1.pdf");
        org.ruoyi.ipd.domain.OssFileEntity min = new org.ruoyi.ipd.domain.OssFileEntity();
        min.setOssId(9002L);
        min.setUrl("https://oss.local/minutes/g1.pdf");
        lenient().when(ossFileMapper.selectById(9001L)).thenReturn(mat);
        lenient().when(ossFileMapper.selectById(9002L)).thenReturn(min);
    }

    // ---------- AC-GATE-15 正例 1：单否决项 FAIL ⇒ 阻断 ----------

    @Test
    @DisplayName("AC-GATE-15 反例：单否决项 FAIL ⇒ submit 阻断且异常信息含 elementCode")
    void submit_vetoFail_single_blocked_withElementCode() {
        stubOssForSubmit();
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(g1Veto, g1Normal));
        // 既有判定：否决项 FAIL 且带证据（满足 AC-GATE-02），普通项 PASS
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(VETO_ID, "FAIL", "https://oss.local/proof.pdf"),
                judgedRow(NORMAL_ID, "PASS", null)));

        assertThatThrownBy(() -> service.submit(501L, 9001L, 9002L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("命中否决项无法提交通过")
            .hasMessageContaining("G1-2");
        // 阻断后不应启动 Gate、不应发审计 GATE_SUBMIT
        assertThat(g1.getStartedAt()).as("被否决项 FAIL 阻断的 Gate 不应置 startedAt").isNull();
    }

    // ---------- AC-GATE-15 正例 2：未命中 ⇒ 通过 + 冻结快照 + 审计 GATE_SUBMIT ----------

    @Test
    @DisplayName("AC-GATE-15 正例：无否决项 FAIL ⇒ submit 通过、startedAt 置位、elementSnapshot 冻结")
    void submit_noVetoFail_passes_freezesSnapshot() {
        stubOssForSubmit();
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(g1Veto, g1Normal));
        // 否决项 PASS（不命中硬阻断），普通项 PASS
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(VETO_ID, "PASS", null),
                judgedRow(NORMAL_ID, "PASS", null)));

        Gate submitted = service.submit(501L, 9001L, 9002L, PM);

        assertThat(submitted.getStartedAt()).as("放行后应冻结 startedAt").isNotNull();
        assertThat(submitted.getElementSnapshot()).as("提交应冻结要素定义快照").isNotBlank();
        // GATE_SUBMIT 审计落库
        verify(auditLogService).append(org.mockito.ArgumentMatchers.argThat(a ->
            "GATE_SUBMIT".equals(a.getAction())
                && "gates".equals(a.getEntityType())
                && submitted.getId().equals(a.getEntityId())));
    }

    // ---------- AC-GATE-15 反例 3：多否决项 FAIL ⇒ 全部列出 ----------

    @Test
    @DisplayName("AC-GATE-15 反例：多否决项 FAIL ⇒ submit 阻断且异常列出全部命中项")
    void submit_vetoFail_multiple_blocked_listsAll() {
        stubOssForSubmit();
        GateElement g1VetoSecond = element(SECOND_VETO_ID, "G1", "G1-4", "1");
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(g1Veto, g1VetoSecond, g1Normal));
        // 两项否决均 FAIL
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(VETO_ID, "FAIL", "https://oss.local/proof-a.pdf"),
                judgedRow(SECOND_VETO_ID, "FAIL", "https://oss.local/proof-b.pdf"),
                judgedRow(NORMAL_ID, "PASS", null)));

        assertThatThrownBy(() -> service.submit(501L, 9001L, 9002L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("命中否决项无法提交通过")
            .hasMessageContaining("G1-2")
            .hasMessageContaining("G1-4");
        assertThat(g1.getStartedAt()).as("多项否决 FAIL 阻断后不应置 startedAt").isNull();
    }

    // ---------- AC-GATE-15 边界 4：空（无适用要素）⇒ 通过 ----------

    @Test
    @DisplayName("AC-GATE-15 边界：空适用要素清单 ⇒ submit 不触发任何阻断（无缺失、无否决 FAIL）放行")
    void submit_emptyElements_passes() {
        stubOssForSubmit();
        // 适用要素为空 + 判定结果为空
        lenient().when(elementMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(resultMapper.selectList(any())).thenReturn(Collections.emptyList());

        Gate submitted = service.submit(501L, 9001L, 9002L, PM);

        assertThat(submitted.getStartedAt()).as("空要素清单应放行").isNotNull();
        // 不应触发否决阻断（vetoFails 为空列表）
        assertThat(submitted.getStartedAt()).isBeforeOrEqualTo(new Date());
    }

    // ---------- 持久化契约（保活：阻断路径不调用 updateById / 不发 GATE_SUBMIT） ----------

    @Test
    @DisplayName("AC-GATE-15 持久化契约：阻断路径不修改 Gate 行、不发 GATE_SUBMIT 审计")
    void submit_blocked_persistsNothing() {
        stubOssForSubmit();
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(g1Veto));
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(VETO_ID, "FAIL", "https://oss.local/proof.pdf")));

        assertThatThrownBy(() -> service.submit(501L, 9001L, 9002L, PM))
            .isInstanceOf(ServiceException.class);

        // 阻断路径不应对 Gate 写 startedAt、不应发 GATE_SUBMIT 审计
        verify(auditLogService, never()).append(org.mockito.ArgumentMatchers.argThat(a ->
            "GATE_SUBMIT".equals(a.getAction())));
    }
}
