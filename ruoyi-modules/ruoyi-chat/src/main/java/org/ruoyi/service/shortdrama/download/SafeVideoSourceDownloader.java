package org.ruoyi.service.shortdrama.download;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class SafeVideoSourceDownloader {

    private final VideoSourceDownloadProperties properties;

    public SafeVideoSourceDownloader(VideoSourceDownloadProperties properties) {
        this.properties = properties;
        validateProperties(properties);
    }

    public long download(String sourceUrl, Path target, long maxSourceBytes, long remainingTotalBytes) {
        if (maxSourceBytes <= 0 || remainingTotalBytes <= 0) {
            throw new VideoSourceDownloadException("Video download size limit is invalid");
        }
        URI current = parseUri(sourceUrl);

        try {
            for (int redirectCount = 0; ; redirectCount++) {
                ResolvedTarget resolved = resolvePublicTarget(current);
                OkHttpClient client = clientFor(resolved);
                try (Response response = client.newCall(new Request.Builder()
                    .url(current.toASCIIString())
                    .header("User-Agent", "ruoyi-ai-short-drama-composer")
                    .get()
                    .build()).execute()) {
                    if (isRedirect(response.code())) {
                        if (redirectCount >= properties.getMaxRedirects()) {
                            throw new VideoSourceDownloadException("Video source exceeded the redirect limit");
                        }
                        String location = response.header("Location");
                        if (location == null || location.isBlank()) {
                            throw new VideoSourceDownloadException("Video source redirect has no Location header");
                        }
                        URI redirected = parseUri(current.resolve(location).toString());
                        if ("https".equalsIgnoreCase(current.getScheme())
                            && "http".equalsIgnoreCase(redirected.getScheme())) {
                            throw new VideoSourceDownloadException("HTTPS video source cannot redirect to HTTP");
                        }
                        current = redirected;
                        continue;
                    }
                    if (!response.isSuccessful()) {
                        throw new VideoSourceDownloadException("Video source download failed with HTTP " + response.code());
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new VideoSourceDownloadException("Video source response is empty");
                    }
                    assertSize(body.contentLength(), maxSourceBytes, remainingTotalBytes);
                    try (InputStream input = body.byteStream()) {
                        long downloaded = copyWithLimit(input, target, maxSourceBytes, remainingTotalBytes);
                        if (downloaded == 0) {
                            throw new VideoSourceDownloadException("Downloaded video source is empty");
                        }
                        return downloaded;
                    }
                } finally {
                    client.connectionPool().evictAll();
                    client.dispatcher().executorService().shutdown();
                }
            }
        } catch (IOException ex) {
            deletePartial(target);
            throw new VideoSourceDownloadException("Video source download failed", ex);
        } catch (RuntimeException ex) {
            deletePartial(target);
            throw ex;
        }
    }

    private OkHttpClient clientFor(ResolvedTarget target) {
        Dns pinnedDns = hostname -> {
            String requested = normalizeHost(hostname);
            if (!target.host().equals(requested)) {
                throw new UnknownHostException("Unvalidated redirect destination: " + hostname);
            }
            return target.addresses();
        };
        return new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns(pinnedDns)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(properties.getConnectTimeout())
            .callTimeout(properties.getCallTimeout())
            .readTimeout(properties.getCallTimeout())
            .build();
    }

    private ResolvedTarget resolvePublicTarget(URI uri) {
        String host = normalizeHost(uri.getHost());
        enforceHostAllowlist(host);
        final List<InetAddress> addresses;
        try {
            addresses = List.copyOf(Arrays.asList(InetAddress.getAllByName(host)));
        } catch (UnknownHostException ex) {
            throw new VideoSourceDownloadException("Video source host cannot be resolved", ex);
        }
        boolean fakeIpAllowed = matchesHostRules(host, properties.getFakeIpAllowedHosts());
        if (addresses.isEmpty() || addresses.stream().anyMatch(address ->
            !isPublicAddress(address) && !(fakeIpAllowed && isFakeProxyAddress(address)))) {
            throw new VideoSourceDownloadException("Video source resolves to a private or special-use address");
        }
        return new ResolvedTarget(host, addresses);
    }

    private boolean matchesHostRules(String host, List<String> rules) {
        if (rules == null || rules.isEmpty()) return false;
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) continue;
            String normalizedRule = normalizeHostRule(rule);
            if (normalizedRule.startsWith("*.")) {
                String suffix = normalizedRule.substring(1);
                if (host.endsWith(suffix) && host.length() > suffix.length()) return true;
            } else if (host.equals(normalizedRule)) {
                return true;
            }
        }
        return false;
    }
    private void enforceHostAllowlist(String host) {
        List<String> rules = properties.getAllowedHosts();
        if (rules == null || rules.stream().allMatch(rule -> rule == null || rule.isBlank())) {
            return;
        }
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) {
                continue;
            }
            String normalizedRule = normalizeHostRule(rule);
            if (normalizedRule.startsWith("*.")) {
                String suffix = normalizedRule.substring(1);
                if (host.endsWith(suffix) && host.length() > suffix.length()) {
                    return;
                }
            } else if (host.equals(normalizedRule)) {
                return;
            }
        }
        throw new VideoSourceDownloadException("Video source host is not allowlisted");
    }

    private static URI parseUri(String value) {
        final URI uri;
        try {
            uri = URI.create(value).normalize();
        } catch (RuntimeException ex) {
            throw new VideoSourceDownloadException("Video source URL is invalid", ex);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new VideoSourceDownloadException("Video source URL must use HTTP or HTTPS");
        }
        if (uri.getHost() == null || uri.getHost().isBlank() || uri.getRawUserInfo() != null) {
            throw new VideoSourceDownloadException("Video source URL authority is invalid");
        }
        if (uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65_535) {
            throw new VideoSourceDownloadException("Video source URL port is invalid");
        }
        return uri;
    }

    static boolean isFakeProxyAddress(InetAddress address) {
        if (!(address instanceof Inet4Address)) return false;
        byte[] bytes = address.getAddress();
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 198 && (second == 18 || second == 19);
    }
    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isPublicIpv4(bytes);
        }
        if (address instanceof Inet6Address) {
            return isPublicIpv6(bytes);
        }
        return false;
    }

    private static boolean isPublicIpv6(byte[] bytes) {
        // Only 2000::/3 global unicast is eligible. Mapped/compatible and standard NAT64 are outside it.
        if (!matchesPrefix(bytes, 3, 0x20)) {
            return false;
        }

        // IANA special-purpose entries within 2000::/3, plus the retired 6bone block.
        // The 2001::/23 parent also covers Teredo, benchmarking and both ORCHID ranges.
        return !matchesPrefix(bytes, 23, 0x20, 0x01, 0x00)
            && !matchesPrefix(bytes, 32, 0x20, 0x01, 0x0D, 0xB8)
            && !matchesPrefix(bytes, 16, 0x20, 0x02)
            && !matchesPrefix(bytes, 48, 0x26, 0x20, 0x00, 0x4F, 0x80, 0x00)
            && !matchesPrefix(bytes, 16, 0x3F, 0xFE)
            && !matchesPrefix(bytes, 20, 0x3F, 0xFF, 0x00);
    }

    private static boolean isPublicIpv4(byte[] bytes) {
        int a = bytes[0] & 0xFF;
        int b = bytes[1] & 0xFF;
        int c = bytes[2] & 0xFF;
        if (a == 0 || a == 10 || a == 127 || a >= 224) {
            return false;
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return false;
        }
        if (a == 169 && b == 254) {
            return false;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return false;
        }
        if (a == 192 && (b == 168 || (b == 0 && c == 0) || (b == 0 && c == 2))) {
            return false;
        }
        if (a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100))) {
            return false;
        }
        return !(a == 203 && b == 0 && c == 113);
    }

    private static boolean matchesPrefix(byte[] address, int prefixLength, int... prefixBytes) {
        int fullBytes = prefixLength / Byte.SIZE;
        int remainingBits = prefixLength % Byte.SIZE;
        int requiredBytes = fullBytes + (remainingBits == 0 ? 0 : 1);
        if (address.length < requiredBytes || prefixBytes.length < requiredBytes) {
            return false;
        }
        for (int index = 0; index < fullBytes; index++) {
            if ((address[index] & 0xFF) != prefixBytes[index]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (Byte.SIZE - remainingBits);
        return ((address[fullBytes] & 0xFF) & mask) == (prefixBytes[fullBytes] & mask);
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            throw new VideoSourceDownloadException("Video source host is missing");
        }
        String unwrapped = host.startsWith("[") && host.endsWith("]")
            ? host.substring(1, host.length() - 1)
            : host;
        if (unwrapped.indexOf(':') >= 0) {
            return unwrapped.toLowerCase(Locale.ROOT);
        }
        try {
            return IDN.toASCII(unwrapped, IDN.USE_STD3_ASCII_RULES)
                .toLowerCase(Locale.ROOT)
                .replaceFirst("\\.$", "");
        } catch (IllegalArgumentException ex) {
            throw new VideoSourceDownloadException("Video source host is invalid", ex);
        }
    }

    private static String normalizeHostRule(String rule) {
        String trimmed = rule.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("*.")) {
            return "*." + normalizeHost(trimmed.substring(2));
        }
        return normalizeHost(trimmed);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static long copyWithLimit(
        InputStream input,
        Path target,
        long maxSourceBytes,
        long remainingTotalBytes
    ) throws IOException {
        long downloaded = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream output = Files.newOutputStream(
            target,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                downloaded += read;
                assertSize(downloaded, maxSourceBytes, remainingTotalBytes);
                output.write(buffer, 0, read);
            }
        }
        return downloaded;
    }

    private static void assertSize(long bytes, long maxSourceBytes, long remainingTotalBytes) {
        if (bytes < 0) {
            return;
        }
        if (bytes > maxSourceBytes) {
            throw new VideoSourceDownloadException("A video source exceeds the per-file size limit");
        }
        if (bytes > remainingTotalBytes) {
            throw new VideoSourceDownloadException("Video sources exceed the total download size limit");
        }
    }

    private static void deletePartial(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // The enclosing composition job also removes its complete work directory.
        }
    }

    private static void validateProperties(VideoSourceDownloadProperties properties) {
        Duration connectTimeout = properties.getConnectTimeout();
        Duration callTimeout = properties.getCallTimeout();
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
            || callTimeout == null || callTimeout.isZero() || callTimeout.isNegative()
            || properties.getMaxRedirects() < 0 || properties.getMaxRedirects() > 10) {
            throw new IllegalArgumentException("Invalid secure video download configuration");
        }
    }

    private record ResolvedTarget(String host, List<InetAddress> addresses) {
    }
}
