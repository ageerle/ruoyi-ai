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
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.mapper.ProductGroupMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品组 CRUD 单测（P2-1；组长随 API 同步 A3）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProductGroupServiceTest {

    @Mock
    private ProductGroupMapper mapper;
    @Mock
    private AuditLogService auditLogService;

    private ProductGroupService service;

    @BeforeEach
    void setUp() {
        service = new ProductGroupService(mapper, auditLogService);
    }

    private ProductGroup group(String name, Long leader) {
        return ProductGroup.builder()
            .id(1L)
            .groupName(name)
            .leaderPersonId(leader)
            .delFlag("0")
            .build();
    }

    @Test
    @DisplayName("list：返回非删除产品组")
    void listAll() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            group("BioCV 产品组", 1001L),
            group("海外系统组", 1002L)));
        List<ProductGroup> all = service.listAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(ProductGroup::getGroupName)
            .containsExactly("BioCV 产品组", "海外系统组");
    }

    @Test
    @DisplayName("create：组名必填")
    void createValidation() {
        assertThatThrownBy(() -> service.create(group("", 1001L), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("产品组组名必填");
    }

    @Test
    @DisplayName("create：同名产品组去重")
    void createDuplicate() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(group("BioCV 产品组", 1001L), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已存在同名");
    }

    @Test
    @DisplayName("create 成功 + 审计；remove 禁止直删旁路（P0-6.2）")
    void createAndRemove() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        ProductGroup created = service.create(group("新组", 1001L), 1L);
        assertThat(created.getDelFlag()).isEqualTo("0");
        verify(auditLogService).append(any());

        assertThatThrownBy(() -> service.remove(1L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("禁止直删");
    }

    @Test
    @DisplayName("updateLeader：组长必填（leaderPersonId null 拒）")
    void updateLeaderValidation() {
        assertThatThrownBy(() -> service.updateLeader(1L, null, 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("组长必填");
        verify(mapper, never()).updateById(any(ProductGroup.class));
    }
}
