package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.ChatConfigBo;
import org.ruoyi.system.domain.vo.ChatConfigVo;
import org.ruoyi.system.service.IChatConfigService;
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
 * 配置信息
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/chatConfig")
public class ChatConfigController extends BaseController {

    private final IChatConfigService chatConfigService;

    /**
     * 查询配置信息列表
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public TableDataInfo<ChatConfigVo> list(ChatConfigBo bo, PageQuery pageQuery) {
        return chatConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出配置信息列表
     */
    @SaCheckPermission("system:config:export")
    @Log(title = "配置信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatConfigBo bo, HttpServletResponse response) {
        List<ChatConfigVo> list = chatConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "配置信息", ChatConfigVo.class, response);
    }

    /**
     * 获取配置信息详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:config:query")
    @GetMapping("/{id}")
    public R<ChatConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatConfigService.queryById(id));
    }

    /**
     * 新增配置信息
     */
    @SaCheckPermission("system:config:add")
    @Log(title = "配置信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatConfigBo bo) {
        return toAjax(chatConfigService.insertByBo(bo));
    }

    /**
     * 修改配置信息
     */
    @SaCheckPermission("system:config:edit")
    @Log(title = "配置信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatConfigBo bo) {
        return toAjax(chatConfigService.updateByBo(bo));
    }

    /**
     * 删除配置信息
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:config:remove")
    @Log(title = "配置信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatConfigService.deleteWithValidByIds(List.of(ids), true));
    }
}
