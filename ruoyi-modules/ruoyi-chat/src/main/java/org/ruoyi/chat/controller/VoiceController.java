package org.ruoyi.chat.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.request.RoleListDto;
import org.ruoyi.system.request.SimpleGenerateRequest;
import org.ruoyi.system.response.SimpleGenerateDataResponse;
import org.ruoyi.system.response.rolelist.ChatAppStoreVO;
import org.ruoyi.system.service.IChatAppStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用市场
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/voice")
public class VoiceController extends BaseController {

    private final IChatAppStoreService voiceRoleService;

    /**
     * 实时语音生成
     */
    @PostMapping("/simpleGenerate")
    public R<SimpleGenerateDataResponse> simpleGenerate(@RequestBody SimpleGenerateRequest simpleGenerateRequest) {
        return R.ok(voiceRoleService.simpleGenerate(simpleGenerateRequest));
    }

    /**
     * 角色市场
     */
    @GetMapping("/roleList")
    public R<List<ChatAppStoreVO>> roleList() {
        return R.ok(voiceRoleService.roleList());
    }

    /**
     * 收藏角色
     */
    @PostMapping("/copyRole")
    public R<String> copyRole(@RequestBody RoleListDto roleListDto) {
        voiceRoleService.copyRole(roleListDto);
        return R.ok();
    }
}
