package org.ruoyi.controller.knowledge;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.bo.knowledge.KnowledgeInfoBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
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
 * 知识库
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/info")
public class KnowledgeInfoController extends BaseController {

    private final IKnowledgeInfoService knowledgeInfoService;

    /**
     * 查询知识库列表
     */
    @SaCheckPermission("system:info:list")
    @GetMapping("/list")
    public TableDataInfo<KnowledgeInfoVo> list(KnowledgeInfoBo bo, PageQuery pageQuery) {
        return knowledgeInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识库列表
     */
    @SaCheckPermission("system:info:export")
    @Log(title = "知识库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeInfoBo bo, HttpServletResponse response) {
        List<KnowledgeInfoVo> list = knowledgeInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库", KnowledgeInfoVo.class, response);
    }

    /**
     * 获取知识库详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:info:query")
    @GetMapping("/{id}")
    public R<KnowledgeInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(knowledgeInfoService.queryById(id));
    }

    /**
     * 新增知识库
     */
    @SaCheckPermission("system:info:add")
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KnowledgeInfoBo bo) {
            bo.setUserId(LoginHelper.getUserId());
        return toAjax(knowledgeInfoService.insertByBo(bo));
    }

    /**
     * 修改知识库
     */
    @SaCheckPermission("system:info:edit")
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KnowledgeInfoBo bo) {
        return toAjax(knowledgeInfoService.updateByBo(bo));
    }

    /**
     * 删除知识库
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:info:remove")
    @Log(title = "知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(knowledgeInfoService.deleteWithValidByIds(List.of(ids), true));
    }
}
