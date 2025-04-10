package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatVoucherBo;
import org.ruoyi.domain.vo.ChatVoucherVo;
import org.ruoyi.service.IChatVoucherService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.excel.utils.ExcelUtil;
/**
 * 用户兑换记录
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/voucher")
public class ChatVoucherController extends BaseController {

    private final IChatVoucherService chatVoucherService;

    /**
     * 查询用户兑换记录列表
     */
    @SaCheckPermission("system:voucher:list")
    @GetMapping("/list")
    public TableDataInfo<ChatVoucherVo> list(ChatVoucherBo bo, PageQuery pageQuery) {
        return chatVoucherService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出用户兑换记录列表
     */
    @SaCheckPermission("system:voucher:export")
    @Log(title = "用户兑换记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatVoucherBo bo, HttpServletResponse response) {
        List<ChatVoucherVo> list = chatVoucherService.queryList(bo);
        ExcelUtil.exportExcel(list, "用户兑换记录", ChatVoucherVo.class, response);
    }

    /**
     * 获取用户兑换记录详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:voucher:query")
    @GetMapping("/{id}")
    public R<ChatVoucherVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatVoucherService.queryById(id));
    }

    /**
     * 新增用户兑换记录
     */
    @SaCheckPermission("system:voucher:add")
    @Log(title = "用户兑换记录", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatVoucherBo bo) {
        return toAjax(chatVoucherService.insertByBo(bo));
    }

    /**
     * 修改用户兑换记录
     */
    @SaCheckPermission("system:voucher:edit")
    @Log(title = "用户兑换记录", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatVoucherBo bo) {
        return toAjax(chatVoucherService.updateByBo(bo));
    }

    /**
     * 删除用户兑换记录
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:voucher:remove")
    @Log(title = "用户兑换记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatVoucherService.deleteWithValidByIds(List.of(ids), true));
    }
}
