package org.ruoyi.system.controller.system;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.ChatAppStoreBo;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.vo.ChatAppStoreVo;
import org.ruoyi.system.service.IChatAppStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用商店
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/store")
public class ChatStoreController extends BaseController {

    private final IChatAppStoreService appStoreService;

    /**
     * 应用商店
     */
    @GetMapping("/appList")
    public R<List<ChatAppStoreVo>> appList(ChatAppStoreBo bo) {
        return R.ok(appStoreService.queryList(bo));
    }

    /**
     * 收藏应用
     */
    @PostMapping("/copyApp")
    public R<String> copyApp() {
        return R.ok();
    }
}
