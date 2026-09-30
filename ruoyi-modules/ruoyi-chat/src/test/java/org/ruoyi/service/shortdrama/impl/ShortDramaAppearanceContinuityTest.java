package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;
import org.ruoyi.mapper.shortdrama.ShortDramaCharacterMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaCharacterAppearanceMapper;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaAppearanceContinuityTest {
    @Test
    void detailUsesEditedAppearanceInsteadOfStaleCharacterDescription() {
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        var characters = mock(ShortDramaCharacterMapper.class);
        var appearances = mock(ShortDramaCharacterAppearanceMapper.class);
        ReflectionTestUtils.setField(service, "characterMapper", characters);
        ReflectionTestUtils.setField(service, "characterAppearanceMapper", appearances);
        var character = new ShortDramaCharacter();
        character.setId(1L);
        character.setName("陈念");
        character.setVisualDescription("旧版深色工装");
        var appearance = new ShortDramaCharacterAppearance();
        appearance.setChangeReason("初始形象");
        appearance.setDescription("26岁，米白色长袖衬衫，深蓝牛仔裤");
        when(characters.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(character));
        when(appearances.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(appearance));
        String constraints = service.buildCharacterAppearanceConstraints(2L);
        assertTrue(constraints.contains("陈念 / 初始形象"));
        assertTrue(constraints.contains("米白色长袖衬衫"));
        assertFalse(constraints.contains("旧版深色工装"));
    }
}
