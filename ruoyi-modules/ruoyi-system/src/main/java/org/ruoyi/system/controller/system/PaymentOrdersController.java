package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.PaymentOrdersBo;
import org.ruoyi.system.domain.vo.PaymentOrdersVo;
import org.ruoyi.system.service.IPaymentOrdersService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付订单
 *
 * @author Lion Li
 * @date 2024-04-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/payOrder")
public class PaymentOrdersController extends BaseController {

    private final IPaymentOrdersService paymentOrdersService;

    /**
     * 查询支付订单列表
     */
    @SaCheckPermission("system:order:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentOrdersVo> list(PaymentOrdersBo bo, PageQuery pageQuery) {
        pageQuery.setOrderByColumn("createTime");
        pageQuery.setIsAsc("desc");
        return paymentOrdersService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出支付订单列表
     */
    @SaCheckPermission("system:order:export")
    @Log(title = "支付订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PaymentOrdersBo bo, HttpServletResponse response) {
        List<PaymentOrdersVo> list = paymentOrdersService.queryList(bo);
        ExcelUtil.exportExcel(list, "支付订单", PaymentOrdersVo.class, response);
    }

    /**
     * 获取支付订单详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:order:query")
    @GetMapping("/{id}")
    public R<PaymentOrdersVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(paymentOrdersService.queryById(id));
    }

    /**
     * 新增支付订单
     */
    @SaCheckPermission("system:order:add")
    @Log(title = "支付订单", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PaymentOrdersBo bo) {
        return toAjax(paymentOrdersService.insertByBo(bo));
    }

    /**
     * 修改支付订单
     */
    @SaCheckPermission("system:order:edit")
    @Log(title = "支付订单", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PaymentOrdersBo bo) {
        return toAjax(paymentOrdersService.updateByBo(bo));
    }

    /**
     * 删除支付订单
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:order:remove")
    @Log(title = "支付订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(paymentOrdersService.deleteWithValidByIds(List.of(ids), true));
    }
}
