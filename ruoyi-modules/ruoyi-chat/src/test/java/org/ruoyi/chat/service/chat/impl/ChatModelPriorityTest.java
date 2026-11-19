package org.ruoyi.chat.service.chat.impl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.common.chat.domain.vo.chat.*;
import org.ruoyi.chat.mapper.ChatModelMapper;
import org.ruoyi.chat.service.chat.IChatProviderService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@Tag("dev") class ChatModelPriorityTest {
    @BeforeAll static void metadata() { TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test-priority"),ChatModel.class); }
    @Test void availableModelsFilterProviderAndCategoryBeforeStablePriorityOrdering() {
        var mapper=mock(ChatModelMapper.class);var providers=mock(IChatProviderService.class);
        var provider=new org.ruoyi.chat.domain.vo.chat.ChatProviderVo();provider.setProviderCode("atlas");
        when(providers.queryList(any())).thenReturn(List.of(provider));
        var query=new ChatModelBo();query.setCategory("image");query.setProviderCode("atlas");
        new ChatModelServiceImpl(mapper,providers).queryAvailableList(query);
        var capture=ArgumentCaptor.forClass(LambdaQueryWrapper.class);verify(mapper).selectVoList(capture.capture());
        var wrapper=capture.getValue();String sql=wrapper.getSqlSegment();
        assertTrue(sql.contains("provider_code ="));assertFalse(sql.contains("provider_code LIKE"));
        assertTrue(sql.contains("category ="));assertTrue(sql.contains("ORDER BY sort_order ASC,id ASC"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("atlas"));assertTrue(wrapper.getParamNameValuePairs().containsValue("image"));
    }
    @Test void modelVisionFlagIsSavedAndCopiedToAdminDetail() {
        var mapper = mock(ChatModelMapper.class);
        var providers = mock(IChatProviderService.class);
        when(mapper.insert(any(ChatModel.class))).thenReturn(1);
        var service = new ChatModelServiceImpl(mapper, providers);
        var command = new ChatModelBo();
        command.setProviderCode("ark");
        command.setSupportsVision(true);
        service.insertByBo(command);
        var capture = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).insert(capture.capture());
        assertEquals(Boolean.TRUE, capture.getValue().getSupportsVision());

        var current = new ChatModel();
        current.setProviderCode("ark");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(mapper.updateById(any(ChatModel.class))).thenReturn(1);
        command.setId(1L);
        command.setSupportsVision(false);
        service.updateByBo(command);
        verify(mapper).updateById(capture.capture());
        assertEquals(Boolean.FALSE, capture.getValue().getSupportsVision());

        var model = new ChatModelVo();
        model.setSupportsVision(true);
        assertEquals(Boolean.TRUE, ChatModelDetailVo.from(model).getSupportsVision());
    }

    @Test void priorityIsSavedAndPublicOptionsDoNotExposeCredentials() {
        var mapper=mock(ChatModelMapper.class);var providers=mock(IChatProviderService.class);
        when(mapper.insert(any(ChatModel.class))).thenReturn(1);
        var command=new ChatModelBo();command.setCategory("image");command.setProviderCode("atlas");command.setSortOrder(7);command.setApiKey("test-only-secret");
        new ChatModelServiceImpl(mapper,providers).insertByBo(command);
        var capture=ArgumentCaptor.forClass(ChatModel.class);verify(mapper).insert(capture.capture());assertEquals(7,capture.getValue().getSortOrder());
        var model=new ChatModelVo();model.setSortOrder(7);model.setCategory("image");model.setApiKey("test-only-secret");
        var option=ChatModelSelectVo.from(model);assertEquals(7,option.getSortOrder());assertEquals("image",option.getCategory());
        assertTrue(java.util.Arrays.stream(ChatModelSelectVo.class.getDeclaredFields()).noneMatch(f->f.getName().equals("apiKey")));
    }
}
