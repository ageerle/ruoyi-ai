package org.ruoyi.service.shortdrama.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.mapper.chat.ChatModelMapper;
import org.ruoyi.service.chat.IChatProviderService;
import org.ruoyi.service.chat.impl.ChatModelServiceImpl;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaAtlasIsolationTest {
    @BeforeAll static void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test-atlas-isolation"), ChatModel.class);
    }

    @Test void sharedBackendStillSelectsOtherProvidersWhileShortDramaRejectsThem() {
        for (String providerCode : List.of("openai", "deepseek", "custom_api")) {
            var mapper = mock(ChatModelMapper.class);
            var providers = mock(IChatProviderService.class);
            var model = new ChatModelVo();
            model.setProviderCode(providerCode); model.setModelName("shared-name"); model.setCategory("chat");
            model.setApiHost("https://other-provider.test/v1"); model.setApiKey("test-only-key");
            when(mapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(model);
            var sharedService = new ChatModelServiceImpl(mapper, providers);
            assertSame(model, sharedService.selectModelByName("shared-name"));
            verify(providers).requireEnabled(providerCode);
            assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.requireProvider(model));
        }
    }
}
