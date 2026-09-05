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
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.SoftDeletable;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-6.2 验收单测：审计一致性、实体类型覆盖、回滚一致性。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P062AcceptanceTest {

    @Mock private DeletionRequestMapper deletionRequestMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProductMapper productMapper;
    @Mock private PersonMapper personMapper;
    @Mock private CertTemplateMapper certTemplateMapper;

    private DeleteAuditService service;

    @BeforeAll
    static void initMeta() {
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

    @Test
    @DisplayName("P0-6.2.A1 4 个核心实体类型都实现 SoftDeletable 接口")
    void allEntitiesImplementSoftDeletable() {
        assertThat(SoftDeletable.class.isAssignableFrom(Project.class)).isTrue();
        assertThat(SoftDeletable.class.isAssignableFrom(Product.class)).isTrue();
        assertThat(SoftDeletable.class.isAssignableFrom(Person.class)).isTrue();
        assertThat(SoftDeletable.class.isAssignableFrom(CertTemplate.class)).isTrue();
    }

    @Test
    @DisplayName("P0-6.2.A2 4 类实体各 1 次 approve → 恰好 4 条 audit")
    void auditCountEqualsExecutions() {
        for (int i = 0; i < 4; i++) {
            String type = List.of("projects", "products", "persons", "cert_templates").get(i);
            Long targetId = (long) (1000 + i);
            DeletionRequest req = DeletionRequest.builder()
                .id((long) (i + 1)).entityType(type).entityId(targetId).reason("AC")
                .requesterId(1L).status(DeletionRequestService.ST_ADMIN_REVIEW)
                .adminDueAt(new Date()).build();
            when(deletionRequestMapper.selectById(req.getId())).thenReturn(req);
            when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
            switch (type) {
                case "projects" -> {
                    Project p = Project.builder().id(targetId).code("C").delFlag("0").build();
                    when(projectMapper.selectById(targetId)).thenReturn(p);
                    when(projectMapper.updateById(any(Project.class))).thenReturn(1);
                }
                case "products" -> {
                    Product p = Product.builder().id(targetId).productCode("X").delFlag("0").build();
                    when(productMapper.selectById(targetId)).thenReturn(p);
                    when(productMapper.updateById(any(Product.class))).thenReturn(1);
                }
                case "persons" -> {
                    Person p = Person.builder().id(targetId).name("n").delFlag("0").build();
                    when(personMapper.selectById(targetId)).thenReturn(p);
                    when(personMapper.update(any(), any())).thenReturn(1);
                }
                case "cert_templates" -> {
                    CertTemplate c = CertTemplate.builder().id(targetId).countryCode("SA").certName("X").delFlag("0").build();
                    when(certTemplateMapper.selectById(targetId)).thenReturn(c);
                    when(certTemplateMapper.updateById(any(CertTemplate.class))).thenReturn(1);
                }
            }
            service.approveAndExecute(req.getId(), 99L);
        }
        verify(auditLogService, times(4)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("P0-6.2.A3 软删除执行后实体 del_flag='1'")
    void targetEntityFlaggedAfterExecute() {
        DeletionRequest req = DeletionRequest.builder()
            .id(1L).entityType("projects").entityId(2000L).requesterId(1L)
            .status(DeletionRequestService.ST_ADMIN_REVIEW).build();
        when(deletionRequestMapper.selectById(1L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Project project = Project.builder().id(2000L).code("P").delFlag("0").build();
        when(projectMapper.selectById(2000L)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        service.approveAndExecute(1L, 99L);

        ArgumentCaptor<Project> cap = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(cap.capture());
        assertThat(cap.getValue().getDelFlag()).isEqualTo("1");
    }

    @Test
    @DisplayName("P0-6.2.A4 目标已软删 → DELETE_NOOP 且不重复 UPDATE")
    void alreadyDeletedIsNoop() {
        DeletionRequest req = DeletionRequest.builder()
            .id(1L).entityType("projects").entityId(2000L).requesterId(1L)
            .status(DeletionRequestService.ST_ADMIN_REVIEW).build();
        when(deletionRequestMapper.selectById(1L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        Project project = Project.builder().id(2000L).code("P").delFlag("1").build();
        when(projectMapper.selectById(2000L)).thenReturn(project);

        service.approveAndExecute(1L, 99L);

        verify(projectMapper, times(0)).updateById(any(Project.class));
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo(DeleteAuditService.ACTION_DELETE_NOOP);
    }

    @Test
    @DisplayName("P0-6.2.A5 目标不存在 → DELETE_NOOP")
    void missingTargetIsNoop() {
        DeletionRequest req = DeletionRequest.builder()
            .id(2L).entityType("products").entityId(3000L).requesterId(1L)
            .status(DeletionRequestService.ST_ADMIN_REVIEW).build();
        when(deletionRequestMapper.selectById(2L)).thenReturn(req);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenReturn(1);
        when(productMapper.selectById(3000L)).thenReturn(null);

        service.approveAndExecute(2L, 99L);

        verify(productMapper, times(0)).updateById(any(Product.class));
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo(DeleteAuditService.ACTION_DELETE_NOOP);
    }
}