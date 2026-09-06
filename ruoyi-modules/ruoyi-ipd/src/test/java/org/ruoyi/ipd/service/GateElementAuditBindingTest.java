package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SEC-REV round 2：GateElementService 审计字段绑 actor（Bug#7）回归。
 *
 * <p>Bug 复现：create/update/publish/archive/disable/copy/revert 7 个写入方法之前
 * 未设置 entity.createBy/updateBy 字段，导致数据库行 create_by/update_by 为 null，
 * 客户端可透传伪造审计身份。
 *
 * <p>修复：在每个写入前显式 setCreateBy(actor.id()) / setUpdateBy(actor.id())。
 */
@Tag("dev")
class GateElementAuditBindingTest {

    private static final IpdActor ACTOR = new IpdActor(161L, "admin", "SUPER_ADMIN", 11L);
    private static final Long IMPERSONATED_USER = 999L;

    private GateElementMapper mapper;
    private AuditLogService auditLogService;
    private AuditLogMapper auditLogMapper;
    private GateElementService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "GAuditB");
        TableInfoHelper.initTableInfo(assistant, GateElement.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(GateElementMapper.class);
        auditLogService = mock(AuditLogService.class);
        auditLogMapper = mock(AuditLogMapper.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement x = inv.getArgument(0);
            x.setId(9L);
            return 1;
        });
        service = new GateElementService(mapper, auditLogService, auditLogMapper);
    }

    private GateElement newElement() {
        return GateElement.builder().gateCode("G1").elementCode("G1-R2").elementName("回归测")
            .isVeto("0").sortOrder(1).build();
    }

    @Test
    @DisplayName("Bug#7: create() 将 createBy/updateBy 绑 actor.id()（拒绝透传）")
    void create_bindsCreateByAndUpdateByToActor() {
        when(mapper.selectCount(any())).thenReturn(0L);
        GateElement patch = newElement();
        // 客户端尝试伪造审计身份 —— 应被覆盖
        patch.setCreateBy(IMPERSONATED_USER);
        patch.setUpdateBy(IMPERSONATED_USER);
        patch.setCreateTime(new java.util.Date());

        GateElement out = service.create(patch, ACTOR);

        assertThat(out.getCreateBy()).isEqualTo(ACTOR.id());
        assertThat(out.getUpdateBy()).isEqualTo(ACTOR.id());
    }

    @Test
    @DisplayName("Bug#7: update() 将 updateBy 绑 actor.id()（拒绝透传）")
    void update_bindsUpdateByToActor() {
        GateElement exist = newElement();
        exist.setId(7L);
        exist.setStatus("draft");
        exist.setVersion(0);
        exist.setEnabled("0");
        when(mapper.selectById(7L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);
        GateElement patch = GateElement.builder().id(7L).elementName("改名").build();
        patch.setUpdateBy(IMPERSONATED_USER); // 客户端伪造

        GateElement out = service.update(patch, ACTOR);

        assertThat(out.getUpdateBy()).isEqualTo(ACTOR.id());
    }

    @Test
    @DisplayName("Bug#7: publish() 将 updateBy 绑 actor.id()")
    void publish_bindsUpdateByToActor() {
        GateElement exist = newElement();
        exist.setId(7L);
        exist.setStatus("draft");
        exist.setVersion(0);
        exist.setEnabled("0");
        when(mapper.selectById(7L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);

        GateElement out = service.publish(7L, ACTOR);

        assertThat(out.getUpdateBy()).isEqualTo(ACTOR.id());
    }

    @Test
    @DisplayName("Bug#7: archive() 将 updateBy 绑 actor.id()")
    void archive_bindsUpdateByToActor() {
        GateElement exist = newElement();
        exist.setId(7L);
        exist.setStatus("published");
        exist.setEnabled("1");
        when(mapper.selectById(7L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);

        GateElement out = service.archive(7L, ACTOR);

        assertThat(out.getUpdateBy()).isEqualTo(ACTOR.id());
    }

    @Test
    @DisplayName("Bug#7: disable() 将 updateBy 绑 actor.id()")
    void disable_bindsUpdateByToActor() {
        GateElement exist = newElement();
        exist.setId(7L);
        exist.setStatus("published");
        exist.setEnabled("1");
        when(mapper.selectById(7L)).thenReturn(exist);
        when(mapper.updateById(any(GateElement.class))).thenReturn(1);

        GateElement out = service.disable(7L, ACTOR);

        assertThat(out.getUpdateBy()).isEqualTo(ACTOR.id());
    }

    @Test
    @DisplayName("Bug#7: copy() 将 createBy/updateBy 绑 actor.id()")
    void copy_bindsCreateByAndUpdateByToActor() {
        GateElement source = newElement();
        source.setId(7L);
        source.setStatus("published");
        source.setEnabled("1");
        when(mapper.selectById(7L)).thenReturn(source);
        when(mapper.selectCount(any())).thenReturn(0L);

        GateElement clone = service.copy(7L, "G1-R2-v2", ACTOR);

        assertThat(clone.getCreateBy()).isEqualTo(ACTOR.id());
        assertThat(clone.getUpdateBy()).isEqualTo(ACTOR.id());
    }
}
