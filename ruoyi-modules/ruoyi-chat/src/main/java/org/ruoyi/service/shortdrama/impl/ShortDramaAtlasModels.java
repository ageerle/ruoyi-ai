package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

/** Resolve short-drama models inside Atlas, never by a name shared across providers. */
public final class ShortDramaAtlasModels {
    private ShortDramaAtlasModels() {}

    public static ChatModelVo require(IChatModelService models, String name, String... categories) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("请选择已配置的 Atlas " + String.join("/", categories) + " 模型");
        return resolve(models, name, categories);
    }

    public static ChatModelVo defaultModel(IChatModelService models, String category) {
        return resolve(models, null, category);
    }

    private static ChatModelVo resolve(IChatModelService models, String name, String... categories) {
        var allowed = Set.of(categories);
        var query = new ChatModelBo();
        query.setProviderCode("atlas"); query.setCategory(categories.length == 1 ? categories[0] : null); query.setModelName(name);
        var matches = models.queryAvailableList(query).stream()
            .filter(model -> "atlas".equalsIgnoreCase(model.getProviderCode()) && model.getCategory() != null && allowed.contains(model.getCategory())
                && (name == null || Objects.equals(name, model.getModelName())))
            .toList();
        if (matches.isEmpty()) throw new IllegalArgumentException("短剧仅支持已启用的 Atlas " + String.join("/", categories) + " 模型，请检查后台配置：" + Objects.toString(name, "默认模型"));
        var selected = matches.get(0);
        if (matches.stream().filter(model -> Objects.equals(selected.getModelName(), model.getModelName())).count() > 1)
            throw new IllegalArgumentException("Atlas 同类模型存在重复配置，请保留唯一配置：" + selected.getModelName());
        requireProvider(selected);
        return selected;
    }

    public static void requireProvider(ChatModelVo model) {
        if (model == null || !"atlas".equalsIgnoreCase(model.getProviderCode()))
            throw new IllegalArgumentException("短剧仅支持 Atlas 模型");
        try {
            var uri = URI.create(Objects.toString(model.getApiHost(), ""));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"api.atlascloud.ai".equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException("短剧 Atlas 模型接口地址必须使用 https://api.atlascloud.ai");
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("短剧 Atlas 模型接口地址必须使用 https://api.atlascloud.ai");
        }
        if (model.getApiKey() == null || model.getApiKey().isBlank())
            throw new IllegalArgumentException("Atlas 模型 API Key 未配置，请在后台填写后重试");
    }
}
