package org.ruoyi.chat.service.agent;

import dev.langchain4j.skills.Skills;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.chat.domain.AgentSkill;
import org.ruoyi.chat.mapper.AgentSkillMapper;
import org.ruoyi.common.core.exception.ServiceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class AgentSkillServiceTest {

    private final AgentSkillMapper mapper = mock(AgentSkillMapper.class);
    private final AgentSkillService service = new AgentSkillService(mapper);

    @Test
    void readsCurrentDefinitionAndSkipsUnavailableSkills() {
        when(mapper.selectList(any())).thenReturn(
            List.of(skill("service-status", "旧版指令")),
            List.of(skill("service-status", "新版指令")),
            List.of());

        var names = List.of("service-status", "missing", "service-status");
        var initial = service.resolveSkills(names);
        assertEquals(1, initial.size());
        assertEquals("旧版指令", initial.getFirst().content());

        var updated = service.resolveSkills(names);
        assertEquals("新版指令", updated.getFirst().content());
        String catalog = Skills.from(updated).formatAvailableSkills();
        assertTrue(catalog.contains("service-status"));
        assertFalse(catalog.contains("missing"));
        assertFalse(catalog.contains("新版指令"));
        assertTrue(service.resolveSkills(names).isEmpty());
    }

    @Test
    void emptyBindingsDoNotReadTheCatalog() {
        assertTrue(service.resolveSkills(null).isEmpty());
        assertTrue(service.resolveSkills(List.of()).isEmpty());
        service.requireEnabled(List.of());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsNewBindingsToUnavailableSkills() {
        when(mapper.selectCount(any())).thenReturn(1L, 0L);
        service.requireEnabled(List.of("service-status"));
        assertThrows(ServiceException.class, () -> service.requireEnabled(List.of("missing")));
    }

    private static AgentSkill skill(String name, String content) {
        AgentSkill record = new AgentSkill();
        record.setName(name);
        record.setDescription("查询演示服务状态");
        record.setStatus("0");
        record.setContent(content);
        return record;
    }
}
