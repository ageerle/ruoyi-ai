package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatAppStoreBo;
import org.ruoyi.domain.vo.ChatAppStoreVo;
import org.ruoyi.service.IChatAppStoreService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;

/**
 * 应用商店
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/appStore")
public class ChatAppStoreController extends BaseController {

    private final IChatAppStoreService chatAppStoreService;

    /**
     * 查询应用商店列表
     */
    @SaCheckPermission("system:appStore:list")
    @GetMapping("/list")
    public TableDataInfo<ChatAppStoreVo> list(ChatAppStoreBo bo, PageQuery pageQuery) {
        return chatAppStoreService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出应用商店列表
     */
    @SaCheckPermission("system:appStore:export")
    @Log(title = "应用商店", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatAppStoreBo bo, HttpServletResponse response) {
        List<ChatAppStoreVo> list = chatAppStoreService.queryList(bo);
        ExcelUtil.exportExcel(list, "应用商店", ChatAppStoreVo.class, response);
    }

    /**
     * 获取应用商店详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:appStore:query")
    @GetMapping("/{id}")
    public R<ChatAppStoreVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatAppStoreService.queryById(id));
    }

    /**
     * 新增应用商店
     */
    @SaCheckPermission("system:appStore:add")
    @Log(title = "应用商店", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatAppStoreBo bo) {
        return toAjax(chatAppStoreService.insertByBo(bo));
    }

    /**
     * 修改应用商店
     */
    @SaCheckPermission("system:appStore:edit")
    @Log(title = "应用商店", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatAppStoreBo bo) {
        return toAjax(chatAppStoreService.updateByBo(bo));
    }

    /**
     * 删除应用商店
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:appStore:remove")
    @Log(title = "应用商店", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatAppStoreService.deleteWithValidByIds(List.of(ids), true));
    }
}
