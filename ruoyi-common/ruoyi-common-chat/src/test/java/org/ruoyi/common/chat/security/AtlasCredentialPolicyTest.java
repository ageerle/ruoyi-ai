package org.ruoyi.common.chat.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasCredentialPolicyTest {
    private static final String MODEL = "bytedance/seed-audio-1.0";
    private static final String HOST = "https://api.atlascloud.ai/v1";
    private static final String REF = "env:ATLAS_API_KEY";

    @Test
    void acceptsOfficialEndpointFormsAtSaveAndUse() {
        for (String suffix : new String[]{"", "/", "/v1", "/v1/", "/api/v1", "/api/v1/"}) {
            String host = "https://api.atlascloud.ai" + suffix;
            assertDoesNotThrow(() -> ChatModelCredentialPolicy.requirePersistableConfiguration("atlas", MODEL, host, REF));
            assertEquals("test-secret", ChatModelCredentialPolicy.resolveApiKeyForUse("atlas", "atlas", MODEL, host, REF,
                name -> "ATLAS_API_KEY".equals(name) ? "test-secret" : null));
        }
    }

    @Test
    void rejectsUntrustedEndpointsBeforeReadingEnvironment() {
        for (String host : new String[]{"http://api.atlascloud.ai/v1", "https://evil.example/v1",
            "https://api.atlascloud.ai.evil.example/v1", "https://api.atlascloud.ai:8443/v1",
            "https://user@api.atlascloud.ai/v1", "https://api.atlascloud.ai/v1?x=1", "https://api.atlascloud.ai/other"}) {
            assertThrows(IllegalArgumentException.class,
                () -> ChatModelCredentialPolicy.requirePersistableConfiguration("atlas", MODEL, host, REF));
            assertThrows(IllegalArgumentException.class,
                () -> ChatModelCredentialPolicy.resolveApiKeyForUse("atlas", "atlas", MODEL, host, REF,
                    name -> { fail("Must validate endpoint before resolving secret"); return null; }));
        }
    }

    @Test
    void rejectsWrongProviderReferenceAndMissingSecret() {
        assertThrows(IllegalArgumentException.class, () -> ChatModelCredentialPolicy.resolveApiKeyForUse(
            "openai", "atlas", MODEL, HOST, REF, name -> "secret"));
        for (String key : new String[]{"env:PPIO_API_KEY", "plaintext", "", null}) {
            assertThrows(IllegalArgumentException.class,
                () -> ChatModelCredentialPolicy.requirePersistableConfiguration("atlas", MODEL, HOST, key));
        }
        assertThrows(IllegalStateException.class, () -> ChatModelCredentialPolicy.resolveApiKeyForUse(
            "atlas", "atlas", MODEL, HOST, REF, name -> null));
        assertThrows(IllegalArgumentException.class,
            () -> ChatModelCredentialPolicy.requirePersistableConfiguration("atlas", "bad model", HOST, REF));
    }
}
