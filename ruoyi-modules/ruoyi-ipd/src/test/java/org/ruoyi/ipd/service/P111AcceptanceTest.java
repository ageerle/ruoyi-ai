package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-1.1 / AC-PROD-01：产品↔项目 1:1 双向绑定验收（Mock 层）。
 * <p>覆盖：创建入口拒绝、绑定入口拒绝、双向回填、幂等、软删拒绝、GUEST_OTHER。
 * <p>真库/HTTP 回归仍属卡面完整验收；本类满足看板「新增 P111AcceptanceTest」交付门禁。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P111AcceptanceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private GateEngine gateEngine;
    @Mock
    private ProjectBootstrapService projectBootstrapService;

    private ProductService productService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, projectMapper, auditLogService);
        projectService = new ProjectService(
            projectMapper, productMapper, auditLogService, gateEngine, projectBootstrapService);
    }

    private Product aliveProduct(Long id, String source, Long projectId) {
        Product p = new Product();
        p.setId(id);
        p.setProductName("人脸门禁");
        p.setSource(source);
        p.setProjectId(projectId);
        p.setDelFlag("0");
        return p;
    }

    private Project aliveProject(Long id, Long productId) {
        Project p = new Project();
        p.setId(id);
        p.setCode("PRJ-2026-001");
        p.setName("S");
        p.setProductId(productId);
        p.setDelFlag("0");
        return p;
    }

    private Project newProjectDraft() {
        Project p = new Project();
        p.setName("人脸门禁 S 级");
        p.setProductId(50L);
        p.setTemplateType("HARDWARE");
        p.setLevel("S");
        p.setLevelCoefficient(new BigDecimal("1.8"));
        p.setLevelCoefficientReason("旗舰");
        return p;
    }

    @Test
    @DisplayName("AC-PROD-01 已挂项目的产品再关联第二个项目 → 拒绝「一个产品仅对应一个项目」")
    void productAlreadyBoundRejectsSecondProject() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, 9L);
        when(productMapper.selectById(3L)).thenReturn(product);

        assertThatThrownBy(() -> productService.bindProject(3L, 99L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("一个产品仅对应一个项目");
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("创建项目：产品.projectId 已占 → 拒绝「一个产品仅对应一个项目」")
    void createProjectWhenProductAlreadyHasProjectId() {
        Product product = aliveProduct(50L, Product.SRC_PM_NEW, 88L);
        when(productMapper.selectById(50L)).thenReturn(product);

        assertThatThrownBy(() -> projectService.create(newProjectDraft(), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("一个产品仅对应一个项目");
    }

    @Test
    @DisplayName("创建项目：同 productId 已有存活项目 → 拒绝 1:1")
    void createProjectWhenProductTakenByOtherProject() {
        when(productMapper.selectById(50L)).thenReturn(aliveProduct(50L, Product.SRC_PM_NEW, null));
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> projectService.create(newProjectDraft(), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("1:1");
    }

    @Test
    @DisplayName("bindProject 成功：两端同事务回填 product.projectId 与 project.productId")
    void bindProjectBidirectional() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, null);
        Project project = aliveProject(9L, null);
        when(productMapper.selectById(3L)).thenReturn(product);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectById(9L)).thenReturn(project);

        productService.bindProject(3L, 9L, 1L);

        assertThat(product.getProjectId()).isEqualTo(9L);
        assertThat(project.getProductId()).isEqualTo(3L);
        verify(productMapper).updateById(product);
        verify(projectMapper).updateById(project);
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("bindProject 幂等：已绑定同一项目 → 不写库不写审计")
    void bindProjectIdempotent() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, 9L);
        when(productMapper.selectById(3L)).thenReturn(product);

        productService.bindProject(3L, 9L, 1L);

        verify(productMapper, never()).updateById(any(Product.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("GUEST_OTHER 不可 bindProject")
    void guestOtherCannotBind() {
        Product product = aliveProduct(3L, Product.SRC_GUEST_OTHER, null);
        when(productMapper.selectById(3L)).thenReturn(product);

        assertThatThrownBy(() -> productService.bindProject(3L, 9L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("其他");
    }

    @Test
    @DisplayName("软删项目不可绑定；软删产品不可绑定")
    void softDeletedRejected() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, null);
        when(productMapper.selectById(3L)).thenReturn(product);
        Project deleted = aliveProject(9L, null);
        deleted.setDelFlag("1");
        when(projectMapper.selectById(9L)).thenReturn(deleted);
        assertThatThrownBy(() -> productService.bindProject(3L, 9L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("项目不存在");

        Product softProduct = aliveProduct(4L, Product.SRC_PM_NEW, null);
        softProduct.setDelFlag("1");
        when(productMapper.selectById(4L)).thenReturn(softProduct);
        assertThatThrownBy(() -> productService.bindProject(4L, 10L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("产品不存在");
    }

    @Test
    @DisplayName("项目已被其他产品占用 → 拒绝 1:1")
    void projectTakenByOtherProduct() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, null);
        Project project = aliveProject(9L, 7L);
        when(productMapper.selectById(3L)).thenReturn(product);
        when(projectMapper.selectById(9L)).thenReturn(project);

        assertThatThrownBy(() -> productService.bindProject(3L, 9L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("1:1");
    }

    @Test
    @DisplayName("AC-INC-36：代码路径无「一产品多项目池分摊」——冲突一律拒绝而非分摊")
    void noMultiProjectPoolAllocation() {
        Product product = aliveProduct(3L, Product.SRC_PM_NEW, 9L);
        when(productMapper.selectById(3L)).thenReturn(product);
        assertThatThrownBy(() -> productService.bindProject(3L, 99L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("一个产品仅对应一个项目")
            .satisfies(t -> assertThat(t.getMessage()).doesNotContain("分摊").doesNotContain("池"));
    }

    @Test
    @DisplayName("创建项目成功后产品侧回填 projectId（创建入口 1:1）")
    void createProjectBackfillsProduct() {
        Product product = aliveProduct(50L, Product.SRC_PM_NEW, null);
        when(productMapper.selectById(50L)).thenReturn(product);
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any())).thenReturn(java.util.List.of());
        when(projectMapper.insert(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(501L);
            return 1;
        });

        Project created = projectService.create(newProjectDraft(), 1L);

        assertThat(created.getId()).isEqualTo(501L);
        assertThat(product.getProjectId()).isEqualTo(501L);
        ArgumentCaptor<Product> cap = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(cap.capture());
        assertThat(cap.getValue().getProjectId()).isEqualTo(501L);
    }
}
