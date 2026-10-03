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
                .append("，仅提供器物轮廓、材质与比例；位置、持握者、动作和出现时机按本镜导演稿。")
                .append("同一物件保持形制，不能复制棚拍背景或让器物凭空出现。\n");
        }
        return text.toString();
    }
}
