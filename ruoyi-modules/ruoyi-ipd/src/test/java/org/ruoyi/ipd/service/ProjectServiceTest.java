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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目服务单测：编码生成/系数区间/1:1/状态机/阶段推进
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private GateEngine gateEngine;
    @Mock
    private ProjectBootstrapService projectBootstrapService;
    @Mock
    private ProjectCertService projectCertService;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectMapper, productMapper, auditLogService, gateEngine,
            projectBootstrapService, projectCertService, NoopTransactionManager.INSTANCE);
    }

    private Project base(String level, String coefficient, String reason) {
        Project p = new Project();
        p.setName("人脸门禁 S 级");
        p.setProductId(50L);
        p.setTemplateType("HARDWARE");
        p.setTargetMarkets("[\"SA\"]");
        p.setMainGroupId(7L);
        p.setLevel(level);
        p.setLevelCoefficient(coefficient == null ? null : new BigDecimal(coefficient));
        p.setLevelCoefficientReason(reason);
        p.setTargetSalesAmount(new BigDecimal("5000000"));
        p.setTargetChannelCount(10);
        p.setTargetNps(70);
        p.setTargetSceneCount(5);
        return p;
    }

    private Product product50() {
        Product product = new Product();
        product.setId(50L);
        product.setProductName("人脸门禁");
        product.setDelFlag("0");
        return product;
    }

    @Test
    @DisplayName("编码生成：年内最大序号 +1，空年为 001")
    void nextCode() {
        Project p1 = new Project();
        p1.setCode("PRJ-2026-001");
        Project p3 = new Project();
        p3.setCode("PRJ-2026-003");
        when(projectMapper.selectList(any())).thenReturn(List.of(p1, p3));
        assertThat(service.nextCode()).isEqualTo("PRJ-2026-004");

        when(projectMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.nextCode()).endsWith("-001");
    }

    @Test
    @DisplayName("创建成功：S 默认 1.5 → 编码/CONCEPT/DRAFT 回填 + 产品 1:1 回填")
    void createOk() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any())).thenReturn(List.of());

        Project created = service.create(base("S", null, null), 1L);

        assertThat(created.getCode()).matches("PRJ-\\d{4}-\\d{3}");
        assertThat(created.getCurrentStage()).isEqualTo("CONCEPT");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(created.getLevelCoefficient()).isEqualByComparingTo("1.5");
        assertThat(product50().getProjectId()).isEqualTo(created.getId());
        verify(projectBootstrapService).bootstrap(created.getId(), 1L);
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("系数校验：S 越下界 1.4 / B 越上界 0.9 / A 带非 1.0 系数 → 全拒")
    void coefficientRange() {
        assertThatThrownBy(() -> service.create(base("S", "1.4", "x"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("1.5");
        assertThatThrownBy(() -> service.create(base("B", "0.9", "x"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("0.8");
        assertThatThrownBy(() -> service.create(base("A", "1.2", null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("A 级");
    }

    @Test
    @DisplayName("AC-INC-15c：S 非默认须走流程；默认 1.5 可无理由")
    void reasonAndCoefficientRequired() {
        assertThatThrownBy(() -> service.create(base("S", "1.8", "旗舰"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("AC-INC-15c");
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.create(base("S", null, null), 1L).getLevelCoefficient())
            .isEqualByComparingTo("1.5");
    }

    @Test
    @DisplayName("产品:项目 = 1:1：产品已被项目占用 → 拒绝")
    void oneToOneGuard() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(base("S", null, null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("1:1");
    }

    @Test
    @DisplayName("状态机：DRAFT→ACTIVE 拒绝；DRAFT→TEAMING 通过；ARCHIVED 无出边")
    void statusMachine() {
        Project draft = new Project();
        draft.setId(9L);
        draft.setName("x");
        draft.setStatus("DRAFT");
        draft.setDelFlag("0");
        draft.setMainGroupId(1L);
        when(projectMapper.selectById(9L)).thenReturn(draft);

        assertThatThrownBy(() -> service.changeStatus(9L, "ACTIVE", 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("非法迁移");
        assertThat(service.changeStatus(9L, "TEAMING", 1L, 1L, "MARKET_PM").getStatus()).isEqualTo("TEAMING");

        Project archived = new Project();
        archived.setId(10L);
        archived.setName("y");
        archived.setStatus("ARCHIVED");
        archived.setDelFlag("0");
        archived.setMainGroupId(1L);
        when(projectMapper.selectById(10L)).thenReturn(archived);
        assertThatThrownBy(() -> service.changeStatus(10L, "ACTIVE", 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已归档");  // ZK-IPD §二.10：归档只读 guard 优先于状态机迁移
    }

    @Test
    @DisplayName("阶段推进：CONCEPT→PLAN 线性；VALID 无上市日期进 LAUNCH 拒绝")
    void stageAdvance() {
        Project concept = new Project();
        concept.setId(9L);
        concept.setName("x");
        concept.setStatus("ACTIVE");
        concept.setCurrentStage("CONCEPT");
        concept.setDelFlag("0");
        concept.setMainGroupId(1L);
        when(projectMapper.selectById(9L)).thenReturn(concept);
        assertThat(service.advanceStage(9L, 1L, 1L, "MARKET_PM").getCurrentStage()).isEqualTo("PLAN");

        Project valid = new Project();
        valid.setId(11L);
        valid.setName("z");
        valid.setStatus("ACTIVE");
        valid.setCurrentStage("VALID");
        valid.setDelFlag("0");
        valid.setMainGroupId(1L);
        when(projectMapper.selectById(11L)).thenReturn(valid);
        assertThatThrownBy(() -> service.advanceStage(11L, 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("上市日期");
    }

    /* ----------------- ZK-IPD §二.10 归档后只读下沉到 service 层 ----------------- */

    @Test
    @DisplayName("ZK-IPD §二.10：归档项目 updateBaselines 拒绝（service 层下沉）")
    void archivedProjectUpdateBaselinesRejected() {
        Project archived = new Project();
        archived.setId(20L);
        archived.setName("archived-proj");
        archived.setStatus("ARCHIVED");
        archived.setDelFlag("0");
        archived.setMainGroupId(1L);
        archived.setTargetSalesAmount(new BigDecimal("100"));
        archived.setTargetChannelCount(1);
        archived.setTargetNps(50);
        archived.setTargetSceneCount(1);
        when(projectMapper.selectById(20L)).thenReturn(archived);
        Project patch = new Project();
        patch.setTargetSalesAmount(new BigDecimal("999999"));
        assertThatThrownBy(() -> service.updateBaselines(20L, patch, 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已归档");
    }

    @Test
    @DisplayName("ZK-IPD §二.10：归档项目 advanceStage 拒绝（service 层下沉）")
    void archivedProjectAdvanceStageRejected() {
        Project archived = new Project();
        archived.setId(21L);
        archived.setName("archived-proj");
        archived.setStatus("ARCHIVED");
        archived.setDelFlag("0");
        archived.setMainGroupId(1L);
        archived.setCurrentStage("LIFECYCLE");
        when(projectMapper.selectById(21L)).thenReturn(archived);
        assertThatThrownBy(() -> service.advanceStage(21L, 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已归档");
    }

    @Test
    @DisplayName("ZK-IPD §二.10：归档项目可读 getById 不抛异常")
    void archivedProjectReadable() {
        Project archived = new Project();
        archived.setId(22L);
        archived.setName("archived");
        archived.setStatus("ARCHIVED");
        archived.setDelFlag("0");
        archived.setMainGroupId(1L);
        when(projectMapper.selectById(22L)).thenReturn(archived);
        // getById 是只读，不应抛异常
        assertThat(service.getById(22L).getStatus()).isEqualTo("ARCHIVED");
    }
}