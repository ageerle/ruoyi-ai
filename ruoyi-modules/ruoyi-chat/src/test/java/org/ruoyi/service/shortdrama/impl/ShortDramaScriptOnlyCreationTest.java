package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.bo.shortdrama.ShortDramaIdeaBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaProjectBo;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaScriptOnlyCreationTest {
    private static final String RESPONSE = "{\"projectName\":\"御风\",\"scriptName\":\"云间行\",\"tone\":\"古风幻想\"}\n===SCRIPT===\n《御风》第一集：云间行\n\n一　山谷上空。晴日。\n匠人乘竹骨布面风筝滑翔。\n\n第一集完。";
    private ShortDramaServiceImpl service;
    private ChatModel model;
    private StreamingChatModel streaming;
    private AbstractChatService chat;
    private ShortDramaProjectMapper projects;
    private ShortDramaScriptMapper scripts;
    private ShortDramaCharacterMapper characters;
    private ShortDramaLocationMapper locations;
    private ShortDramaStoryboardMapper storyboards;
    private ShortDramaVisualAssetService visualAssets;
    private ConcurrentHashMap<Object, Object> emitters;
    private CountDownLatch scriptSaved;

    @BeforeAll static void jsonMapper() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        var converter = mock(io.github.linpeilie.Converter.class);
        when(converter.convert(any(Object.class), any(Class.class))).thenAnswer(call -> {
            Object target = ((Class<?>) call.getArgument(1)).getDeclaredConstructor().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(call.getArgument(0), target);
            return target;
        });
        factory.registerSingleton("converter", converter);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
        var context = new org.springframework.context.support.GenericApplicationContext(factory);
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test", java.util.Map.of("tenant.enable", "false")));
        context.refresh();
        new cn.hutool.extra.spring.SpringUtil().setApplicationContext(context);
    }

    @BeforeEach void fixture() {
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        model = mock(ChatModel.class); streaming = mock(StreamingChatModel.class);
        chat = mock(AbstractChatService.class);
        projects = mock(ShortDramaProjectMapper.class); scripts = mock(ShortDramaScriptMapper.class);
        characters = mock(ShortDramaCharacterMapper.class); locations = mock(ShortDramaLocationMapper.class);
        storyboards = mock(ShortDramaStoryboardMapper.class); visualAssets = mock(ShortDramaVisualAssetService.class);
        var models = mock(IChatModelService.class); var factory = mock(ChatServiceFactory.class);
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        ReflectionTestUtils.setField(service, "scriptMapper", scripts);
        ReflectionTestUtils.setField(service, "characterMapper", characters);
        ReflectionTestUtils.setField(service, "locationMapper", locations);
        ReflectionTestUtils.setField(service, "storyboardMapper", storyboards);
        ReflectionTestUtils.setField(service, "visualAssets", visualAssets);
        ReflectionTestUtils.setField(service, "chatModelService", models);
        ReflectionTestUtils.setField(service, "chatServiceFactory", factory);
        emitters = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(service, "activeEmitters", emitters);
        ReflectionTestUtils.setField(service, "planningProgress", new ConcurrentHashMap<>());
        var configuration = new ChatModelVo(); configuration.setModelName("writing-model"); configuration.setProviderCode("atlas");
        when(models.selectModelByName("writing-model")).thenReturn(configuration);
        when(factory.getOriginalService("atlas")).thenReturn(chat);
        when(chat.buildChatModel(eq(configuration), any())).thenReturn(model);
        when(chat.buildStreamingChatModel(eq(configuration), any())).thenReturn(streaming);
        when(model.chat(anyString())).thenReturn(RESPONSE);
        doAnswer(call -> {
            StreamingChatResponseHandler handler = call.getArgument(1);
            handler.onPartialResponse(RESPONSE);
            handler.onCompleteResponse(null);
            return null;
        }).when(streaming).chat(anyList(), any(StreamingChatResponseHandler.class));
        scriptSaved = new CountDownLatch(1);
        when(scripts.insert(any(ShortDramaScript.class))).thenAnswer(call -> { scriptSaved.countDown(); return 1; });
        doReturn(new ShortDramaDetailVo()).when(service).getDetail(anyLong(), eq(1L));
    }

    private ShortDramaIdeaBo idea(Boolean legacyFlag) {
        var idea = new ShortDramaIdeaBo(); idea.setIdea("古人乘竹布风筝翱翔"); idea.setModel("writing-model");
        idea.setScriptOnly(legacyFlag); return idea;
    }

    @ParameterizedTest @NullSource @ValueSource(booleans = {false, true})
    void synchronousCreationStopsAfterSavingScriptForEveryLegacyFlag(Boolean legacyFlag) {
        assertNotNull(service.createFromIdea(idea(legacyFlag), 1L));
        verify(model, times(1)).chat(anyString());
        verify(projects).insert(argThat((ShortDramaProject project) -> "古人乘竹布风筝翱翔".equals(project.getOriginalIdea())));
        verify(scripts).insert(argThat((ShortDramaScript script) -> script.getScriptText().contains("竹骨布面风筝")));
        verifyNoInteractions(characters, locations, storyboards, visualAssets);
    }

    @ParameterizedTest @NullSource @ValueSource(booleans = {false, true})
    void streamingCreationCompletesWithoutStartingLaterStages(Boolean legacyFlag) throws Exception {
        assertNotNull(service.createFromIdeaStream(idea(legacyFlag), 1L));
        assertTrue(scriptSaved.await(3, TimeUnit.SECONDS), "剧本应先保存");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!emitters.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(emitters.isEmpty(), "流应在剧本保存后结束");
        verify(streaming, times(1)).chat(anyList(), any(StreamingChatResponseHandler.class));
        verify(chat, never()).buildChatModel(any());
        verify(projects).insert(argThat((ShortDramaProject project) -> "古人乘竹布风筝翱翔".equals(project.getOriginalIdea())));
        verify(scripts).insert(any(ShortDramaScript.class));
        verifyNoInteractions(characters, locations, storyboards, visualAssets);
    }

    private record RunningStream(SseEmitter emitter, StreamingChatResponseHandler handler) {}

    @Test
    void savingProjectMetadataPreservesTheOriginalUserInput() {
        var previous = new ShortDramaProject();
        previous.setId(7L);
        previous.setUserId(1L);
        previous.setOriginalIdea("最初输入\n保留手工水墨，15秒。");
        when(projects.selectById(7L)).thenReturn(previous);
        var update = new ShortDramaProjectBo();
        update.setId(7L);
        update.setProjectName("修改后的标题");

        assertEquals(7L, service.saveProject(update, 1L));
        verify(projects).updateById(argThat((ShortDramaProject project) ->
            previous.getOriginalIdea().equals(project.getOriginalIdea())
                && "修改后的标题".equals(project.getProjectName())));
        verifyNoInteractions(characters, locations, storyboards, visualAssets);
    }

    private RunningStream startControlledStream() throws Exception {
        var handler = new AtomicReference<StreamingChatResponseHandler>();
        var ready = new CountDownLatch(1);
        doAnswer(call -> {
            handler.set(call.getArgument(1));
            ready.countDown();
            return null;
        }).when(streaming).chat(anyList(), any(StreamingChatResponseHandler.class));
        var emitter = service.createFromIdeaStream(idea(true), 1L);
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        return new RunningStream(emitter, handler.get());
    }

    private static String emitted(SseEmitter emitter) {
        // SseEmitter guards the buffered event batch with its write lock, including terminal sends.
        var lock = (java.util.concurrent.locks.Lock) ReflectionTestUtils.getField(emitter, "writeLock");
        assertNotNull(lock);
        lock.lock();
        try {
            var attempts = (Set<?>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
            assertNotNull(attempts);
            return attempts.stream().map(value -> String.valueOf(((ResponseBodyEmitter.DataWithMediaType) value).getData()))
                .collect(java.util.stream.Collectors.joining());
        } finally { lock.unlock(); }
    }

    @Test
    void thinkingIsOnlyCountedAndNeverExposedOrSaved() throws Exception {
        var running = startControlledStream();
        running.handler().onPartialThinking(new PartialThinking("先建立人物冲突。\n"));
        String early = emitted(running.emitter());
        assertFalse(early.contains("先建立人物冲突。"));
        assertTrue(early.contains("event:progress"));
        assertFalse(early.contains("\"phase\":\"script\""));
        verify(scripts, never()).insert(any(ShortDramaScript.class));
        verify(chat).buildStreamingChatModel(any(), argThat(request -> Boolean.TRUE.equals(request.getEnableThinking())));

        int split = RESPONSE.indexOf("===SCRIPT===") + 5;
        running.handler().onPartialResponse(RESPONSE.substring(0, split));
        running.handler().onPartialResponse(RESPONSE.substring(split));
        assertTrue(emitted(running.emitter()).contains("竹骨布面风筝"), "正文应在完成前推送");
        running.handler().onCompleteResponse(null);
        assertTrue(scriptSaved.await(3, TimeUnit.SECONDS));
        verify(scripts).insert(argThat((ShortDramaScript script) -> !script.getScriptText().contains("先建立人物冲突")));
        verifyNoInteractions(characters, locations, storyboards, visualAssets);
    }

    @Test
    void completeOnlyProviderStillEmitsAndSavesScript() throws Exception {
        var running = startControlledStream();
        running.handler().onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(RESPONSE)).build());
        assertTrue(scriptSaved.await(3, TimeUnit.SECONDS));
        assertTrue(emitted(running.emitter()).contains("竹骨布面风筝"));
        verify(scripts).insert(argThat((ShortDramaScript script) -> script.getScriptText().contains("竹骨布面风筝")));
    }

    @Test
    void textWithoutDelimiterIsBackfilledAndNormalStreamIsNotDuplicated() throws Exception {
        var running = startControlledStream();
        String withoutDelimiter = RESPONSE.replace("===SCRIPT===", "");
        running.handler().onPartialResponse(withoutDelimiter);
        running.handler().onCompleteResponse(null);
        assertTrue(scriptSaved.await(3, TimeUnit.SECONDS));
        String events = emitted(running.emitter());
        assertTrue(events.contains("竹骨布面风筝"));
        assertEquals(1, events.split("竹骨布面风筝", -1).length - 1);
    }

    @Test
    void completeResponseBackfillsOnlyTheMissingSuffix() throws Exception {
        var running = startControlledStream();
        int split = RESPONSE.indexOf("竹骨布面风筝");
        running.handler().onPartialResponse(RESPONSE.substring(0, split));
        running.handler().onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(RESPONSE)).build());
        assertTrue(scriptSaved.await(3, TimeUnit.SECONDS));
        String events = emitted(running.emitter());
        assertEquals(1, events.split("云间行", -1).length - 1, "已经推送的剧本标题不能重复");
        assertEquals(1, events.split("竹骨布面风筝", -1).length - 1);
    }
}
