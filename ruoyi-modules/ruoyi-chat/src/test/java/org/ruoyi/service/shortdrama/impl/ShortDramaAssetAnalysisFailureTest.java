package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaAssetAnalysisFailureTest {
    private static final String COMBINED = "{\"characters\":[{\"name\":\"朱承晏\",\"roleLevel\":\"S\",\"visualDescription\":\"独立人物形象\"}],\"locations\":[{\"name\":\"县衙_白天\",\"descriptions\":[\"木桌与旧墙\"]}],\"props\":[{\"name\":\"风筝\",\"description\":\"竹骨与布翼\"}]}";
    private static final String CHARACTERS = "[{\"name\":\"朱承晏\",\"roleLevel\":\"S\",\"visualKeywords\":\"青色县令常服\"}]";
    private static final String LOCATIONS = "[{\"name\":\"县衙_白天\",\"summary\":\"简陋县衙\",\"descriptions\":[\"木桌与旧墙\"]}]";
    private static final String VISUAL = "{\"characters\":[{\"name\":\"朱承晏\",\"appearances\":[{\"descriptions\":[\"青色常服，清晰五官与衣纹\"]}]}]}";
    private ShortDramaServiceImpl service;
    private ChatModel model;
    private ShortDramaVisualAssetService visualAssets;
    private ShortDramaScript script;
    private ShortDramaCharacterMapper characters;
    private ShortDramaCharacterAppearanceMapper appearances;
    private ShortDramaLocationMapper locations;
    private ShortDramaAudioMapper audios;
    private PlatformTransactionManager transactions;
    private ShortDramaDetailVo result;

    @BeforeAll static void jsonMapper() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
    }

    @BeforeEach void fixture() {
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        model = mock(ChatModel.class);
        visualAssets = mock(ShortDramaVisualAssetService.class);
        ReflectionTestUtils.setField(service, "visualAssets", visualAssets);
        ReflectionTestUtils.setField(service, "assetGenerationStates", new java.util.concurrent.ConcurrentHashMap<>());
        ReflectionTestUtils.setField(service, "storyboardGenerationStates", new java.util.concurrent.ConcurrentHashMap<>());
        var catalog = mock(ShortDramaSkillCatalog.class);
        when(catalog.snapshot(any())).thenReturn(new ShortDramaSkillCatalog.Snapshot(10L, "version", ""));
        ReflectionTestUtils.setField(service, "skillCatalog", catalog);
        characters = mock(ShortDramaCharacterMapper.class);
        appearances = mock(ShortDramaCharacterAppearanceMapper.class);
        locations = mock(ShortDramaLocationMapper.class);
        audios = mock(ShortDramaAudioMapper.class);
        transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "transactionTemplate", new TransactionTemplate(transactions));
        ReflectionTestUtils.setField(service, "characterMapper", characters);
        ReflectionTestUtils.setField(service, "characterAppearanceMapper", appearances);
        ReflectionTestUtils.setField(service, "locationMapper", locations);
        ReflectionTestUtils.setField(service, "audioMapper", audios);
        var projects = mock(ShortDramaProjectMapper.class);
        var scripts = mock(ShortDramaScriptMapper.class);
        var models = mock(IChatModelService.class);
        var factory = mock(ChatServiceFactory.class);
        var chat = mock(AbstractChatService.class);
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        ReflectionTestUtils.setField(service, "scriptMapper", scripts);
        ReflectionTestUtils.setField(service, "chatModelService", models);
        ReflectionTestUtils.setField(service, "chatServiceFactory", factory);
        var project = new ShortDramaProject(); project.setId(10L); project.setUserId(1L);
        when(projects.selectById(10L)).thenReturn(project);
        script = new ShortDramaScript(); script.setId(20L); script.setProjectId(10L);
        script.setScriptText("朱承晏在县衙召集工匠，检查贫困县的水渠。");
        when(scripts.selectById(20L)).thenReturn(script);
        when(scripts.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(script);
        var configuration = new ChatModelVo(); configuration.setModelName("writing-model"); configuration.setProviderCode("atlas");
        when(models.selectModelByName("writing-model")).thenReturn(configuration);
        when(factory.getOriginalService("atlas")).thenReturn(chat);
        when(chat.buildChatModel(eq(configuration), any())).thenReturn(model);
        when(model.chat(anyString())).thenReturn(COMBINED);
        when(locations.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        when(characters.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        result = new ShortDramaDetailVo();
        doReturn(result).when(service).getDetail(10L, 1L);
    }

    @Test void transportFailureMakesOneCallAndCannotWriteAssets() {
        when(model.chat(anyString())).thenThrow(new IllegalStateException("Received RST_STREAM Internal error"));
        assertTrue(assertThrows(IllegalStateException.class, this::analyze).getMessage().contains("Received RST_STREAM"));
        verify(model, times(1)).chat(anyString());
        verifyNoInteractions(characters, appearances, locations, audios, transactions, visualAssets);
    }
    @Test void blankScriptCannotBeReplacedByWorldbuilding() {
        script.setScriptText(" "); script.setOutlineText(null); script.setWorldbuilding("完整世界观");
        assertThrows(IllegalArgumentException.class, this::analyze);
        verifyNoInteractions(model, characters, appearances, locations, transactions, visualAssets);
    }
    @Test void incompleteCombinedResultCannotPartiallySaveCharacters() {
        for (String response : List.of("not json", COMBINED.replace("竹骨与布翼", ""), COMBINED.replace("独立人物形象", ""))) {
            when(model.chat(anyString())).thenReturn(response);
            assertThrows(IllegalStateException.class, this::analyze);
        }
        verifyNoInteractions(characters, appearances, locations, audios, transactions, visualAssets);
    }
    @Test void validCombinedResultSavesAllCategoriesInOneTransactionWithoutDeletingAssets() {
        assertSame(result, analyze());
        verify(model, times(1)).chat(anyString());
        verify(characters).insert(any(ShortDramaCharacter.class));
        verify(appearances).insert(any(ShortDramaCharacterAppearance.class));
        verify(locations).insert(any(ShortDramaLocation.class));
        verify(visualAssets).saveAnalyzedProps(eq(10L), argThat(items -> items.size() == 1), eq(1L));
        verify(transactions).commit(any()); verify(transactions, never()).rollback(any());
        verify(characters, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(locations, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verifyNoInteractions(audios);
    }
    @Test void sameNamedApprovedAssetsKeepIdsDescriptionsAndMedia() {
        var approvedCharacter = new ShortDramaCharacter(); approvedCharacter.setId(99L); approvedCharacter.setName("朱承晏"); approvedCharacter.setVisualDescription("已批准形象");
        var approvedLocation = new ShortDramaLocation(); approvedLocation.setId(98L); approvedLocation.setName("县衙_白天"); approvedLocation.setDescriptions("已批准场景");
        when(characters.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(approvedCharacter));
        when(locations.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(approvedLocation));
        analyze();
        verify(characters, never()).insert(any(ShortDramaCharacter.class)); verify(characters, never()).updateById(any(ShortDramaCharacter.class));
        verify(locations, never()).insert(any(ShortDramaLocation.class)); verify(locations, never()).updateById(any(ShortDramaLocation.class));
        verifyNoInteractions(appearances); assertEquals("已批准形象", approvedCharacter.getVisualDescription());
    }
    @Test void propPersistenceFailureRollsBackAllCategories() {
        doThrow(new IllegalStateException("prop save failed")).when(visualAssets).saveAnalyzedProps(anyLong(), anyList(), anyLong());
        assertThrows(IllegalStateException.class, this::analyze);
        verify(transactions).rollback(any()); verify(transactions, never()).commit(any());
        verify(service, never()).getDetail(10L, 1L);
    }
    private ShortDramaDetailVo analyze() { return service.analyzeAssets(10L, 20L, 1L, "writing-model"); }
}
