package org.ruoyi.common.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.bo.chat.ModelBatchKeyBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelDetailVo;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ModelApiKeyValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"qianwen", "zhipu", "openai", "deepseek", "ppio", "atlas",
        "minimax", "dify", "coze", "xiaomi", "custom_api", "custom_anthropic"})
    void acceptsAdminConfiguredKeysForEveryProvider(String provider) {
        var model = new ChatModelBo();
        model.setId(1L);
        model.setCategory("chat");
        model.setProviderCode(provider);
        model.setModelName("configured-model");
        model.setApiHost("http://localhost:11434/v1");
        model.setApiKey("test-key-" + provider);
        var batch = new ModelBatchKeyBo();
        batch.setProviderCode(provider);
        batch.setApiKey(model.getApiKey());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(model, AddGroup.class, EditGroup.class).isEmpty());
            assertTrue(validator.validate(batch).isEmpty());
        }
    }

    @Test
    void acceptsKeylessLocalModelsAndDoesNotRequireHiddenHostField() {
        var model = new ChatModelBo();
        model.setCategory("chat");
        model.setProviderCode("ollama");
        model.setModelName("local-model");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(model, AddGroup.class).isEmpty());
        }
    }

    @Test
    void keyRemainsWritableButIsNotSerializedOrIncludedInToString() throws Exception {
        var mapper = new ObjectMapper();
        var model = mapper.readValue("{\"providerCode\":\"qianwen\",\"apiKey\":\"test-secret\"}", ChatModelBo.class);
        assertEquals("test-secret", model.getApiKey());
        var view = new ChatModelVo();
        view.setApiKey(model.getApiKey());
        assertFalse(mapper.writeValueAsString(model).contains("test-secret"));
        assertFalse(mapper.writeValueAsString(view).contains("apiKey"));
        assertFalse(model.toString().contains("test-secret"));
        assertFalse(view.toString().contains("test-secret"));
    }

    @Test
    void adminDetailReturnsExactKeyWithoutChangingListOrLogSerialization() throws Exception {
        var mapper = new ObjectMapper();
        var model = new ChatModelVo();
        model.setId(12L);
        model.setApiKey(" 任意/key: value ");
        var detail = ChatModelDetailVo.from(model);
        assertEquals(model.getApiKey(), mapper.readTree(mapper.writeValueAsString(detail)).get("apiKey").asText());
        assertEquals(model.getId(), detail.getId());
        assertFalse(mapper.writeValueAsString(model).contains("apiKey"));
        assertFalse(detail.toString().contains(model.getApiKey()));
        assertNull(ChatModelDetailVo.from(null));
    }

    @Test
    void customEndpointsNeedNoDeploymentVariablesAndSupportLocalServices() {
        assertEquals("http://localhost:11434/v1", CustomApiCredentialPolicy.normalizeBaseUrl(
            "custom_api", "http://localhost:11434/v1/chat/completions"));
        assertEquals("https://example.com/v1", CustomApiCredentialPolicy.normalizeBaseUrl(
            "custom_anthropic", "https://example.com/v1/messages"));
        assertThrows(IllegalArgumentException.class, () -> CustomApiCredentialPolicy.normalizeBaseUrl(
            "custom_anthropic", "https://example.com/v1/chat/completions"));
    }
}
