package org.ruoyi.chat.controller.knowledge;

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
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeRoleBo;
import org.ruoyi.domain.vo.KnowledgeRoleVo;
import org.ruoyi.service.IKnowledgeRoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库角色
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/knowledgeRole")
public class KnowledgeRoleController extends BaseController {

    private final IKnowledgeRoleService knowledgeRoleService;

    /**
     * 查询知识库角色列表
     */
    @GetMapping("/list")
    public TableDataInfo<KnowledgeRoleVo> list(KnowledgeRoleBo bo, PageQuery pageQuery) {
        return knowledgeRoleService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识库角色列表
     */
    @Log(title = "知识库角色", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeRoleBo bo, HttpServletResponse response) {
        List<KnowledgeRoleVo> list = knowledgeRoleService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库角色", KnowledgeRoleVo.class, response);
    }

    /**
     * 获取知识库角色详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<KnowledgeRoleVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable Long id) {
        return R.ok(knowledgeRoleService.queryById(id));
    }

    /**
     * 新增知识库角色
     */
    @Log(title = "知识库角色", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeRoleBo bo) {
        return toAjax(knowledgeRoleService.insertByBo(bo));
    }

    /**
     * 修改知识库角色
     */
    @Log(title = "知识库角色", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeRoleBo bo) {
        return toAjax(knowledgeRoleService.updateByBo(bo));
    }

    /**
     * 删除知识库角色
     *
     * @param ids 主键串
     */
    @Log(title = "知识库角色", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeRoleService.deleteWithValidByIds(List.of(ids), true));
    }
}
