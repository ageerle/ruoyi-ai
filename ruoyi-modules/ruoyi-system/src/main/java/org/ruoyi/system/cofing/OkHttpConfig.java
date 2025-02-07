package org.ruoyi.system.cofing;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.OkHttpUtil;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.vo.SysModelVo;
import org.ruoyi.system.service.ISysModelService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OkHttpConfig {

    private final ISysModelService sysModelService;
    private final Map<String, OkHttpUtil> okHttpUtilMap = new HashMap<>();
    @Getter
    private String generate;

    @PostConstruct
    public void init() {
        initializeOkHttpUtil("suno");
        initializeOkHttpUtil("luma");
    }

    private void initializeOkHttpUtil(String modelName) {
        SysModelBo sysModelBo = new SysModelBo();
        sysModelBo.setModelName(modelName);
        List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
        if (!sysModelList.isEmpty()) {
            SysModelVo model = sysModelList.get(0);
            OkHttpUtil okHttpUtil = new OkHttpUtil();
            okHttpUtil.setApiHost(model.getApiHost());
            okHttpUtil.setApiKey(model.getApiKey());
            generate = String.valueOf(model.getModelPrice());
            okHttpUtilMap.put(modelName, okHttpUtil);
        }
    }

    public OkHttpUtil getOkHttpUtil(String modelName) {
        return okHttpUtilMap.get(modelName);
    }
}
