package org.ruoyi.service.image.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class AtlasImageGenerationServiceImplTest {
    private static final String SEEDREAM47 = "bytedance/seedream-v4.7/text-to-image";
    @Test void synchronousSeedream47UsesOneAsyncSubmissionThenOriginalPredictionWithoutSyncFlag() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var submitted = new java.util.concurrent.atomic.AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/model/generateImage", exchange -> {
            requests.incrementAndGet(); submitted.set(new com.fasterxml.jackson.databind.ObjectMapper().readTree(exchange.getRequestBody()));
            byte[] bytes = "{\"code\":200,\"data\":{\"id\":\"task-47\",\"status\":\"processing\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start();
        try {
            var predictions = org.mockito.Mockito.mock(org.ruoyi.service.media.AtlasPredictionService.class);
            var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo(); model.setModelName(SEEDREAM47);
            model.setApiHost("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1"); model.setApiKey("local-test-only");
            org.mockito.Mockito.when(predictions.retrieve(model, "task-47")).thenReturn(org.ruoyi.common.chat.entity.media.MediaGenerationResponse.builder()
                .id("task-47").status("completed").url("https://example.com/candidate.png").build());
            var service = new AtlasImageGenerationServiceImpl(predictions);
            assertEquals("https://example.com/candidate.png", service.generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(model).prompt("单幅人物候选").size("16:9").seed(123).build()));
            assertEquals(1, requests.get()); assertFalse(submitted.get().has("enable_sync_mode")); assertFalse(submitted.get().has("seed"));
            org.mockito.Mockito.verify(predictions).retrieve(model, "task-47");
        } finally { server.stop(0); }
    }
    @Test void seedream47PredictionFailureIncludesOriginalIdWithoutResubmitting() {
        var predictions = org.mockito.Mockito.mock(org.ruoyi.service.media.AtlasPredictionService.class);
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo(); model.setModelName(SEEDREAM47);
        org.mockito.Mockito.when(predictions.retrieve(model, "failed-task")).thenReturn(org.ruoyi.common.chat.entity.media.MediaGenerationResponse.builder().status("failed").build());
        var service = new AtlasImageGenerationServiceImpl(predictions);
        assertTrue(assertThrows(IllegalStateException.class, () -> service.awaitSeedream47Image(model, "failed-task")).getMessage().contains("failed-task"));
        org.mockito.Mockito.verify(predictions).retrieve(model, "failed-task");
    }

    @Test void seedream47UsesDocumentedSingleImagePayloadAndTwoKPresets() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo(); model.setModelName(SEEDREAM47);
        var payload = AtlasImageGenerationServiceImpl.buildPayload(model, "明代青年县令，国风三维动画", "16:9", 123, null);
        assertEquals(SEEDREAM47, payload.path("model").asText());
        assertEquals("2848*1600", payload.path("size").asText());
        assertEquals("standard", payload.path("prompt_expansion_mode").asText());
        assertFalse(payload.path("enable_base64_output").asBoolean());
        for (String unsupported : java.util.List.of("seed", "aspect_ratio", "num_images", "n", "images", "image_url")) {
            assertFalse(payload.has(unsupported), unsupported);
        }
        assertEquals("1600*2848", AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, "9:16"));
        assertEquals("2304*1728", AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, "4:3"));
        assertEquals("1728*2304", AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, "3:4"));
        assertEquals("1280*720", AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, "1280x720"));
        assertEquals("1536*864", AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, "1536*864"));
    }

    @Test void seedream47RejectsInvalidPixelBudgetAndExtremeShapeBeforeSubmission() {
        for (String size : java.util.List.of("512*512", "0*2048", "4097*4097", "16000*64", "garbage", "5:4", "-2048*-2048", "2048**2048")) {
            assertThrows(IllegalArgumentException.class, () -> AtlasImageGenerationServiceImpl.resolveSize(SEEDREAM47, size), size);
        }
    }

    @Test void seedream47NeverSilentlyConvertsToAnEditModelOrDiscardsReferenceImages() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo(); model.setModelName(SEEDREAM47);
        var single = assertThrows(IllegalArgumentException.class,
            () -> AtlasImageGenerationServiceImpl.buildPayload(model, "服饰候选", "16:9", null, "https://example.com/identity.png"));
        assertTrue(single.getMessage().contains("不会自动切换"));
        var context = org.ruoyi.common.chat.entity.image.ImageContext.builder().chatModelVo(model)
            .referenceImages(java.util.List.of("https://example.com/identity.png")).build();
        assertThrows(IllegalArgumentException.class, () -> AtlasImageGenerationServiceImpl.applyReferenceImages(
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(), context));
        assertEquals(SEEDREAM47, model.getModelName());
    }
    @Test void sendsAllIdentityAndLocationReferencesInOrder() {
        var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();model.setModelName("openai/gpt-image-2/edit");
        var context=org.ruoyi.common.chat.entity.image.ImageContext.builder().chatModelVo(model).referenceImages(java.util.List.of("person1","person2","location","prop")).build();
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        AtlasImageGenerationServiceImpl.applyReferenceImages(payload,context);
        assertEquals(4,payload.path("images").size());assertEquals("location",payload.path("images").get(2).asText());
    }

    @Test void supportsGptImage25NativeWidescreenAndSixteenReferences() {
        var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();model.setModelName("openai/gpt-image-2.5-flare/edit");
        var refs=java.util.stream.IntStream.range(0,16).mapToObj(i->"reference-"+i).toList();
        var context=org.ruoyi.common.chat.entity.image.ImageContext.builder().chatModelVo(model).referenceImages(refs).build();
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        AtlasImageGenerationServiceImpl.applyReferenceImages(payload,context);
        assertEquals(16,payload.path("images").size());
        assertEquals("1536x864",AtlasImageGenerationServiceImpl.resolveSize(model.getModelName(),"16:9"));
    }

    @Test
    void shouldUseGptImageSupportedLandscapeSize() {
        String model = "openai/gpt-image-2/text-to-image";

        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "3:2"));
        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "1152*768"));
        assertEquals("1536x864", AtlasImageGenerationServiceImpl.resolveSize(model, "16:9"));
    }

    @Test
    void shouldUseGptImageSupportedPortraitAndSquareSizes() {
        String model = "openai/gpt-image-2/edit";

        assertEquals("864x1536", AtlasImageGenerationServiceImpl.resolveSize(model, "9:16"));
        assertEquals("1024x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "1:1"));
    }

    @Test
    void shouldKeepLegacyAtlasSizeFormatForOtherModels() {
        String model = "microsoft/mai-image-2.5-flash/text-to-image";

        assertEquals("1152*768", AtlasImageGenerationServiceImpl.resolveSize(model, "3:2"));
        assertEquals("1360*768", AtlasImageGenerationServiceImpl.resolveSize(model, "16:9"));
    }

    @Test
    void shouldDetectGptImageModels() {
        assertTrue(AtlasImageGenerationServiceImpl.isGptImageModel("openai/gpt-image-2/text-to-image"));
        assertTrue(AtlasImageGenerationServiceImpl.isGptImageModel("openai/gpt-image-2/edit"));
        assertFalse(AtlasImageGenerationServiceImpl.isGptImageModel("microsoft/mai-image-2.5/edit"));
    }
}
