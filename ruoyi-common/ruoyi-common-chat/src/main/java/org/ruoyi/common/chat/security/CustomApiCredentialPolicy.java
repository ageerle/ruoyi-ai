package org.ruoyi.common.chat.security;

import java.net.URI;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Binds configurable OpenAI/Anthropic endpoints to dedicated deployment credentials. */
public final class CustomApiCredentialPolicy {

    public static final String OPENAI_PROVIDER = "custom_api";
    public static final String ANTHROPIC_PROVIDER = "custom_anthropic";
    public static final String REFERENCE_PATTERN =
        "CUSTOM_(?:OPENAI|ANTHROPIC)(?:_[A-Z][A-Z0-9_]{0,31})?_API_KEY";

    private CustomApiCredentialPolicy() {
    }

    public static boolean isCustomProvider(String providerCode) {
        return OPENAI_PROVIDER.equals(providerCode) || ANTHROPIC_PROVIDER.equals(providerCode);
    }

    public static void requireReference(String providerCode, String reference) {
        String protocol;
        if (OPENAI_PROVIDER.equals(providerCode)) {
            protocol = "OPENAI";
        } else if (ANTHROPIC_PROVIDER.equals(providerCode)) {
            protocol = "ANTHROPIC";
        } else {
            throw new IllegalArgumentException("不支持的自定义厂商协议");
        }
        String pattern = "env:CUSTOM_" + protocol + "(?:_[A-Z][A-Z0-9_]{0,31})?_API_KEY";
        if (reference == null || !Pattern.matches(pattern, reference)) {
            throw new IllegalArgumentException("自定义厂商密钥必须使用与协议匹配的环境变量引用");
        }
    }

    public static String requireConfiguration(String providerCode, String modelName,
                                               String apiHost, String reference) {
        return requireConfiguration(providerCode, modelName, apiHost, reference, System::getenv);
    }

    static String requireConfiguration(String providerCode, String modelName, String apiHost,
                                       String reference, Function<String, String> environment) {
        requireReference(providerCode, reference);
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("自定义厂商的模型名称不能为空");
        }
        String baseUrl = normalizeBaseUrl(providerCode, apiHost);
        String keyVariable = reference.substring("env:".length());
        String hostVariable = keyVariable.substring(0, keyVariable.length() - "_API_KEY".length()) + "_BASE_URL";
        String configuredHost = environment.apply(hostVariable);
        if (configuredHost == null || configuredHost.isBlank()) {
            throw new IllegalArgumentException("请先在后端配置自定义厂商地址环境变量：" + hostVariable);
        }
        if (!baseUrl.equals(normalizeBaseUrl(providerCode, configuredHost))) {
            throw new IllegalArgumentException("模型请求地址与该密钥绑定的自定义厂商地址不一致");
        }
        return baseUrl;
    }

    /** Accepts a base URL or the selected protocol's complete chat endpoint. */
    public static String normalizeBaseUrl(String providerCode, String apiHost) {
        if (!isCustomProvider(providerCode)) {
            throw new IllegalArgumentException("不支持的自定义厂商协议");
        }
        ChatModelCredentialPolicy.requireSecureApiHost(apiHost);
        String normalized = apiHost.replaceAll("/+$", "");
        String suffix = ANTHROPIC_PROVIDER.equals(providerCode) ? "/messages" : "/chat/completions";
        String otherSuffix = ANTHROPIC_PROVIDER.equals(providerCode) ? "/chat/completions" : "/messages";
        if (normalized.endsWith(otherSuffix)) {
            throw new IllegalArgumentException("请求地址与所选自定义厂商协议不匹配");
        }
        if (normalized.endsWith(suffix)) {
            normalized = normalized.substring(0, normalized.length() - suffix.length());
        } else if (URI.create(normalized).getPath().isEmpty()) {
            normalized += "/v1";
        }
        return normalized;
    }
}
