package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.system.domain.SysConfig;
import org.ruoyi.system.mapper.SysConfigMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class SysConfigUpsertTest {

    private static final String KEY = "chat.image.visionModels";
    private static final String NAME = "聊天默认视觉模型";
    private final SysConfigMapper mapper = mock(SysConfigMapper.class);
    private final SysConfigServiceImpl config = new SysConfigServiceImpl(mapper, null);

    @Test
    void createsBuiltInSettingWhenTenantHasNoExistingRow() {
        when(mapper.insert(any(SysConfig.class))).thenReturn(1);

        assertEquals("ark-vision", config.upsertConfigValue(KEY, NAME, "ark-vision"));

        var saved = ArgumentCaptor.forClass(SysConfig.class);
        verify(mapper).insert(saved.capture());
        assertEquals(KEY, saved.getValue().getConfigKey());
        assertEquals(NAME, saved.getValue().getConfigName());
        assertEquals("ark-vision", saved.getValue().getConfigValue());
        assertEquals(SystemConstants.YES, saved.getValue().getConfigType());
        verify(mapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void updatesOnlyValueOfTheExistingParameter() {
        var current = new SysConfig();
        current.setConfigId(27L);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(mapper.updateById(any(SysConfig.class))).thenReturn(1);

        assertEquals("other-vision", config.upsertConfigValue(KEY, NAME, "other-vision"));

        var saved = ArgumentCaptor.forClass(SysConfig.class);
        verify(mapper).updateById(saved.capture());
        assertEquals(27L, saved.getValue().getConfigId());
        assertEquals("other-vision", saved.getValue().getConfigValue());
        assertNull(saved.getValue().getConfigName());
        assertNull(saved.getValue().getConfigKey());
        verify(mapper, never()).insert(any(SysConfig.class));
    }
}
