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

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectMapper, productMapper, auditLogService);
    }

    private Project base(String level, String coefficient, String reason) {
        Project p = new Project();
        p.setName("人脸门禁 S 级");
        p.setProductId(50L);
        p.setTemplateType("HARDWARE");
        p.setLevel(level);
        p.setLevelCoefficient(coefficient == null ? null : new BigDecimal(coefficient));
        p.setLevelCoefficientReason(reason);
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
    @DisplayName("创建成功：S 1.8 + 理由 → 编码/CONCEPT/DRAFT 回填 + 产品 1:1 回填")
    void createOk() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any())).thenReturn(List.of());

        Project created = service.create(base("S", "1.8", "旗舰差异化"), 1L);

        assertThat(created.getCode()).matches("PRJ-\\d{4}-\\d{3}");
        assertThat(created.getCurrentStage()).isEqualTo("CONCEPT");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(product50().getProjectId()).isEqualTo(created.getId());
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("系数校验：S 越下界 1.4 / B 越上界 0.9 / A 带系数 → 全拒")
    void coefficientRange() {
        assertThatThrownBy(() -> service.create(base("S", "1.4", "x"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("1.5");
        assertThatThrownBy(() -> service.create(base("B", "0.9", "x"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("0.8");
        assertThatThrownBy(() -> service.create(base("A", "1.0", null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("A 级");
    }

    @Test
    @DisplayName("S/B 理由必填；S 无系数必填")
    void reasonAndCoefficientRequired() {
        assertThatThrownBy(() -> service.create(base("S", "1.8", null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("理由必填");
        assertThatThrownBy(() -> service.create(base("S", null, "x"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("必填");
    }

    @Test
    @DisplayName("产品:项目 = 1:1：产品已被项目占用 → 拒绝")
    void oneToOneGuard() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(base("S", "1.8", "x"), 1L))
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
        when(projectMapper.selectById(9L)).thenReturn(draft);

        assertThatThrownBy(() -> service.changeStatus(9L, "ACTIVE", 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("非法迁移");
        assertThat(service.changeStatus(9L, "TEAMING", 1L).getStatus()).isEqualTo("TEAMING");

        Project archived = new Project();
        archived.setId(10L);
        archived.setName("y");
        archived.setStatus("ARCHIVED");
        archived.setDelFlag("0");
        when(projectMapper.selectById(10L)).thenReturn(archived);
        assertThatThrownBy(() -> service.changeStatus(10L, "ACTIVE", 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("非法迁移");
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
        when(projectMapper.selectById(9L)).thenReturn(concept);
        assertThat(service.advanceStage(9L, 1L).getCurrentStage()).isEqualTo("PLAN");

        Project valid = new Project();
        valid.setId(11L);
        valid.setName("z");
        valid.setStatus("ACTIVE");
        valid.setCurrentStage("VALID");
        valid.setDelFlag("0");
        when(projectMapper.selectById(11L)).thenReturn(valid);
        assertThatThrownBy(() -> service.advanceStage(11L, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("上市日期");
    }
}