package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.image.IImageGenerationService;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.ruoyi.domain.entity.shortdrama.ShortDramaLocation;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterAppearanceVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaLocationVo;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaImageRevisionTest {
    @TempDir Path directory;
    private ShortDramaServiceImpl service;
    private ShortDramaImagePromptEvidence evidence;
    private ShortDramaCharacterAppearance appearance;
    private ShortDramaCharacterAppearanceMapper appearances;
    private IImageGenerationService images;
    private AtlasPredictionService predictions;
    private ChatModelVo model;
    private ShortDramaScript script;
    private ShortDramaLocation location;

    @BeforeAll static void jsonAndConverter() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("objectMapper", new ObjectMapper().findAndRegisterModules());
        var converter = mock(Converter.class);
        // MapstructUtils caches the first converter across the whole test JVM.
        when(converter.convert(any(Object.class), any(Class.class))).thenAnswer(call -> {
            Object target = ((Class<?>) call.getArgument(1)).getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(call.getArgument(0), target);
            return target;
        });
        when(converter.convert(any(ShortDramaCharacterAppearance.class), eq(ShortDramaCharacterAppearanceVo.class)))
            .thenAnswer(invocation -> {
                var vo = new ShortDramaCharacterAppearanceVo(); BeanUtils.copyProperties(invocation.getArgument(0), vo); return vo;
            });
        when(converter.convert(any(ShortDramaCharacter.class), eq(ShortDramaCharacterVo.class)))
            .thenAnswer(invocation -> {
                var vo = new ShortDramaCharacterVo(); BeanUtils.copyProperties(invocation.getArgument(0), vo); return vo;
            });
        factory.registerSingleton("converter", converter);
        when(converter.convert(any(ShortDramaLocation.class), eq(ShortDramaLocationVo.class)))
            .thenAnswer(invocation -> {
                var vo = new ShortDramaLocationVo(); BeanUtils.copyProperties(invocation.getArgument(0), vo); return vo;
            });
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
    }

    @BeforeEach void fixture() {
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        evidence = new ShortDramaImagePromptEvidence(directory);
        appearance = new ShortDramaCharacterAppearance();
        appearance.setId(20L); appearance.setCharacterId(30L);
        appearance.setDescription("成年男子，写实肤质细微毛孔，交领服装");
        appearance.setReferenceImageUrl("https://example.com/approved.png");
        appearance.setSelectedImageIndex(0);
        appearance.setImageUrls("[\"https://example.com/approved.png\"]");
        appearance.setImageDescriptions("[\"approved prompt\"]");
        var character = new ShortDramaCharacter(); character.setId(30L); character.setProjectId(10L);
        character.setVisualDescription(appearance.getDescription());
        var project = new ShortDramaProject(); project.setId(10L); project.setUserId(1L); project.setArtStyle("chinese-3d");
        appearances = mock(ShortDramaCharacterAppearanceMapper.class);
        when(appearances.selectById(20L)).thenReturn(appearance);
        var characters = mock(ShortDramaCharacterMapper.class); when(characters.selectById(30L)).thenReturn(character);
        var projects = mock(ShortDramaProjectMapper.class); when(projects.selectById(10L)).thenReturn(project);
        script = new ShortDramaScript(); script.setId(40L); script.setProjectId(10L);
        script.setWorldbuilding("故事发生在前现代农业社会，资源紧张的建设初期；未来计划扩建港口和引入工业设备，尚未实现。");
        var scripts = mock(ShortDramaScriptMapper.class); when(scripts.selectOne(any())).thenReturn(script);
        location = new ShortDramaLocation(); location.setId(50L); location.setProjectId(10L);
        location.setDescriptions("[\"小型试育苗棚，木架覆草席，陶制育苗盆，土畦上只有少量两片叶幼苗\"]");
        var locations = mock(ShortDramaLocationMapper.class); when(locations.selectById(50L)).thenReturn(location);
        var models = mock(IChatModelService.class);
        model = new ChatModelVo(); model.setModelName("edit-model"); model.setProviderCode("atlas");
        when(models.selectModelByName("edit-model")).thenReturn(model);
        images = mock(IImageGenerationService.class);
        when(images.startImageGeneration(any())).thenReturn(MediaGenerationResponse.builder().id("prediction-1").status("processing").build());
        when(images.generateImage(any())).thenReturn("");
        var factory = mock(ImageServiceFactory.class); when(factory.getOriginalService("atlas")).thenReturn(images);
        predictions = mock(AtlasPredictionService.class);
        when(predictions.retrieve(model, "prediction-1")).thenReturn(MediaGenerationResponse.builder()
            .id("prediction-1").status("completed").url("https://example.com/candidate.png").build());
        ReflectionTestUtils.setField(service, "imagePromptEvidence", evidence);
        ReflectionTestUtils.setField(service, "characterAppearanceMapper", appearances);
        ReflectionTestUtils.setField(service, "characterMapper", characters);
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        ReflectionTestUtils.setField(service, "scriptMapper", scripts);
        ReflectionTestUtils.setField(service, "locationMapper", locations);
        ReflectionTestUtils.setField(service, "chatModelService", models);
        ReflectionTestUtils.setField(service, "imageServiceFactory", factory);
        ReflectionTestUtils.setField(service, "atlasPredictionService", predictions);
    }

    @Test void actualStartPassesExplicitEditToProviderAndConfirmationRetainsApprovedSelection() {
        service.startImageGeneration("appearance", 20L, "edit-model", "https://example.com/rejected.png?token=secret",
            "identity_revision", "保留脸型与年龄，动画材质，交领改为闭合圆领", 1L);
        var input = ArgumentCaptor.forClass(ImageContext.class);
        verify(images).startImageGeneration(input.capture());
        String finalPrompt = input.getValue().getPrompt();
        assertTrue(finalPrompt.contains("左右领缘不交叉"));
        assertTrue(finalPrompt.contains("参考图只绑定脸部身份"));
        assertTrue(finalPrompt.contains(script.getWorldbuilding()));
        assertTrue(finalPrompt.contains("风格化CG雕塑造型"));
        var receipt = evidence.forPrediction("appearance", 20L, 1L, "edit-model", "prediction-1");
        assertTrue(receipt.revision());
        assertEquals(ShortDramaImagePromptEvidence.sha256(finalPrompt), receipt.promptSha256());
        assertEquals(finalPrompt, receipt.finalPrompt());
        appearance.setDescription("请求完成前被编辑的另一版描述");
        service.confirmAppearanceImage(20L, "prediction-1", "edit-model", 1L);
        assertEquals("https://example.com/approved.png", appearance.getReferenceImageUrl());
        assertEquals(0, appearance.getSelectedImageIndex());
        assertTrue(appearance.getImageUrls().contains("candidate.png"));
        assertTrue(appearance.getImageDescriptions().contains("本轮明确修订要求"));
        assertFalse(appearance.getImageDescriptions().contains("请求完成前被编辑"));
        service.selectAppearanceImage(20L, 1, 1L);
        assertEquals("https://example.com/candidate.png", appearance.getReferenceImageUrl());
        assertEquals(1, appearance.getSelectedImageIndex());
    }

    @Test void repeatedConfirmDoesNotDuplicateTheCandidateOrChangeSelection() {
        service.startImageGeneration("appearance", 20L, "edit-model", "https://example.com/identity.png", "identity_revision", "修正布料材质", 1L);
        service.confirmAppearanceImage(20L, "prediction-1", "edit-model", 1L);
        String saved = appearance.getImageUrls();
        service.confirmAppearanceImage(20L, "prediction-1", "edit-model", 1L);
        assertEquals(saved, appearance.getImageUrls());
        assertEquals(0, appearance.getSelectedImageIndex());
        verify(appearances, times(1)).updateById(appearance);
    }

    @Test void omittedIntentKeepsExistingNormalConfirmationBehavior() {
        service.startImageGeneration("appearance", 20L, "edit-model", null, 1L);
        assertFalse(evidence.forPrediction("appearance", 20L, 1L, "edit-model", "prediction-1").revision());
        service.confirmAppearanceImage(20L, "prediction-1", "edit-model", 1L);
        assertEquals("https://example.com/candidate.png", appearance.getReferenceImageUrl());
        assertEquals(1, appearance.getSelectedImageIndex());
    }

    @Test void everySynchronousCharacterImagePathReceivesTheProjectsWorldAndVisibleCgStyle() {
        service.generateCharacterImage(30L, "edit-model", null, 1L);
        service.generateAppearanceImage(20L, "edit-model", null, 1L);
        service.regenerateAppearanceImage(20L, "edit-model", null, 1L);
        var inputs = ArgumentCaptor.forClass(ImageContext.class);
        verify(images, times(3)).generateImage(inputs.capture());
        for (ImageContext input : inputs.getAllValues()) {
            assertTrue(input.getPrompt().contains(script.getWorldbuilding()));
            assertTrue(input.getPrompt().contains("风格化CG雕塑造型"));
            assertTrue(input.getPrompt().contains("西式翻领衬衫"));
            assertTrue(input.getPrompt().contains("默认已批准参考的身份与衣物仍保持"));
        }
    }

    @Test void synchronousAndAsyncLocationsReceiveCurrentScaleMaterialsAndGrowthState() {
        service.generateLocationImage(50L, "edit-model", null, 1L);
        service.regenerateLocationImage(50L, "edit-model", null, 1L);
        var inputs = ArgumentCaptor.forClass(ImageContext.class);
        verify(images, times(2)).generateImage(inputs.capture());
        for (ImageContext input : inputs.getAllValues()) {
            assertLocationScope(input.getPrompt());
        }
        service.startImageGeneration("location", 50L, "edit-model", null, 1L);
        var asyncInput = ArgumentCaptor.forClass(ImageContext.class);
        verify(images).startImageGeneration(asyncInput.capture());
        String submittedPrompt = asyncInput.getValue().getPrompt();
        assertLocationScope(submittedPrompt);
        script.setWorldbuilding("请求完成前的新世界观");
        location.setDescriptions("[\"请求完成前的另一版场景\"]");
        service.confirmLocationImage(50L, "prediction-1", "edit-model", 1L);
        assertTrue(location.getImageDescriptions().contains("木架覆草席"));
        assertFalse(location.getImageDescriptions().contains("请求完成前"));
        assertEquals(ShortDramaImagePromptEvidence.sha256(submittedPrompt),
            evidence.forPrediction("location", 50L, 1L, "edit-model", "prediction-1").promptSha256());
    }

    private void assertLocationScope(String prompt) {
        assertTrue(prompt.contains(script.getWorldbuilding()));
        assertTrue(prompt.contains("小型试育苗棚，木架覆草席，陶制育苗盆，土畦上只有少量两片叶幼苗"));
        assertTrue(prompt.contains("按上述场景实际规模建立完整空间构图"));
        assertTrue(prompt.contains("未来装备、其他年代人物与计划成果不自动进入本图"));
        assertTrue(prompt.contains("塑料育苗盘"));
        assertTrue(prompt.contains("不能画成成熟稻穗"));
        assertTrue(prompt.contains("牌匾、城名、招牌与可读文字不凭空创造"));
        assertFalse(prompt.contains("宽广空间全景"));
    }

    @Test void invalidEditInputsFailBeforeProviderSubmission() {
        assertThrows(IllegalArgumentException.class, () -> service.startImageGeneration("appearance", 20L, "edit-model", null,
            "identity_revision", "换材质", 1L));
        assertThrows(IllegalArgumentException.class, () -> service.startImageGeneration("location", 20L, "edit-model", "https://example.com/ref.png",
            "identity_revision", "换材质", 1L));
        assertThrows(IllegalArgumentException.class, () -> service.startImageGeneration("appearance", 20L, "edit-model", "https://example.com/ref.png",
            "identity_revision", "", 1L));
        verifyNoInteractions(images);
    }
    @Test void locationAsyncEditUsesExactRequestEvidenceAndKeepsApprovedSelectionUntilChosen() {
        approvedLocation(); String frozen = location.getDescriptions(); String name = location.getName();
        String requirements = "清晨冷蓝光，清空错误武器；旧木架与入口几何不变";
        service.startImageGeneration("location", 50L, "edit-model", "https://example.com/sunset.png", null, requirements, 1L);
        var input = ArgumentCaptor.forClass(ImageContext.class); verify(images).startImageGeneration(input.capture());
        String submitted = input.getValue().getPrompt();
        assertTrue(submitted.contains(requirements)); assertTrue(submitted.contains("当前地点的局部修订候选"));
        assertEquals("https://example.com/sunset.png", input.getValue().getImage());
        var receipt = evidence.forPrediction("location", 50L, 1L, "edit-model", "prediction-1");
        assertTrue(receipt.revision()); assertEquals("location_revision", receipt.referencePurpose());
        assertEquals(requirements, receipt.revisionRequirements()); assertEquals(submitted, receipt.finalPrompt());
        assertEquals(ShortDramaImagePromptEvidence.sha256(submitted), receipt.promptSha256());
        script.setWorldbuilding("异步完成前的另一份世界观");
        service.confirmLocationImage(50L, "prediction-1", "edit-model", 1L);
        assertEquals(frozen, location.getDescriptions()); assertEquals(name, location.getName());
        assertEquals("https://example.com/location-approved.png", location.getReferenceImageUrl()); assertEquals(0, location.getSelectedImageIndex());
        assertTrue(location.getImageUrls().contains("candidate.png")); assertTrue(location.getImageDescriptions().contains("清晨冷蓝光"));
        assertFalse(location.getImageDescriptions().contains("异步完成前"));
        String saved = location.getImageUrls(); service.confirmLocationImage(50L, "prediction-1", "edit-model", 1L); assertEquals(saved, location.getImageUrls());
        service.selectLocationImage(50L, 1, 1L); assertEquals("https://example.com/candidate.png", location.getReferenceImageUrl());
    }
    @Test void bothSynchronousLocationRoutesSupportReferenceEditOrT2IAndSaveActualPromptEvidence() throws Exception {
        approvedLocation(); String frozen = location.getDescriptions();
        when(images.generateImage(any())).thenReturn("https://example.com/location-edit.png", "https://example.com/location-t2i.png");
        service.generateLocationImage(50L, "edit-model", "https://example.com/sunset.png", "改为秋日午后中性光；几何不变", 1L);
        service.regenerateLocationImage(50L, "edit-model", null, "清空错误武器，改为冷晨光；几何不变", 1L);
        var inputs = ArgumentCaptor.forClass(ImageContext.class); verify(images, times(2)).generateImage(inputs.capture());
        assertEquals("https://example.com/sunset.png", inputs.getAllValues().get(0).getImage());
        assertNull(inputs.getAllValues().get(1).getImage());
        assertTrue(inputs.getAllValues().get(0).getPrompt().contains("秋日午后中性光")); assertTrue(inputs.getAllValues().get(1).getPrompt().contains("清空错误武器"));
        assertEquals(frozen, location.getDescriptions()); assertEquals("https://example.com/location-approved.png", location.getReferenceImageUrl());
        assertEquals(0, location.getSelectedImageIndex()); assertTrue(location.getImageUrls().contains("location-edit.png")); assertTrue(location.getImageUrls().contains("location-t2i.png"));
        try (var files = Files.list(directory.resolve("attempts"))) {
            var saved = files.map(file -> {
                try { return new ObjectMapper().readValue(Files.readString(file), ShortDramaImagePromptEvidence.Evidence.class); }
                catch (Exception e) { throw new RuntimeException(e); }
            }).toList();
            assertEquals(2, saved.size());
            for (var input : inputs.getAllValues()) {
                var item = saved.stream().filter(e -> e.promptSha256().equals(ShortDramaImagePromptEvidence.sha256(input.getPrompt()))).findFirst().orElseThrow();
                assertEquals(input.getPrompt(), item.finalPrompt()); assertTrue(item.revision()); assertEquals("completed", item.providerStatus());
            }
        }
    }
    @Test void locationT2IRevisionNeedsNoReferenceAndInvalidLengthOwnerQuotaOrEvidenceCannotSubmit() throws Exception {
        service.startImageGeneration("location", 50L, "edit-model", null, "location_revision", "秋日午后光，保持入口几何", 1L);
        var input = ArgumentCaptor.forClass(ImageContext.class); verify(images).startImageGeneration(input.capture()); assertNull(input.getValue().getImage());
        clearInvocations(images);
        assertThrows(IllegalArgumentException.class, () -> service.generateLocationImage(50L, "edit-model", null, "光".repeat(4001), 1L));
        assertThrows(IllegalArgumentException.class, () -> service.startImageGeneration("location", 50L, "edit-model", null, null, "光".repeat(4001), 1L));
        assertThrows(IllegalArgumentException.class, () -> service.regenerateLocationImage(50L, "edit-model", null, "改光色", 2L));
        verifyNoInteractions(images);
        var full = java.util.stream.IntStream.range(0, org.ruoyi.constant.ShortDramaImageConstants.MAX_IMAGE_VARIANTS).mapToObj(i -> "https://example.com/" + i + ".png").toList();
        location.setImageUrls(new ObjectMapper().writeValueAsString(full));
        assertThrows(IllegalArgumentException.class, () -> service.generateLocationImage(50L, "edit-model", null, "改光色", 1L));
        assertThrows(IllegalArgumentException.class, () -> service.startImageGeneration("location", 50L, "edit-model", null, null, "改光色", 1L));
        location.setImageUrls("[]"); Path invalid = directory.resolve("is-a-file"); Files.writeString(invalid, "fixture");
        ReflectionTestUtils.setField(service, "imagePromptEvidence", new ShortDramaImagePromptEvidence(invalid));
        assertThrows(IllegalStateException.class, () -> service.generateLocationImage(50L, "edit-model", null, "改光色", 1L));
        assertThrows(IllegalStateException.class, () -> service.startImageGeneration("location", 50L, "edit-model", null, null, "改光色", 1L));
        verifyNoInteractions(images);
    }
    private void approvedLocation() {
        location.setName("试育苗棚_秋日傍晚"); location.setReferenceImageUrl("https://example.com/location-approved.png");
        location.setImageUrls("[\"https://example.com/location-approved.png\"]"); location.setImageDescriptions("[\"approved frozen location\"]"); location.setSelectedImageIndex(0);
    }

    @Test void evidenceRedactsSecretsRetainsActualInputHashAndSurvivesRestart() throws Exception {
        String prompt = "布料；api_key=sample-secret；Authorization: Bearer credential-value https://example.com/ref?token=signed-secret";
        var prepared = evidence.prepare("appearance", 20L, 10L, 1L, "edit-model", "identity_revision", "修订布料", prompt,
            "https://example.com/ref?token=signed-secret");
        evidence.submitted(prepared, "../provider/prediction", "processing");
        var restarted = new ShortDramaImagePromptEvidence(directory);
        var loaded = restarted.forPrediction("appearance", 20L, 1L, "edit-model", "../provider/prediction");
        assertEquals(ShortDramaImagePromptEvidence.sha256(prompt), loaded.promptSha256());
        assertFalse(loaded.finalPrompt().contains("sample-secret"));
        assertFalse(loaded.finalPrompt().contains("credential-value"));
        assertFalse(loaded.finalPrompt().contains("signed-secret"));
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String saved = Files.readString(file);
                assertFalse(saved.contains("signed-secret"));
                assertTrue(file.toAbsolutePath().startsWith(directory.toAbsolutePath()));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> restarted.forPrediction("appearance", 99L, 1L, "edit-model", "../provider/prediction"));
    }

    @Test void evidenceFailureBeforeSubmissionBlocksPaidWork() throws Exception {
        Path invalid = directory.resolve("is-a-file"); Files.writeString(invalid, "fixture");
        ReflectionTestUtils.setField(service, "imagePromptEvidence", new ShortDramaImagePromptEvidence(invalid));
        assertThrows(IllegalStateException.class, () -> service.startImageGeneration("appearance", 20L, "edit-model", null, 1L));
        verifyNoInteractions(images);
        verify(appearances, never()).updateById(any(ShortDramaCharacterAppearance.class));
    }
}
