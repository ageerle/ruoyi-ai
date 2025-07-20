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
import org.ruoyi.domain.bo.KnowledgeRoleGroupBo;
import org.ruoyi.domain.vo.KnowledgeRoleGroupVo;
import org.ruoyi.service.IKnowledgeRoleGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库角色组
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/knowledgeRoleGroup")
public class KnowledgeRoleGroupController extends BaseController {

    private final IKnowledgeRoleGroupService knowledgeRoleGroupService;

    /**
     * 查询知识库角色组列表
     */
    @GetMapping("/list")
    public TableDataInfo<KnowledgeRoleGroupVo> list(KnowledgeRoleGroupBo bo, PageQuery pageQuery) {
        return knowledgeRoleGroupService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识库角色组列表
     */
    @Log(title = "知识库角色组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeRoleGroupBo bo, HttpServletResponse response) {
        List<KnowledgeRoleGroupVo> list = knowledgeRoleGroupService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库角色组", KnowledgeRoleGroupVo.class, response);
    }

    /**
     * 获取知识库角色组详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<KnowledgeRoleGroupVo> getInfo(@NotNull(message = "主键不能为空")
                                           @PathVariable Long id) {
        return R.ok(knowledgeRoleGroupService.queryById(id));
    }

    /**
     * 新增知识库角色组
     */
    @Log(title = "知识库角色组", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeRoleGroupBo bo) {
        return toAjax(knowledgeRoleGroupService.insertByBo(bo));
    }

    /**
     * 修改知识库角色组
     */
    @Log(title = "知识库角色组", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeRoleGroupBo bo) {
        return toAjax(knowledgeRoleGroupService.updateByBo(bo));
    }

    /**
     * 删除知识库角色组
     *
     * @param ids 主键串
     */
    @Log(title = "知识库角色组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeRoleGroupService.deleteWithValidByIds(List.of(ids), true));
    }
}
