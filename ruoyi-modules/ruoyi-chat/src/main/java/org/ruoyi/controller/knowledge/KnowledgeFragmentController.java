package org.ruoyi.controller.knowledge;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.domain.bo.knowledge.KnowledgeFragmentBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.service.knowledge.IKnowledgeFragmentService;
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
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 知识片段
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/fragment")
public class KnowledgeFragmentController extends BaseController {

    private final IKnowledgeFragmentService knowledgeFragmentService;

    /**
     * 查询知识片段列表
     */
    @SaCheckPermission("system:fragment:list")
    @GetMapping("/list")
    public TableDataInfo<KnowledgeFragmentVo> list(KnowledgeFragmentBo bo, PageQuery pageQuery) {
        return knowledgeFragmentService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识片段列表
     */
    @SaCheckPermission("system:fragment:export")
    @Log(title = "知识片段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeFragmentBo bo, HttpServletResponse response) {
        List<KnowledgeFragmentVo> list = knowledgeFragmentService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识片段", KnowledgeFragmentVo.class, response);
    }

    /**
     * 获取知识片段详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:fragment:query")
    @GetMapping("/{id}")
    public R<KnowledgeFragmentVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(knowledgeFragmentService.queryById(id));
    }

    /**
     * 新增知识片段
     */
    @SaCheckPermission("system:fragment:add")
    @Log(title = "知识片段", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeFragmentBo bo) {
        return toAjax(knowledgeFragmentService.insertByBo(bo));
    }

    /**
     * 修改知识片段
     */
    @SaCheckPermission("system:fragment:edit")
    @Log(title = "知识片段", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeFragmentBo bo) {
        return toAjax(knowledgeFragmentService.updateByBo(bo));
    }

    /**
     * 删除知识片段
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:fragment:remove")
    @Log(title = "知识片段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeFragmentService.deleteWithValidByIds(List.of(ids), true));
    }
}
