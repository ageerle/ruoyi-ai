package org.ruoyi.service.shortdrama.download;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Inet6Address;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class SafeVideoSourceDownloaderTest {

    @Test
    void rejectsPrivateAndSpecialUseIpv4Addresses() throws Exception {
        assertFalse(isPublic("127.0.0.1"));
        assertFalse(isPublic("10.0.0.1"));
        assertFalse(isPublic("100.64.0.1"));
        assertFalse(isPublic("169.254.169.254"));
        assertFalse(isPublic("172.16.0.1"));
        assertFalse(isPublic("192.168.1.1"));
        assertFalse(isPublic("198.18.0.1"));
        assertFalse(isPublic("224.0.0.1"));
    }

    @Test
    void identifiesTunFakeIpRangeWithoutTreatingItAsPublic() throws Exception {
        InetAddress fakeIp = InetAddress.getByName("198.18.0.226");

        assertTrue(SafeVideoSourceDownloader.isFakeProxyAddress(fakeIp));
        assertFalse(SafeVideoSourceDownloader.isPublicAddress(fakeIp));
        assertFalse(SafeVideoSourceDownloader.isFakeProxyAddress(InetAddress.getByName("192.168.1.1")));
    }
    @Test
    void rejectsPrivateAndSpecialUseIpv6Addresses() throws Exception {
        assertFalse(isPublic("::1"));
        assertFalse(isPublic("fe80::1"));
        assertFalse(isPublic("fd00::1"));
        assertFalse(isPublic("100::1"));
        assertFalse(isPublic("2001::1"));
        assertFalse(isPublic("2001:2::1"));
        assertFalse(isPublic("2001:10::1"));
        assertFalse(isPublic("2001:20::1"));
        assertFalse(isPublic("2001:db8::1"));
        assertFalse(isPublic("2002:0808:0808::1"));
        assertFalse(isPublic("2620:4f:8000::1"));
        assertFalse(isPublic("3ffe::1"));
        assertFalse(isPublic("3fff::1"));
        assertFalse(isPublic("5f00::1"));
    }

    @Test
    void rejectsIpv4EmbeddingAndTranslationPrefixes() throws Exception {
        assertFalse(isPublic("::8.8.8.8"));
        assertFalse(isPublic("64:ff9b::8.8.8.8"));
        assertFalse(isPublic("64:ff9b:1::8.8.8.8"));
        assertFalse(isPublic(rawMappedIpv6(8, 8, 8, 8)));
        assertFalse(isPublic(rawMappedIpv6(127, 0, 0, 1)));
    }

    @Test
    void acceptsRepresentativePublicAddresses() throws Exception {
        assertTrue(isPublic("8.8.8.8"));
        assertTrue(isPublic("1.1.1.1"));
        assertTrue(isPublic("2001:200::1"));
        assertTrue(isPublic("2001:4860:4860::8888"));
        assertTrue(isPublic("2606:4700:4700::1111"));
    }

    private static boolean isPublic(String value) throws Exception {
        return SafeVideoSourceDownloader.isPublicAddress(InetAddress.getByName(value));
    }

    private static boolean isPublic(InetAddress address) {
        return SafeVideoSourceDownloader.isPublicAddress(address);
    }

    private static Inet6Address rawMappedIpv6(int a, int b, int c, int d) throws Exception {
        byte[] bytes = new byte[16];
        bytes[10] = (byte) 0xFF;
        bytes[11] = (byte) 0xFF;
        bytes[12] = (byte) a;
        bytes[13] = (byte) b;
        bytes[14] = (byte) c;
        bytes[15] = (byte) d;
        return Inet6Address.getByAddress(null, bytes, -1);
    }
}
