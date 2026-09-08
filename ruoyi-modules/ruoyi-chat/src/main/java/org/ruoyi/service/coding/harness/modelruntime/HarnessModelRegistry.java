package org.ruoyi.service.coding.harness.modelruntime;

import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.springframework.stereotype.Service;

/** Resolves Harness model names against the same tenant-scoped registry used by chat. */
@Service
public class HarnessModelRegistry {

    private final IChatModelService chatModelService;

    public HarnessModelRegistry(IChatModelService chatModelService) {
        this.chatModelService = chatModelService;
    }

    public ChatModelVo requireConfigured(String modelName) {
        String normalized = HarnessModelPolicy.normalizeModel(modelName);
        ChatModelVo model = chatModelService.selectModelByName(normalized);
        if (model == null) {
            throw new IllegalArgumentException("模型未配置或已删除: " + normalized);
        }
        return model;
    }

    /** An automatic session is admitted only when every deterministic route candidate exists. */
    public void requireSessionModelReady(String requestedModel) {
        String normalized = HarnessModelPolicy.normalizeModel(requestedModel);
        if (!HarnessModelPolicy.isAutomatic(normalized)) {
            requireConfigured(normalized);
            return;
        }
        HarnessModelPolicy.automaticCandidates().forEach(this::requireConfigured);
    }
}
