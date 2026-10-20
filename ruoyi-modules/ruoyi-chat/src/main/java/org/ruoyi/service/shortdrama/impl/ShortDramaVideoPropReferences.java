package org.ruoyi.service.shortdrama.impl;

import java.util.List;

/** Give each attached prop an explicit role instead of an unlabelled extra image. */
final class ShortDramaVideoPropReferences {
    record Binding(String title, String imageUrl) { }

    static String direction(List<Binding> bindings, List<String> images) {
        if (bindings.isEmpty() || images == null) return "";
        var text = new StringBuilder("\n[本镜道具身份参考]\n");
        for (var binding : bindings) {
            int index = images.indexOf(binding.imageUrl());
            if (index < 0) throw new IllegalArgumentException("道具参考未实际附图：" + binding.title());
            text.append(binding.title()).append("对应@image").append(index + 1)
                .append("；\n");
        }
        return text.append("图片仅提供器物外形，位置、持握与出现时机按导演稿。\n").toString();
    }
}
