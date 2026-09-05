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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.CertTemplateMapper;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.service.executor.CertTemplateSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.PersonSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.ProductSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.ProjectSoftDeleteExecutor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-6.2 删除审核引擎一致性单测（事务原子性 + 审计 + 软删除）。
 * 关键约束（G-11）：approve → softDelete → audit.append 三者同事务，任一失败全回滚。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DeleteAuditServiceTest {

    @Mock private DeletionRequestMapper deletionRequestMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProductMapper productMapper;
    @Mock private PersonMapper personMapper;
    @Mock private CertTemplateMapper certTemplateMapper;

    private DeleteAuditService service;

    @BeforeAll
    static void initMybatisMeta() {
        // 让 MyBatis-Plus LambdaUpdateWrapper 能识别 Person 实体
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P062-test"), Person.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeleteAuditService(deletionRequestMapper, auditLogService, List.of(
            new ProjectSoftDeleteExecutor(projectMapper),
            new ProductSoftDeleteExecutor(productMapper),
            new PersonSoftDeleteExecutor(personMapper),
            new CertTemplateSoftDeleteExecutor(certTemplateMapper)
        ));
    }

    private DeletionRequest pending(Long id, String entityType, Long entityId) {
        return DeletionRequest.builder()
            .id(id).entityType(entityType).entityId(entityId).reason("测试删除")
            .requesterId(1L).leaderId(2L).leaderDecision("APPROVE").leaderDecidedAt(new Date())
            .adminDueAt(new Date(System.currentTimeMillis() + 86_400_000))
            .status(DeletionRequestService.ST_ADMIN_REVIEW)
            .build();
    }

    @Test
    @DisplayName("P0-6.2.1 Project 审批通过 → softDelete + audit.append 同事务")
    void projectApproveExecutes() {
        DeletionRequest req = pending(10L, "projects", 100L);
        when(deletionRequestMapper.selectById(10L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Project project = Project.builder().id(100L).code("P").name("T").delFlag("0").build();
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        DeletionRequest after = service.approveAndExecute(10L, 99L);

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_DELETED);
        verify(projectMapper).updateById(any(Project.class));
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(DeleteAuditService.ACTION_DELETE_EXECUTE);
    }

    @Test
    @DisplayName("P0-6.2.2 Product 审批通过 → softDelete + audit.append 同事务")
    void productApproveExecutes() {
        DeletionRequest req = pending(11L, "products", 200L);
        when(deletionRequestMapper.selectById(11L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Product product = Product.builder().id(200L).productCode("P1").productName("T").delFlag("0").build();
        when(productMapper.selectById(200L)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        service.approveAndExecute(11L, 99L);

        verify(productMapper).updateById(any(Product.class));
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.3 Person 审批通过 → audit.append 同事务")
    void personApproveExecutes() {
        DeletionRequest req = pending(12L, "persons", 300L);
        when(deletionRequestMapper.selectById(12L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Person person = Person.builder().id(300L).name("n").personType("MARKET_PM").delFlag("0").build();
        when(personMapper.selectById(300L)).thenReturn(person);
        when(personMapper.update(any(), any())).thenReturn(1);

        service.approveAndExecute(12L, 99L);

        verify(personMapper).update(any(), any());
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.4 CertTemplate 审批通过 → softDelete + audit.append 同事务")
    void certTemplateApproveExecutes() {
        DeletionRequest req = pending(13L, "cert_templates", 400L);
        when(deletionRequestMapper.selectById(13L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        CertTemplate cert = CertTemplate.builder().id(400L).countryCode("SA").certName("SABER").delFlag("0").build();
        when(certTemplateMapper.selectById(400L)).thenReturn(cert);
        when(certTemplateMapper.updateById(any(CertTemplate.class))).thenReturn(1);

        service.approveAndExecute(13L, 99L);

        verify(certTemplateMapper).updateById(any(CertTemplate.class));
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.5 状态机守卫：LEADER_REVIEW → 抛异常，无副作用")
    void leaderReviewRejected() {
        DeletionRequest req = pending(15L, "projects", 600L);
        req.setStatus(DeletionRequestService.ST_LEADER_REVIEW);
        when(deletionRequestMapper.selectById(15L)).thenReturn(req);

        assertThatThrownBy(() -> service.approveAndExecute(15L, 99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("状态机不匹配");

        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.6 不支持的 entity_type → 抛异常，无副作用")
    void unsupportedEntityTypeRejected() {
        DeletionRequest req = pending(16L, "unknown_entities", 700L);
        when(deletionRequestMapper.selectById(16L)).thenReturn(req);

        assertThatThrownBy(() -> service.approveAndExecute(16L, 99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不支持的 entity_type");

        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.7 软删除抛异常 → 不写 audit（事务回滚语义）")
    void softDeleteFailureSkipsAudit() {
        DeletionRequest req = pending(17L, "projects", 800L);
        when(deletionRequestMapper.selectById(17L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        when(projectMapper.selectById(800L)).thenThrow(new RuntimeException("DB连接中断"));

        assertThatThrownBy(() -> service.approveAndExecute(17L, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB连接中断");

        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.8 adminId=null 拒绝执行")
    void nullAdminIdRejected() {
        assertThatThrownBy(() -> service.approveAndExecute(99L, null))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("adminId");

        verify(deletionRequestMapper, never()).selectById(org.mockito.ArgumentMatchers.anyLong());
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.9 supportedEntityTypes 列出全部 4 种类型")
    void supportedEntityTypesListed() {
        assertThat(service.supportedEntityTypes())
            .containsExactlyInAnyOrder("projects", "products", "persons", "cert_templates");
    }

    @Test
    @DisplayName("P0-6.2.10 审计条目字段准确性")
    void auditEntryFieldsAreCorrect() {
        DeletionRequest req = pending(21L, "products", 1200L);
        when(deletionRequestMapper.selectById(21L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Product product = Product.builder().id(1200L).productCode("P-X").delFlag("0").build();
        when(productMapper.selectById(1200L)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        service.approveAndExecute(21L, 42L);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog captured = cap.getValue();
        assertThat(captured.getOperatorId()).isEqualTo(42L);
        assertThat(captured.getEntityType()).isEqualTo("products");
        assertThat(captured.getEntityId()).isEqualTo(1200L);
        assertThat(captured.getReason()).isEqualTo("deletion_request:21");
        assertThat(captured.getAction()).isEqualTo(DeleteAuditService.ACTION_DELETE_EXECUTE);
    }
}