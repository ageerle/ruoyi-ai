package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.chat.enums.DisplayType;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
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
 * 聊天模型
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/model")
public class ChatModelController extends BaseController {

    private final IChatModelService chatModelService;

    /**
     * 查询聊天模型列表
     */
    @SaCheckPermission("system:model:list")
    @GetMapping("/list")
    public TableDataInfo<ChatModelVo> list(ChatModelBo bo, PageQuery pageQuery) {
        return chatModelService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询用户模型列表
     */
    @GetMapping("/modelList")
    public R<List<ChatModelVo>> modelList(ChatModelBo bo) {
        bo.setModelShow(DisplayType.VISIBLE.getCode());
        return R.ok(chatModelService.queryList(bo));
    }

    /**
     * 导出聊天模型列表
     */
    @SaCheckPermission("system:model:export")
    @Log(title = "聊天模型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatModelBo bo, HttpServletResponse response) {
        List<ChatModelVo> list = chatModelService.queryList(bo);
        ExcelUtil.exportExcel(list, "聊天模型", ChatModelVo.class, response);
    }

    /**
     * 获取聊天模型详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:model:query")
    @GetMapping("/{id}")
    public R<ChatModelVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatModelService.queryById(id));
    }

    /**
     * 新增聊天模型
     */
    @SaCheckPermission("system:model:add")
    @Log(title = "聊天模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatModelBo bo) {
        return toAjax(chatModelService.insertByBo(bo));
    }

    /**
     * 修改聊天模型
     */
    @SaCheckPermission("system:model:edit")
    @Log(title = "聊天模型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatModelBo bo) {
        return toAjax(chatModelService.updateByBo(bo));
    }

    /**
     * 删除聊天模型
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:model:remove")
    @Log(title = "聊天模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatModelService.deleteWithValidByIds(List.of(ids), true));
    }
}
