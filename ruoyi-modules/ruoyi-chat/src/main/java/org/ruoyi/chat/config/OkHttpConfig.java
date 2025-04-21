package org.ruoyi.chat.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.OkHttpUtil;

import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OkHttpConfig {

    private final IChatModelService chatModelService;
    private final Map<String, OkHttpUtil> okHttpUtilMap = new HashMap<>();
    @Getter
    private String generate;

    @PostConstruct
    public void init() {
        initializeOkHttpUtil("suno");
        initializeOkHttpUtil("luma");
        initializeOkHttpUtil("ppt");
    }

    private void initializeOkHttpUtil(String modelName) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(modelName);
        OkHttpUtil okHttpUtil = new OkHttpUtil();
        okHttpUtil.setApiHost(chatModelVo.getApiHost());
        okHttpUtil.setApiKey(chatModelVo.getApiKey());
        generate = String.valueOf(chatModelVo.getModelPrice());
        okHttpUtilMap.put(modelName, okHttpUtil);
    }

    public OkHttpUtil getOkHttpUtil(String modelName) {
        return okHttpUtilMap.get(modelName);
    }
}
