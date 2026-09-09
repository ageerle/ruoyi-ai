package org.ruoyi.common.chat.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Security policy binding model credential references to trusted provider endpoints. */
public final class ChatModelCredentialPolicy {

    public static final String HTTPS_API_HOST_REGEXP =
        "^https://(?![^/?#]*@)[^\\s/?#]+(?::[0-9]{1,5})?(?:/[^\\s?#]*)?$";

    public static final String DEEPSEEK_PROVIDER = "deepseek";
    public static final String DEEPSEEK_API_HOST = "https://api.deepseek.com";
    public static final String PPIO_PROVIDER = "ppio";
    public static final String PPIO_API_HOST = "https://api.ppio.com/openai/v1";

    private static final Set<String> PPIO_API_HOSTS = Set.of(
        PPIO_API_HOST, PPIO_API_HOST + "/",
        "https://api.ppio.com/openai", "https://api.ppio.com/openai/"
    );

    private static final Set<String> DEEPSEEK_MODELS = Set.of(
        "deepseek-v4-flash",
        "deepseek-v4-pro",
        "deepseek-v4-flash-vision-exp"
    );

    private ChatModelCredentialPolicy() {
    }

    /**
     * Resolves a provider credential only after the effective provider/model/endpoint/reference
     * tuple passes the same allowlist used at the persistence boundary.
     */
    public static String resolveApiKeyForUse(String consumingProviderCode,
                                             String providerCode,
                                             String modelName,
                                             String apiHost,
                                             String apiKey) {
        return resolveApiKeyForUse(consumingProviderCode, providerCode, modelName, apiHost,
            apiKey, System::getenv);
    }

    static String resolveApiKeyForUse(String consumingProviderCode,
                                      String providerCode,
                                      String modelName,
                                      String apiHost,
                                      String apiKey,
                                      Function<String, String> environment) {
        if (!Objects.equals(consumingProviderCode, providerCode)) {
            throw new IllegalArgumentException(
                "Credential consumer does not match the configured provider");
        }
        if (CustomApiCredentialPolicy.isCustomProvider(providerCode)) {
            CustomApiCredentialPolicy.requireConfiguration(providerCode, modelName, apiHost, apiKey, environment);
        } else {
            requireTrustedConfiguration(providerCode, modelName, apiHost, apiKey);
        }
        return ChatModelSecretReference.resolveAllowlistedReference(apiKey,
            Objects.requireNonNull(environment, "environment"));
    }

    /** Validates the effective row which will remain after an insert or partial update. */
    public static void requirePersistableConfiguration(String providerCode,
                                                       String modelName,
                                                       String apiHost,
                                                       String apiKey) {
        requireSecureApiHost(apiHost);
        if (CustomApiCredentialPolicy.isCustomProvider(providerCode)) {
            CustomApiCredentialPolicy.requireConfiguration(providerCode, modelName, apiHost, apiKey);
            return;
        }
        if (PPIO_PROVIDER.equals(providerCode)) {
            requirePpioConfiguration(providerCode, modelName, apiHost, apiKey);
            return;
        }
        if (apiKey != null) {
            ChatModelSecretReference.requirePersistableReference(apiKey);
            if (!isDeepSeekConfiguration(providerCode, modelName)) {
                throw new IllegalArgumentException(
                    "Model API key reference is not allowed for this provider");
            }
        }
        if (isDeepSeekConfiguration(providerCode, modelName)) {
            requireDeepSeekConfiguration(providerCode, modelName, apiHost, apiKey);
        }
    }

    /** Re-validates the configuration immediately before a DeepSeek provider client is built. */
    public static void requireDeepSeekConfiguration(String providerCode,
                                                    String modelName,
                                                    String apiHost,
                                                    String apiKey) {
        if (!DEEPSEEK_PROVIDER.equals(providerCode)) {
            throw new IllegalArgumentException("DeepSeek model provider is not trusted");
        }
        if (!DEEPSEEK_MODELS.contains(modelName)) {
            throw new IllegalArgumentException("DeepSeek model is not allowlisted");
        }
        requireSecureApiHost(apiHost);
        if (!DEEPSEEK_API_HOST.equals(apiHost)) {
            throw new IllegalArgumentException("DeepSeek API host is not allowlisted");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("DeepSeek API key reference is required");
        }
        requireProviderReference(providerCode, apiKey);
    }

    /** Binds each provider to its own environment variable, including batch credential updates. */
    public static void requireProviderReference(String providerCode, String apiKey) {
        if (CustomApiCredentialPolicy.isCustomProvider(providerCode)) {
            CustomApiCredentialPolicy.requireReference(providerCode, apiKey);
            return;
        }
        String expectedReference;
        if (DEEPSEEK_PROVIDER.equals(providerCode)) {
            expectedReference = "env:DEEPSEEK_API_KEY";
        } else if (PPIO_PROVIDER.equals(providerCode)) {
            expectedReference = "env:PPIO_API_KEY";
        } else {
            throw new IllegalArgumentException("Model credential provider is not allowlisted");
        }
        ChatModelSecretReference.requirePersistableReference(apiKey);
        if (!expectedReference.equals(apiKey)) {
            throw new IllegalArgumentException("Model API key reference does not match the provider");
        }
    }

    /** Validates credential use without allowing another adapter to consume a provider's key. */
    public static void requireTrustedConfiguration(String providerCode, String modelName,
                                                   String apiHost, String apiKey) {
        if (CustomApiCredentialPolicy.isCustomProvider(providerCode)) {
            CustomApiCredentialPolicy.requireConfiguration(providerCode, modelName, apiHost, apiKey);
        } else if (PPIO_PROVIDER.equals(providerCode)) {
            requirePpioConfiguration(providerCode, modelName, apiHost, apiKey);
        } else {
            requireDeepSeekConfiguration(providerCode, modelName, apiHost, apiKey);
        }
    }

    /** Validates PPIO and returns the base URL expected by the OpenAI Chat Completions client. */
    public static String requirePpioConfiguration(String providerCode, String modelName,
                                                  String apiHost, String apiKey) {
        if (!PPIO_PROVIDER.equals(providerCode)) {
            throw new IllegalArgumentException("PPIO model provider is not trusted");
        }
        // PPIO hosts models from multiple vendors; use the full model ID from its catalog.
        if (modelName == null || !modelName.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,254}")) {
            throw new IllegalArgumentException("PPIO model ID is invalid");
        }
        requireSecureApiHost(apiHost);
        if (!PPIO_API_HOSTS.contains(apiHost)) {
            throw new IllegalArgumentException("PPIO API host is not allowlisted");
        }
        requireProviderReference(providerCode, apiKey);
        return PPIO_API_HOST;
    }

    public static String requireSecureApiHost(String apiHost) {
        if (apiHost == null || apiHost.isBlank()) {
            throw new IllegalArgumentException("Model API host must be an absolute HTTPS URI");
        }
        final URI uri;
        try {
            uri = new URI(apiHost);
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException("Model API host must be an absolute HTTPS URI");
        }
        if (!uri.isAbsolute() || !"https".equals(uri.getScheme()) || uri.getHost() == null
            || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
            || uri.getPort() > 65535) {
            throw new IllegalArgumentException(
                "Model API host must be HTTPS without userinfo, query, or fragment");
        }
        return apiHost;
    }

    public static boolean isDeepSeekConfiguration(String providerCode, String modelName) {
        return DEEPSEEK_PROVIDER.equals(providerCode) || DEEPSEEK_MODELS.contains(modelName);
    }
}
