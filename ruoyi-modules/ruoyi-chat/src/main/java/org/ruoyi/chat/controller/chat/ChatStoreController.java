package org.ruoyi.chat.controller.chat;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.ChatAppStoreBo;
import org.ruoyi.domain.vo.ChatAppStoreVo;
import org.ruoyi.service.IChatAppStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
