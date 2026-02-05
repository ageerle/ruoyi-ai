package org.ruoyi.controller.knowledge;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphInstanceBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphInstanceVo;
import org.ruoyi.service.knowledge.IKnowledgeGraphInstanceService;
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
 * 知识图谱实例
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/graphInstance")
public class KnowledgeGraphInstanceController extends BaseController {

    private final IKnowledgeGraphInstanceService knowledgeGraphInstanceService;

    /**
     * 查询知识图谱实例列表
     */
    @SaCheckPermission("system:graphInstance:list")
    @GetMapping("/list")
    public TableDataInfo<KnowledgeGraphInstanceVo> list(KnowledgeGraphInstanceBo bo, PageQuery pageQuery) {
        return knowledgeGraphInstanceService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识图谱实例列表
     */
    @SaCheckPermission("system:graphInstance:export")
    @Log(title = "知识图谱实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeGraphInstanceBo bo, HttpServletResponse response) {
        List<KnowledgeGraphInstanceVo> list = knowledgeGraphInstanceService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识图谱实例", KnowledgeGraphInstanceVo.class, response);
    }

    /**
     * 获取知识图谱实例详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:graphInstance:query")
    @GetMapping("/{id}")
    public R<KnowledgeGraphInstanceVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(knowledgeGraphInstanceService.queryById(id));
    }

    /**
     * 新增知识图谱实例
     */
    @SaCheckPermission("system:graphInstance:add")
    @Log(title = "知识图谱实例", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeGraphInstanceBo bo) {
        return toAjax(knowledgeGraphInstanceService.insertByBo(bo));
    }

    /**
     * 修改知识图谱实例
     */
    @SaCheckPermission("system:graphInstance:edit")
    @Log(title = "知识图谱实例", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeGraphInstanceBo bo) {
        return toAjax(knowledgeGraphInstanceService.updateByBo(bo));
    }

    /**
     * 删除知识图谱实例
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:graphInstance:remove")
    @Log(title = "知识图谱实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeGraphInstanceService.deleteWithValidByIds(List.of(ids), true));
    }
}
