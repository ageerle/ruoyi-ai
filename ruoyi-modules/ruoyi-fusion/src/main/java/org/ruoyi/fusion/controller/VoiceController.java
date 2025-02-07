package org.ruoyi.fusion.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.VoiceRoleBo;
import org.ruoyi.system.domain.vo.VoiceRoleVo;
import org.ruoyi.system.request.RoleListDto;
import org.ruoyi.system.request.RoleRequest;
import org.ruoyi.system.request.SimpleGenerateRequest;
import org.ruoyi.system.response.SimpleGenerateDataResponse;
import org.ruoyi.system.response.rolelist.RoleListVO;
import org.ruoyi.system.service.IVoiceRoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 配音角色
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/voice")
public class VoiceController extends BaseController {

    private final IVoiceRoleService voiceRoleService;

    /**
     * 查询配音角色列表
     */
    @GetMapping("/list")
    public List<VoiceRoleVo> list(VoiceRoleBo bo) {
        if(LoginHelper.getUserId() == null){
            return new ArrayList<>();
        }
        bo.setCreateBy(LoginHelper.getUserId());
        return voiceRoleService.queryList(bo);
    }

    /**
     * 获取配音角色详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:role:query")
    @GetMapping("/{id}")
    public R<VoiceRoleVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(voiceRoleService.queryById(id));
    }


    /**
     * 新增配音角色
     */
    @Log(title = "配音角色", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<Void> add(@RequestBody RoleRequest roleRequest) {
        return toAjax(voiceRoleService.insertByBo(roleRequest));
    }

    /**
     * 修改配音角色
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "配音角色", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody VoiceRoleBo bo) {
        return toAjax(voiceRoleService.updateByBo(bo));
    }

    /**
     * 删除配音角色
     *
     * @param ids 主键串
     */
    @Log(title = "配音角色", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(voiceRoleService.deleteWithValidByIds(List.of(ids), true));
    }

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
    public R<List<RoleListVO>> roleList() {
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
