package org.ruoyi.service.shortdrama.download;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "short-drama.composition.download")
public class VideoSourceDownloadProperties {

    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration callTimeout = Duration.ofMinutes(10);
    private int maxRedirects = 5;

    /**
     * Optional exact hosts or wildcard suffixes such as *.example.com.
     * When empty, any public destination is accepted.
     */
    private List<String> allowedHosts = new ArrayList<>();
    /**
     * Hosts allowed to resolve into a TUN proxy fake-IP range (198.18.0.0/15).
     * This exception never applies to loopback, RFC1918 or link-local addresses.
     */
    private List<String> fakeIpAllowedHosts = new ArrayList<>();
}
