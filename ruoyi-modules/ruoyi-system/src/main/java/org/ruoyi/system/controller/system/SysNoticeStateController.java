package org.ruoyi.system.controller.system;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysNoticeStateBo;
import org.ruoyi.system.domain.vo.SysNoticeStateVo;
import org.ruoyi.system.service.ISysNoticeStateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户阅读状态
 *
 * @author Lion Li
 * @date 2024-05-11
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/noticeState")
public class SysNoticeStateController extends BaseController {

    private final ISysNoticeStateService sysNoticeStateService;

    /**
     * 查询用户阅读状态列表
     */
    @GetMapping("/list")
    public TableDataInfo<SysNoticeStateVo> list(SysNoticeStateBo bo, PageQuery pageQuery) {
        return sysNoticeStateService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出用户阅读状态列表
     */
    @Log(title = "用户阅读状态", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysNoticeStateBo bo, HttpServletResponse response) {
        List<SysNoticeStateVo> list = sysNoticeStateService.queryList(bo);
        ExcelUtil.exportExcel(list, "用户阅读状态", SysNoticeStateVo.class, response);
    }

    /**
     * 获取用户阅读状态详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SysNoticeStateVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(sysNoticeStateService.queryById(id));
    }

    /**
     * 新增用户阅读状态
     */
    @Log(title = "用户阅读状态", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysNoticeStateBo bo) {
        return toAjax(sysNoticeStateService.insertByBo(bo));
    }

    /**
     * 修改用户阅读状态
     */
    @PutMapping()
    public R<Void> edit(@RequestBody SysNoticeStateBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        return toAjax(sysNoticeStateService.updateByBo(bo));
    }

    /**
     * 删除用户阅读状态
     *
     * @param ids 主键串
     */
    @Log(title = "用户阅读状态", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysNoticeStateService.deleteWithValidByIds(List.of(ids), true));
    }
}
