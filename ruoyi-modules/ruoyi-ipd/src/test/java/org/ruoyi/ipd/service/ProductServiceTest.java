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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品服务单测：三路来源/1:1/启停
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private AuditLogService auditLogService;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productMapper, projectMapper, auditLogService);
    }

    private Product product(String source, String modelCode, Long projectId) {
        Product p = new Product();
        p.setProductName("人脸门禁 Pro");
        p.setSource(source);
        p.setModelCode(modelCode);
        p.setProjectId(projectId);
        p.setGroupId(1L);
        return p;
    }

    @Test
    @DisplayName("来源非法拒绝（白名单外）")
    void invalidSource() {
        assertThatThrownBy(() -> service.create(product("HACK", null, null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("来源非法");
    }

    @Test
    @DisplayName("ADMIN_IMPORT 必须 modelCode")
    void adminImportNeedsModel() {
        assertThatThrownBy(() -> service.create(product("ADMIN_IMPORT", null, null), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("modelCode");
    }

    @Test
    @DisplayName("GUEST_OTHER 占位不可关联项目")
    void guestOtherCannotBindProject() {
        assertThatThrownBy(() -> service.create(product("GUEST_OTHER", null, 9L), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("其他");
    }

    @Test
    @DisplayName("1:1：项目已被其他产品占用 → 拒绝")
    void oneToOneTaken() {
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(product("PM_NEW", null, 9L), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("1:1");
    }

    @Test
    @DisplayName("PM_NEW 正常创建 + 默认 ACTIVE + 审计")
    void createOk() {
        // projectId=null 不触发占用检查，无需 stub selectCount（Mockito strict）
        Product created = service.create(product("PM_NEW", null, null), 1L);
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("bindProject：项目存在且未占用 → 两端回填关联")
    void bindProjectOk() {
        Product p = product("PM_NEW", null, null);
        p.setId(3L);
        p.setDelFlag("0");
        when(productMapper.selectById(3L)).thenReturn(p);
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        Project project = new Project();
        project.setId(9L);
        project.setDelFlag("0");
        when(projectMapper.selectById(9L)).thenReturn(project);

        service.bindProject(3L, 9L, 1L, 1L, "MARKET_PM");
        assertThat(p.getProjectId()).isEqualTo(9L);
        assertThat(project.getProductId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("启停：非法状态拒绝，合法切换 + 审计")
    void changeStatus() {
        Product p = product("PM_NEW", null, null);
        p.setId(3L);
        p.setStatus("ACTIVE");
        p.setDelFlag("0");
        when(productMapper.selectById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.changeStatus(3L, "DELETED", 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("状态非法");
        service.changeStatus(3L, "INACTIVE", 1L, 1L, "MARKET_PM");
        assertThat(p.getStatus()).isEqualTo("INACTIVE");
    }
}