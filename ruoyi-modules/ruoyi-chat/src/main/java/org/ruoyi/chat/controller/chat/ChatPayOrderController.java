package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatPayOrderBo;
import org.ruoyi.domain.vo.ChatPayOrderVo;
import org.ruoyi.service.IChatPayOrderService;
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
 * 支付订单
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/payOrder")
public class ChatPayOrderController extends BaseController {

    private final IChatPayOrderService chatPayOrderService;

    /**
     * 查询支付订单列表
     */
    @SaCheckPermission("system:payOrder:list")
    @GetMapping("/list")
    public TableDataInfo<ChatPayOrderVo> list(ChatPayOrderBo bo, PageQuery pageQuery) {
        return chatPayOrderService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出支付订单列表
     */
    @SaCheckPermission("system:payOrder:export")
    @Log(title = "支付订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatPayOrderBo bo, HttpServletResponse response) {
        List<ChatPayOrderVo> list = chatPayOrderService.queryList(bo);
        ExcelUtil.exportExcel(list, "支付订单", ChatPayOrderVo.class, response);
    }

    /**
     * 获取支付订单详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:payOrder:query")
    @GetMapping("/{id}")
    public R<ChatPayOrderVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatPayOrderService.queryById(id));
    }

    /**
     * 新增支付订单
     */
    @SaCheckPermission("system:payOrder:add")
    @Log(title = "支付订单", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatPayOrderBo bo) {
        return toAjax(chatPayOrderService.insertByBo(bo));
    }

    /**
     * 修改支付订单
     */
    @SaCheckPermission("system:payOrder:edit")
    @Log(title = "支付订单", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatPayOrderBo bo) {
        return toAjax(chatPayOrderService.updateByBo(bo));
    }

    /**
     * 删除支付订单
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:payOrder:remove")
    @Log(title = "支付订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatPayOrderService.deleteWithValidByIds(List.of(ids), true));
    }
}
