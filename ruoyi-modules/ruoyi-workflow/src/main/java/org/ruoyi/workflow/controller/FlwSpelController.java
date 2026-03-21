package org.ruoyi.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.bo.FlowSpelBo;
import org.ruoyi.workflow.domain.vo.FlowSpelVo;
import org.ruoyi.workflow.service.IFlwSpelService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程spel达式定义
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/spel")
public class FlwSpelController extends BaseController {

    private final IFlwSpelService flwSpelService;

    /**
     * 查询流程spel达式定义列表
     */
    @SaCheckPermission("workflow:spel:list")
    @GetMapping("/list")
    public TableDataInfo<FlowSpelVo> list(FlowSpelBo bo, PageQuery pageQuery) {
        return flwSpelService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取流程spel达式定义详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("workflow:spel:query")
    @GetMapping("/{id}")
    public R<FlowSpelVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(flwSpelService.queryById(id));
    }

    /**
     * 新增流程spel达式定义
     */
    @SaCheckPermission("workflow:spel:add")
    @Log(title = "流程spel达式定义", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FlowSpelBo bo) {
        return toAjax(flwSpelService.insertByBo(bo));
    }

    /**
     * 修改流程spel达式定义
     */
    @SaCheckPermission("workflow:spel:edit")
    @Log(title = "流程spel达式定义", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FlowSpelBo bo) {
        return toAjax(flwSpelService.updateByBo(bo));
    }

    /**
     * 删除流程spel达式定义
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workflow:spel:remove")
    @Log(title = "流程spel达式定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(flwSpelService.deleteWithValidByIds(List.of(ids), true));
    }
}
