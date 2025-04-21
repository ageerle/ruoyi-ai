package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatGptsBo;
import org.ruoyi.domain.vo.ChatGptsVo;
import org.ruoyi.service.IChatGptsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;

/**
 * 应用管理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/gpts")
public class ChatGptsController extends BaseController {

    private final IChatGptsService chatGptsService;

    /**
     * 查询应用管理列表
     */
    @SaCheckPermission("system:gpts:list")
    @GetMapping("/list")
    public TableDataInfo<ChatGptsVo> list(ChatGptsBo bo, PageQuery pageQuery) {
        return chatGptsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出应用管理列表
     */
    @SaCheckPermission("system:gpts:export")
    @Log(title = "应用管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatGptsBo bo, HttpServletResponse response) {
        List<ChatGptsVo> list = chatGptsService.queryList(bo);
        ExcelUtil.exportExcel(list, "应用管理", ChatGptsVo.class, response);
    }

    /**
     * 获取应用管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:gpts:query")
    @GetMapping("/{id}")
    public R<ChatGptsVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatGptsService.queryById(id));
    }

    /**
     * 新增应用管理
     */
    @SaCheckPermission("system:gpts:add")
    @Log(title = "应用管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatGptsBo bo) {
        return toAjax(chatGptsService.insertByBo(bo));
    }

    /**
     * 修改应用管理
     */
    @SaCheckPermission("system:gpts:edit")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatGptsBo bo) {
        return toAjax(chatGptsService.updateByBo(bo));
    }

    /**
     * 删除应用管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:gpts:remove")
    @Log(title = "应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatGptsService.deleteWithValidByIds(List.of(ids), true));
    }
}
