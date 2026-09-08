package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.SysUrlBo;
import org.ruoyi.system.domain.vo.SysUrlShortcutVo;
import org.ruoyi.system.domain.vo.SysUrlVo;
import org.ruoyi.system.service.ISysUrlService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * URL 管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/url")
public class SysUrlController extends BaseController {

    private final ISysUrlService urlService;

    /**
     * 分页查询 URL 列表（支持名称、状态筛选）
     */
    @SaCheckPermission("system:url:list")
    @GetMapping("/list")
    public TableDataInfo<SysUrlVo> list(SysUrlBo bo, PageQuery pageQuery) {
        return urlService.selectPageUrlList(bo, pageQuery);
    }

    /**
     * 获取 Copilot 新会话快捷入口（当前租户已启用链接）
     * 允许 coding:harness:use 或 system:url:list 权限
     */
    @SaCheckPermission(value = {"system:url:list", "coding:harness:use"}, mode = SaMode.OR)
    @GetMapping("/shortcuts")
    public R<List<SysUrlShortcutVo>> shortcuts() {
        return R.ok(urlService.selectShortcutList());
    }

    /**
     * 根据 ID 获取 URL 详细信息
     *
     * @param urlId 链接ID
     */
    @SaCheckPermission("system:url:query")
    @GetMapping(value = "/{urlId}")
    public R<SysUrlVo> getInfo(@NotNull(message = "链接ID不能为空") @PathVariable Long urlId) {
        return R.ok(urlService.selectUrlById(urlId));
    }

    /**
     * 新增 URL
     */
    @SaCheckPermission("system:url:add")
    @Log(title = "URL管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysUrlBo bo) {
        return toAjax(urlService.insertUrl(bo));
    }

    /**
     * 修改 URL
     */
    @SaCheckPermission("system:url:edit")
    @Log(title = "URL管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysUrlBo bo) {
        return toAjax(urlService.updateUrl(bo));
    }

    /**
     * 删除 URL（支持批量）
     *
     * @param urlIds 链接ID串
     */
    @SaCheckPermission("system:url:remove")
    @Log(title = "URL管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{urlIds}")
    public R<Void> remove(@PathVariable Long[] urlIds) {
        return toAjax(urlService.deleteUrlByIds(urlIds));
    }
}
