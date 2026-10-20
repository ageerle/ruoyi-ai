package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.service.media.AtlasMediaSupport;
import java.net.URI;
import java.util.*;

/** Transport copies keep the approved asset and its reference index unchanged. */
final class ShortDramaVideoReferenceMirrors {
    private ShortDramaVideoReferenceMirrors() {}

    static List<String> apply(String continuity, List<String> originals) {
        try {
            var root = AtlasMediaSupport.OBJECT_MAPPER.readTree(continuity == null || continuity.isBlank() ? "{}" : continuity);
            var mirrors = root.path("video_reference_mirrors");
            if (mirrors.isMissingNode() || mirrors.isNull()) return originals;
            if (!mirrors.isObject()) throw new IllegalArgumentException("视频参考镜像须为原图URL到上传URL的映射");
            var selected = originals == null ? List.<String>of() : originals;
            var names = mirrors.fieldNames();
            while (names.hasNext()) {
                String source = names.next();
                if (!selected.contains(source)) throw new IllegalArgumentException("视频参考镜像已过期：原图不在本镜参考中");
                var target = mirrors.get(source);
                if (!target.isTextual()) throw new IllegalArgumentException("视频参考镜像URL无效");
                URI uri = URI.create(target.asText());
                if (!Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException("视频参考镜像必须是完整HTTP图片URL");
            }
            return selected.stream().map(source -> mirrors.has(source) ? mirrors.get(source).asText() : source).toList();
        } catch (java.io.IOException e) { throw new IllegalArgumentException("视频参考镜像JSON无效", e); }
    }
}
