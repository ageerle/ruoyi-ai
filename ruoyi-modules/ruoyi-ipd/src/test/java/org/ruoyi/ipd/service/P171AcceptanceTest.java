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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeAll
    static void initMeta() {
        // ProjectCertService 内联构造 LambdaQueryWrapper<ProjectCertItem>，纯 Mockito 环境需预初始化
        // TableInfo 元数据（lambda cache），否则运行时抛 "can not find lambda cache for this entity"。
        // 模式对齐同包 P062AcceptanceTest.initMeta()。
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "P171-test");
        TableInfoHelper.initTableInfo(assistant, ProjectCertItem.class);
    }

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
        // R8-P0-6 批量契约：sync 走一次性 selectList 判重 + insertBatch，不再逐条 selectCount/insert
        when(itemMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectCertItem>> cap = ArgumentCaptor.forClass(List.class);
        verify(itemMapper).insertBatch(cap.capture(), eq(200));
        assertThat(cap.getValue()).hasSize(1);
        ProjectCertItem item = cap.getValue().get(0);
        assertThat(item.getCertName()).isEqualTo("SABER/SASO");
        assertThat(item.getSource()).isEqualTo("AUTO");
        assertThat(item.getStatus()).isEqualTo("PENDING");
        assertThat(item.getCatalogVersion()).startsWith("tpl-");
    }

    @Test
    @DisplayName("AC-PROD-11：BR/IN/KR 分别带出 ANATEL/BIS/KC")
    void acProd11BrInKr() {
        Project p = project(11L, "[\"BR\",\"IN\",\"KR\"]");
        when(certTemplateService.resolve(any())).thenReturn(List.of(
            tpl(2L, "BR", "巴西", "ANATEL"),
            tpl(3L, "IN", "印度", "BIS"),
            tpl(4L, "KR", "韩国", "KC")));
        // R8-P0-6 批量契约：一次性 selectList 判重 + insertBatch
        when(itemMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectCertItem>> cap = ArgumentCaptor.forClass(List.class);
        verify(itemMapper).insertBatch(cap.capture(), eq(200));
        assertThat(cap.getValue()).hasSize(3);
        assertThat(cap.getValue()).extracting(ProjectCertItem::getCertName)
            .containsExactly("ANATEL", "BIS", "KC");
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
        // R8-P0-6 批量契约：已存在项由 selectList 返回（含 DONE 项），sync 判重后跳过、不 insertBatch
        ProjectCertItem done = ProjectCertItem.builder().id(50L).projectId(13L)
            .countryCode("SA").certName("SABER/SASO").status("DONE").delFlag("0").build();
        when(itemMapper.selectList(any())).thenReturn(List.of(done));
        assertThat(service.syncFromProject(p, 9L)).isEqualTo(0);
        verify(itemMapper, never()).insertBatch(anyCollection(), anyInt());

        // DONE 保持；sync 因已存在不 insertBatch（重入一次验证幂等）
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
