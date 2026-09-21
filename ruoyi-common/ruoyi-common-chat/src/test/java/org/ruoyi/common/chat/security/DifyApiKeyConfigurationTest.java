package org.ruoyi.common.chat.security;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.bo.chat.ModelBatchKeyBo;
import org.ruoyi.common.core.validate.AddGroup;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class DifyApiKeyConfigurationTest {
    @Test
    void acceptsAnAppKeyForCloudAndSelfHostedDify() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String host : new String[]{"https://api.dify.ai/v1", "http://localhost:8080/v1"}) {
                var model = new ChatModelBo();
                model.setCategory("chat");
                model.setProviderCode("dify");
                model.setModelName("dify-chat");
                model.setApiHost(host);
                model.setApiKey("app-test-key");
                assertTrue(factory.getValidator().validate(model, AddGroup.class).isEmpty());
            }
        }
    }

    @Test
    void batchUpdatesStillRequireAProviderAndANonblankKey() {
        var batch = new ModelBatchKeyBo();
        batch.setProviderCode("dify");
        batch.setApiKey("");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(batch).isEmpty());
            batch.setApiKey("app-test-key");
            assertTrue(factory.getValidator().validate(batch).isEmpty());
        }
    }
}
