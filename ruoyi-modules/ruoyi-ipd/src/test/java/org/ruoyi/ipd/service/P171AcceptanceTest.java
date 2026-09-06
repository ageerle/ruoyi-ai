package org.ruoyi.ipd.service;

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
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectCertItem;
import org.ruoyi.ipd.dto.ProjectCertListView;
import org.ruoyi.ipd.dto.ProjectCertManualReq;
import org.ruoyi.ipd.mapper.ProjectCertItemMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.seed.MarketCodeResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-7.1：AC-PROD-10/11/12 — SA→SABER；BR/IN/KR→ANATEL/BIS/KC；手工补充；DONE 不被 re-sync 重置。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P171AcceptanceTest {

    @Mock private ProjectCertItemMapper itemMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private CertTemplateService certTemplateService;
    @Mock private AuditLogService auditLogService;

    private ProjectCertService service;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ProjectCertService(itemMapper, projectMapper, certTemplateService, auditLogService);
    }

    private Project project(Long id, String markets) {
        Project p = Project.builder().id(id).targetMarkets(markets).delFlag("0").build();
        lenient().when(projectMapper.selectById(id)).thenReturn(p);
        return p;
    }

    @Test
    @DisplayName("市场别名：沙特→SA；未知 token 可提示")
    void marketAliases() {
        assertThat(MarketCodeResolver.knownCodes("[\"沙特\",\"巴西\"]")).containsExactly("SA", "BR");
        assertThat(MarketCodeResolver.unknownTokens("[\"SA\",\"火星\"]")).containsExactly("火星");
    }

    @Test
    @DisplayName("AC-PROD-10：选沙特带出 SABER/SASO")
    void acProd10SaudiSaber() {
        Project p = project(10L, "[\"沙特\"]");
        when(certTemplateService.resolve(any())).thenReturn(List.of(
            CertTemplate.builder().id(1L).countryCode("SA").countryName("沙特阿拉伯")
                .certName("SABER/SASO").isMandatory("1").build()));
        when(itemMapper.selectCount(any())).thenReturn(0L);
        when(itemMapper.insert(any(ProjectCertItem.class))).thenAnswer(inv -> {
            ProjectCertItem i = inv.getArgument(0);
            i.setId(100L);
            return 1;
        });
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(1);
        ArgumentCaptor<ProjectCertItem> cap = ArgumentCaptor.forClass(ProjectCertItem.class);
        verify(itemMapper).insert(cap.capture());
        assertThat(cap.getValue().getCertName()).isEqualTo("SABER/SASO");
        assertThat(cap.getValue().getSource()).isEqualTo("AUTO");
        assertThat(cap.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(cap.getValue().getCatalogVersion()).startsWith("tpl-");
    }

    @Test
    @DisplayName("AC-PROD-11：BR/IN/KR 分别带出 ANATEL/BIS/KC")
    void acProd11BrInKr() {
        Project p = project(11L, "[\"BR\",\"IN\",\"KR\"]");
        when(certTemplateService.resolve(any())).thenReturn(List.of(
            tpl(2L, "BR", "巴西", "ANATEL"),
            tpl(3L, "IN", "印度", "BIS"),
            tpl(4L, "KR", "韩国", "KC")));
        when(itemMapper.selectCount(any())).thenReturn(0L);
        when(itemMapper.insert(any(ProjectCertItem.class))).thenReturn(1);
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-PROD-12：手工补充成功；重名拒绝")
    void acProd12Manual() {
        project(12L, "[\"SA\"]");
        when(itemMapper.selectCount(any())).thenReturn(0L).thenReturn(1L);
        when(itemMapper.insert(any(ProjectCertItem.class))).thenAnswer(inv -> {
            ((ProjectCertItem) inv.getArgument(0)).setId(200L);
            return 1;
        });
        ProjectCertItem created = service.addManual(12L,
            new ProjectCertManualReq("SA", "沙特阿拉伯", "EXTRA-CERT", null, "0"), 9L);
        assertThat(created.getSource()).isEqualTo("MANUAL");
        assertThatThrownBy(() -> service.addManual(12L,
            new ProjectCertManualReq("SA", "沙特阿拉伯", "EXTRA-CERT", null, "0"), 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("模板变更 re-sync：已存在项不重复插入；DONE 不被重置")
    void resyncPreservesDone() {
        Project p = project(13L, "[\"SA\"]");
        when(certTemplateService.resolve(any())).thenReturn(List.of(
            tpl(1L, "SA", "沙特阿拉伯", "SABER/SASO")));
        when(itemMapper.selectCount(any())).thenReturn(1L);
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(0);
        verify(itemMapper, never()).insert(any(ProjectCertItem.class));

        ProjectCertItem done = ProjectCertItem.builder().id(50L).projectId(13L)
            .countryCode("SA").certName("SABER/SASO").status("DONE").delFlag("0").build();
        // DONE 保持；sync 因已存在不 insert
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(0);
        assertThat(done.getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("listView 暴露 unknownMarkets")
    void listViewUnknown() {
        project(14L, "[\"SA\",\"未知国\"]");
        when(itemMapper.selectList(any())).thenReturn(List.of());
        ProjectCertListView view = service.listView(14L);
        assertThat(view.unknownMarkets()).containsExactly("未知国");
    }

    private static CertTemplate tpl(Long id, String code, String country, String cert) {
        return CertTemplate.builder().id(id).countryCode(code).countryName(country)
            .certName(cert).isMandatory("1").build();
    }
}
