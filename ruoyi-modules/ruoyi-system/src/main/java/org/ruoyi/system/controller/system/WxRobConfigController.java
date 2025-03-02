package org.ruoyi.system.controller.system;

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
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.wechat.web.utils.UUIDShortUtil;
import org.ruoyi.system.domain.bo.WxRobConfigBo;
import org.ruoyi.system.domain.vo.WxRobConfigVo;
import org.ruoyi.system.service.IWxRobConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机器人
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/robConfig")
public class WxRobConfigController extends BaseController {

    private final IWxRobConfigService wxRobConfigService;

    /**
     * 查询机器人列表
     */
    @GetMapping("/list")
    public TableDataInfo<WxRobConfigVo> list(WxRobConfigBo bo, PageQuery pageQuery) {
        return wxRobConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询当前用户绑定的机器人信息
     * @param bo 查询参数
     * @return 机器人信息
     */
    @GetMapping("/getRobConfig")
    public R<List<WxRobConfigVo>> botList(WxRobConfigBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        return R.ok(wxRobConfigService.queryList(bo));
    }

    /**
     * 导出机器人列表
     */
    @Log(title = "机器人", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WxRobConfigBo bo, HttpServletResponse response) {
        List<WxRobConfigVo> list = wxRobConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "机器人", WxRobConfigVo.class, response);
    }

    /**
     * 获取机器人详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<WxRobConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(wxRobConfigService.queryById(id));
    }

    /**
     * 新增机器人
     */
    @Log(title = "机器人", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WxRobConfigBo bo) {
        String uniKey = UUIDShortUtil.generateShortUuid();
        bo.setUniqueKey(uniKey);
        return toAjax(wxRobConfigService.insertByBo(bo));
    }

    /**
     * 修改机器人
     */
    @Log(title = "机器人", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WxRobConfigBo bo) {
        return toAjax(wxRobConfigService.updateByBo(bo));
    }

    /**
     * 删除机器人
     *
     * @param ids 主键串
     */
    @Log(title = "删除机器人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(wxRobConfigService.deleteWithValidByIds(List.of(ids), true));
    }
}
