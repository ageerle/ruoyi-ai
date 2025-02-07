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
import org.ruoyi.system.domain.bo.ChatGptsBo;
import org.ruoyi.system.domain.vo.ChatGptsVo;
import org.ruoyi.system.service.IChatGptsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * gpts管理
 *
 * @author Lion Li
 * @date 2024-07-09
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/gpts")
public class ChatGptsController extends BaseController {

    private final IChatGptsService chatGptsService;

    /**
     * 查询gpts管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<ChatGptsVo> list(ChatGptsBo bo, PageQuery pageQuery) {
        return chatGptsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出gpts管理列表
     */
    @SaCheckPermission("system:gpts:export")
    @Log(title = "gpts管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatGptsBo bo, HttpServletResponse response) {
        List<ChatGptsVo> list = chatGptsService.queryList(bo);
        ExcelUtil.exportExcel(list, "gpts管理", ChatGptsVo.class, response);
    }

    /**
     * 获取gpts管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:gpts:query")
    @GetMapping("/{id}")
    public R<ChatGptsVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatGptsService.queryById(id));
    }

    /**
     * 新增gpts管理
     */
    @SaCheckPermission("system:gpts:add")
    @Log(title = "gpts管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatGptsBo bo) {
        return toAjax(chatGptsService.insertByBo(bo));
    }

    /**
     * 修改gpts管理
     */
    @SaCheckPermission("system:gpts:edit")
    @Log(title = "gpts管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatGptsBo bo) {
        return toAjax(chatGptsService.updateByBo(bo));
    }

    /**
     * 删除gpts管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:gpts:remove")
    @Log(title = "gpts管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatGptsService.deleteWithValidByIds(List.of(ids), true));
    }
}
