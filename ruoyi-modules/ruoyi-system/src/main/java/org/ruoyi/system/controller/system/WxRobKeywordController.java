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
import org.ruoyi.system.domain.bo.WxRobKeywordBo;
import org.ruoyi.system.domain.vo.WxRobKeywordVo;
import org.ruoyi.system.service.IWxRobKeywordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【请填写功能名称】
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/robKeyword")
public class WxRobKeywordController extends BaseController {

    private final IWxRobKeywordService wxRobKeywordService;

    /**
     * 查询【请填写功能名称】列表
     */
    @SaCheckPermission("system:robKeyword:list")
    @GetMapping("/list")
    public TableDataInfo<WxRobKeywordVo> list(WxRobKeywordBo bo, PageQuery pageQuery) {
        return wxRobKeywordService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @SaCheckPermission("system:robKeyword:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WxRobKeywordBo bo, HttpServletResponse response) {
        List<WxRobKeywordVo> list = wxRobKeywordService.queryList(bo);
        ExcelUtil.exportExcel(list, "【请填写功能名称】", WxRobKeywordVo.class, response);
    }

    /**
     * 获取【请填写功能名称】详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:robKeyword:query")
    @GetMapping("/{id}")
    public R<WxRobKeywordVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(wxRobKeywordService.queryById(id));
    }

    /**
     * 新增【请填写功能名称】
     */
    @SaCheckPermission("system:robKeyword:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WxRobKeywordBo bo) {
        return toAjax(wxRobKeywordService.insertByBo(bo));
    }

    /**
     * 修改【请填写功能名称】
     */
    @SaCheckPermission("system:robKeyword:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WxRobKeywordBo bo) {
        return toAjax(wxRobKeywordService.updateByBo(bo));
    }

    /**
     * 删除【请填写功能名称】
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:robKeyword:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(wxRobKeywordService.deleteWithValidByIds(List.of(ids), true));
    }
}
