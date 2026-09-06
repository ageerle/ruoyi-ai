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
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P1-1.2：产品编辑 + 超管批量导入（AC-PROD-06/07）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P112AcceptanceTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productMapper, projectMapper, auditLogService);
    }

    @Test
    @DisplayName("AC-PROD-07：PM 新增在研产品状态=IN_RD")
    void pmNewCreatesInRd() {
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(10L);
            return 1;
        });
        Product created = service.create(Product.builder()
            .productName("在研探针").source(Product.SRC_PM_NEW).build(), 1L);
        assertThat(created.getStatus()).isEqualTo(Product.ST_IN_RD);
    }

    @Test
    @DisplayName("AC-PROD-06：超管导入在售型号状态=ON_SALE且须 modelCode")
    void adminImportOnSale() {
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(11L);
            return 1;
        });
        assertThatThrownBy(() -> service.create(Product.builder()
            .productName("缺型号").source(Product.SRC_ADMIN_IMPORT).build(), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("modelCode");
        Product created = service.create(Product.builder()
            .productName("在售X").modelCode("ZK-X100").source(Product.SRC_ADMIN_IMPORT).build(), 1L);
        assertThat(created.getStatus()).isEqualTo(Product.ST_ON_SALE);
    }

    @Test
    @DisplayName("批量导入：按 modelCode 幂等 UPSERT；坏行进报告不中断")
    void batchImportReport() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L + (p.getModelCode() == null ? 0 : p.getModelCode().hashCode() % 50));
            return 1;
        });
        List<Map<String, Object>> report = service.batchImportOnSale(List.of(
            Product.builder().productName("A").modelCode("M-A").build(),
            Product.builder().productName("缺码").build(),
            Product.builder().productName("B").modelCode("M-B").build()
        ), 9L);
        assertThat(report).hasSize(3);
        assertThat(report.get(0).get("ok")).isEqualTo(true);
        assertThat(report.get(1).get("ok")).isEqualTo(false);
        assertThat(report.get(2).get("ok")).isEqualTo(true);
    }

    @Test
    @DisplayName("编辑产品名称成功；禁止改来源")
    void updateAllowsNameRejectsSourceChange() {
        Product existing = Product.builder().id(5L).productName("旧").source(Product.SRC_PM_NEW)
            .status(Product.ST_IN_RD).delFlag("0").groupId(1L).build();
        when(productMapper.selectById(5L)).thenReturn(existing);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        Product updated = service.update(5L, Product.builder().productName("新名").build(), 1L, 1L, "MARKET_PM");
        assertThat(updated.getProductName()).isEqualTo("新名");
        assertThatThrownBy(() -> service.update(5L,
            Product.builder().source(Product.SRC_ADMIN_IMPORT).build(), 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("来源");
    }
}
