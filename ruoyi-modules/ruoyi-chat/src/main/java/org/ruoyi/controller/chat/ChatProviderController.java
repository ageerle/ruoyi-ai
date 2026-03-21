package org.ruoyi.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.service.chat.IChatProviderService;
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
import org.ruoyi.domain.vo.chat.ChatProviderVo;
import org.ruoyi.domain.bo.chat.ChatProviderBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 厂商管理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/provider")
public class ChatProviderController extends BaseController {

    private final IChatProviderService chatProviderService;

    /**
     * 查询厂商管理列表
     */
    @SaCheckPermission("system:provider:list")
    @GetMapping("/list")
    public TableDataInfo<ChatProviderVo> list(ChatProviderBo bo, PageQuery pageQuery) {
        return chatProviderService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出厂商管理列表
     */
    @SaCheckPermission("system:provider:export")
    @Log(title = "厂商管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatProviderBo bo, HttpServletResponse response) {
        List<ChatProviderVo> list = chatProviderService.queryList(bo);
        ExcelUtil.exportExcel(list, "厂商管理", ChatProviderVo.class, response);
    }

    /**
     * 获取厂商管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:provider:query")
    @GetMapping("/{id}")
    public R<ChatProviderVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatProviderService.queryById(id));
    }

    /**
     * 新增厂商管理
     */
    @SaCheckPermission("system:provider:add")
    @Log(title = "厂商管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatProviderBo bo) {
        return toAjax(chatProviderService.insertByBo(bo));
    }

    /**
     * 修改厂商管理
     */
    @SaCheckPermission("system:provider:edit")
    @Log(title = "厂商管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatProviderBo bo) {
        return toAjax(chatProviderService.updateByBo(bo));
    }

    /**
     * 删除厂商管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:provider:remove")
    @Log(title = "厂商管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatProviderService.deleteWithValidByIds(List.of(ids), true));
    }
}
