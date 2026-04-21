package org.ruoyi.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.enums.ModelType;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

import java.util.LinkedHashMap;

/**
 * 模型管理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/model")
public class ChatModelController extends BaseController {

    private final IChatModelService chatModelService;

    /**
     * 查询模型管理列表
     */
    @SaCheckPermission("system:model:list")
    @GetMapping("/list")
    public TableDataInfo<ChatModelVo> list(ChatModelBo bo, PageQuery pageQuery) {
        return chatModelService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询用户聊天模型列表
     */
    @GetMapping("/modelList")
    public R<List<ChatModelVo>> modelList(ChatModelBo bo) {
        bo.setCategory(ModelType.CHAT.getKey());
        return R.ok(chatModelService.queryList(bo));
    }

    /**
     * 获取模型供应商枚举
     */
    @GetMapping("/providerOptions")
    public R<List<LinkedHashMap<String, String>>> providerOptions() {
        List<LinkedHashMap<String, String>> options = new java.util.ArrayList<>();
        for (ChatModeType type : ChatModeType.values()) {
            LinkedHashMap<String, String> item = new LinkedHashMap<>();
            item.put("label", type.getDescription());
            item.put("value", type.getCode());
            options.add(item);
        }
        return R.ok(options);
    }

    /**
     * 导出模型管理列表
     */
    @SaCheckPermission("system:model:export")
    @Log(title = "模型管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatModelBo bo, HttpServletResponse response) {
        List<ChatModelVo> list = chatModelService.queryList(bo);
        ExcelUtil.exportExcel(list, "模型管理", ChatModelVo.class, response);
    }

    /**
     * 获取模型管理详细信息
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
     * 新增模型管理
     */
    @SaCheckPermission("system:model:add")
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatModelBo bo) {
        return toAjax(chatModelService.insertByBo(bo));
    }

    /**
     * 修改模型管理
     */
    @SaCheckPermission("system:model:edit")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatModelBo bo) {
        return toAjax(chatModelService.updateByBo(bo));
    }

    /**
     * 删除模型管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:model:remove")
    @Log(title = "模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatModelService.deleteWithValidByIds(List.of(ids), true));
    }
}
