package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Test catalog with the same provider/category/name scope as production. No network calls. */
final class ShortDramaAtlasTestModels {
    private static final Map<IChatModelService, Map<String, ChatModelVo>> CATALOGS = new WeakHashMap<>();
    static synchronized void register(IChatModelService service, String name, ChatModelVo model, String category) {
        model.setModelName(name); model.setProviderCode("atlas"); model.setCategory(category);
        model.setApiHost("https://api.atlascloud.ai/v1"); model.setApiKey("test-only-key");
        var catalog = CATALOGS.computeIfAbsent(service, ignored -> new LinkedHashMap<>());
        catalog.put(category + ":" + name, model);
        doAnswer(call -> {
            ChatModelBo query = call.getArgument(0);
            return catalog.values().stream().filter(entry -> entry.getProviderCode().equals(query.getProviderCode())
                && (query.getCategory() == null || entry.getCategory().equals(query.getCategory()))
                && (query.getModelName() == null || entry.getModelName().equals(query.getModelName()))).toList();
        }).when(service).queryAvailableList(any());
    }
}
