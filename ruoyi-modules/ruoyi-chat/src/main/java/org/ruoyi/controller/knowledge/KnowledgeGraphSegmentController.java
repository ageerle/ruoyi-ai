package org.ruoyi.controller.knowledge;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphSegmentBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphSegmentVo;
import org.ruoyi.service.knowledge.IKnowledgeGraphSegmentService;
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
 * 知识图谱片段
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/graphSegment")
public class KnowledgeGraphSegmentController extends BaseController {

    private final IKnowledgeGraphSegmentService knowledgeGraphSegmentService;

    /**
     * 查询知识图谱片段列表
     */
    @SaCheckPermission("system:graphSegment:list")
    @GetMapping("/list")
    public TableDataInfo<KnowledgeGraphSegmentVo> list(KnowledgeGraphSegmentBo bo, PageQuery pageQuery) {
        return knowledgeGraphSegmentService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识图谱片段列表
     */
    @SaCheckPermission("system:graphSegment:export")
    @Log(title = "知识图谱片段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeGraphSegmentBo bo, HttpServletResponse response) {
        List<KnowledgeGraphSegmentVo> list = knowledgeGraphSegmentService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识图谱片段", KnowledgeGraphSegmentVo.class, response);
    }

    /**
     * 获取知识图谱片段详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:graphSegment:query")
    @GetMapping("/{id}")
    public R<KnowledgeGraphSegmentVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(knowledgeGraphSegmentService.queryById(id));
    }

    /**
     * 新增知识图谱片段
     */
    @SaCheckPermission("system:graphSegment:add")
    @Log(title = "知识图谱片段", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeGraphSegmentBo bo) {
        return toAjax(knowledgeGraphSegmentService.insertByBo(bo));
    }

    /**
     * 修改知识图谱片段
     */
    @SaCheckPermission("system:graphSegment:edit")
    @Log(title = "知识图谱片段", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeGraphSegmentBo bo) {
        return toAjax(knowledgeGraphSegmentService.updateByBo(bo));
    }

    /**
     * 删除知识图谱片段
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:graphSegment:remove")
    @Log(title = "知识图谱片段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeGraphSegmentService.deleteWithValidByIds(List.of(ids), true));
    }
}
