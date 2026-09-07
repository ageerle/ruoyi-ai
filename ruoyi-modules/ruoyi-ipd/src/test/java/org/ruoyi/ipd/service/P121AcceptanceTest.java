package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P1-2.1 验收：四基准必填、系数默认/区间、奖金池算例、服务端 DRAFT。
 * <p>形态为 Mockito 单元验收；真库/HTTP 另见 QA 卡，不得据此标 done。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P121AcceptanceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProductMapper productMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private GateEngine gateEngine;
    @Mock private ProjectBootstrapService projectBootstrapService;
    @Mock private ProjectCertService projectCertService;
    @Mock private org.ruoyi.ipd.mapper.StageActionMapper stageActionMapper;
    @Mock private org.ruoyi.ipd.mapper.KpiRecordMapper kpiRecordMapper;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectMapper, productMapper, stageActionMapper, kpiRecordMapper, auditLogService, gateEngine, projectBootstrapService, projectCertService, NoopTransactionManager.INSTANCE);
    }

    private Product productOk() {
        Product p = new Product();
        p.setId(50L);
        p.setSource(Product.SRC_PM_NEW);
        p.setDelFlag("0");
        return p;
    }

    /**
     * 构造带齐四基准与模板/市场/主组的立项草稿。
     *
     * @param level 级别
     * @param coef  系数，null 表示走默认
     * @param sales 目标销售额
     * @return Project
     */
    private Project draft(String level, String coef, String sales) {
        Project p = new Project();
        p.setName("立项验收项目");
        p.setProductId(50L);
        p.setTemplateType("HARDWARE");
        p.setTargetMarkets("[\"SA\"]");
        p.setMainGroupId(7L);
        p.setLevel(level);
        p.setLevelCoefficient(coef == null ? null : new BigDecimal(coef));
        p.setLevelCoefficientReason(coef == null || "1.5".equals(coef) || "0.8".equals(coef) ? null : "差异化定值");
        p.setTargetSalesAmount(new BigDecimal(sales));
        p.setTargetChannelCount(10);
        p.setTargetNps(70);
        p.setTargetSceneCount(5);
        p.setStatus("ACTIVE"); // 客户端注入应被服务端覆盖为 DRAFT
        return p;
    }

    private void stubCreateHappyPath() {
        when(productMapper.selectById(50L)).thenReturn(productOk());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("AC-INC-12 S 级默认 1.5；奖金池 = 实际回款 500万×5%×1.5 = 37.5万；状态强制 DRAFT（ZK 实际回款口径）")
    void acInc12SDefaultAndBonusPool() {
        stubCreateHappyPath();
        Project created = service.create(draft("S", null, "5000000"), 1L);
        assertThat(created.getLevelCoefficient()).isEqualByComparingTo("1.5");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(ProjectService.computeBonusPool(created.getTargetSalesAmount(), created.getLevelCoefficient()))
            .isEqualByComparingTo("375000");
    }

    @Test
    @DisplayName("AC-INC-13 A 级默认 1.0；奖金池 = 实际回款 500万×5%×1.0 = 25万（ZK 实际回款口径）")
    void acInc13ADefault() {
        stubCreateHappyPath();
        Project created = service.create(draft("A", null, "5000000"), 1L);
        assertThat(created.getLevelCoefficient()).isEqualByComparingTo("1.0");
        assertThat(ProjectService.computeBonusPool(created.getTargetSalesAmount(), created.getLevelCoefficient()))
            .isEqualByComparingTo("250000");
    }

    @Test
    @DisplayName("AC-INC-14 B 级默认 0.8；奖金池 = 实际回款 200万×5%×0.8 = 8万（ZK 实际回款口径）")
    void acInc14BDefault() {
        stubCreateHappyPath();
        Project created = service.create(draft("B", null, "2000000"), 1L);
        assertThat(created.getLevelCoefficient()).isEqualByComparingTo("0.8");
        assertThat(ProjectService.computeBonusPool(created.getTargetSalesAmount(), created.getLevelCoefficient()))
            .isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("AC-INC-15 S 级系数 2.5 拒绝，提示区间 1.5–2.0")
    void acInc15SOutOfRange() {
        assertThatThrownBy(() -> service.create(draft("S", "2.5", "5000000"), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("S 级系数区间为 1.5–2.0");
    }

    @Test
    @DisplayName("AC-INC-15b A 级录入 1.2 拒绝")
    void acInc15bARejectCustom() {
        assertThatThrownBy(() -> service.create(draft("A", "1.2", "5000000"), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("A 级为固定 1.0 不可改");
    }

    @Test
    @DisplayName("P1-2.1 缺模板/市场/主组/四基准任一 → 拒绝")
    void baselinesRequired() {
        Project missingMarket = draft("S", null, "5000000");
        missingMarket.setTargetMarkets(" ");
        assertThatThrownBy(() -> service.create(missingMarket, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("目标市场");

        Project missingGroup = draft("S", null, "5000000");
        missingGroup.setMainGroupId(null);
        assertThatThrownBy(() -> service.create(missingGroup, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("主组");

        Project badTpl = draft("S", null, "5000000");
        badTpl.setTemplateType("UNKNOWN");
        assertThatThrownBy(() -> service.create(badTpl, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("模板类型");

        Project noSales = draft("S", null, "5000000");
        noSales.setTargetSalesAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.create(noSales, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("目标销售额");
    }

    @Test
    @DisplayName("AC-INC-15c：立项非默认系数拒绝，须走双PM+组长流程")
    void sbNonDefaultMustUseWorkflow() {
        Project custom = draft("S", "1.8", "5000000");
        assertThatThrownBy(() -> service.create(custom, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("AC-INC-15c");
    }
}
