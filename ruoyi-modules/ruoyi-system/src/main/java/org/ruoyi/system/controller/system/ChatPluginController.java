package org.ruoyi.system.controller.system;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.system.domain.vo.ChatPluginVo;
import org.ruoyi.system.domain.bo.ChatPluginBo;
import org.ruoyi.system.service.IChatPluginService;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 插件管理
 *
 * @author ageerle
 * @date 2025-03-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/plugin")
public class ChatPluginController extends BaseController {

    private final IChatPluginService chatPluginService;

    /**
     * 查询插件管理列表
     */
    @SaCheckPermission("system:plugin:list")
    @GetMapping("/list")
    public TableDataInfo<ChatPluginVo> list(ChatPluginBo bo, PageQuery pageQuery) {
        return chatPluginService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出插件管理列表
     */
    @SaCheckPermission("system:plugin:export")
    @Log(title = "插件管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatPluginBo bo, HttpServletResponse response) {
        List<ChatPluginVo> list = chatPluginService.queryList(bo);
        ExcelUtil.exportExcel(list, "插件管理", ChatPluginVo.class, response);
    }

    /**
     * 获取插件管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:plugin:query")
    @GetMapping("/{id}")
    public R<ChatPluginVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatPluginService.queryById(id));
    }

    /**
     * 新增插件管理
     */
    @SaCheckPermission("system:plugin:add")
    @Log(title = "插件管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatPluginBo bo) {
        return toAjax(chatPluginService.insertByBo(bo));
    }

    /**
     * 修改插件管理
     */
    @SaCheckPermission("system:plugin:edit")
    @Log(title = "插件管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatPluginBo bo) {
        return toAjax(chatPluginService.updateByBo(bo));
    }

    /**
     * 删除插件管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:plugin:remove")
    @Log(title = "插件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatPluginService.deleteWithValidByIds(List.of(ids), true));
    }
}
