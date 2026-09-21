package org.ruoyi.common.chat.security;

import java.net.URI;

/** Normalizes the OpenAI/Anthropic endpoint configured in model management. */
public final class CustomApiCredentialPolicy {

    public static final String OPENAI_PROVIDER = "custom_api";
    public static final String ANTHROPIC_PROVIDER = "custom_anthropic";

    private CustomApiCredentialPolicy() {
    }

    public static boolean isCustomProvider(String providerCode) {
        return OPENAI_PROVIDER.equals(providerCode) || ANTHROPIC_PROVIDER.equals(providerCode);
    }

    /** Accepts a base URL or the selected protocol's complete chat endpoint. */
    public static String normalizeBaseUrl(String providerCode, String apiHost) {
        if (!isCustomProvider(providerCode)) {
            throw new IllegalArgumentException("不支持的自定义厂商协议");
        }
        if (apiHost == null || apiHost.isBlank()) {
            throw new IllegalArgumentException("请填写自定义厂商的请求地址");
        }
        URI uri = URI.create(apiHost);
        if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
            || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("请求地址必须是有效的 HTTP 或 HTTPS 地址");
        }
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
