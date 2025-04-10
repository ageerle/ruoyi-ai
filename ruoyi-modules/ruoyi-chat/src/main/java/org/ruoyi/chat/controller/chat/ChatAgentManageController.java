package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.ChatAgentManageBo;
import org.ruoyi.service.IChatAgentManageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.system.domain.vo.ChatAgentManageVo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 智能体管理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/agentManage")
public class ChatAgentManageController extends BaseController {

    private final IChatAgentManageService chatAgentManageService;

    /**
     * 查询智能体管理列表
     */
    @SaCheckPermission("system:agentManage:list")
    @GetMapping("/list")
    public TableDataInfo<ChatAgentManageVo> list(ChatAgentManageBo bo, PageQuery pageQuery) {
        return chatAgentManageService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出智能体管理列表
     */
    @SaCheckPermission("system:agentManage:export")
    @Log(title = "智能体管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatAgentManageBo bo, HttpServletResponse response) {
        List<ChatAgentManageVo> list = chatAgentManageService.queryList(bo);
        ExcelUtil.exportExcel(list, "智能体管理", ChatAgentManageVo.class, response);
    }

    /**
     * 获取智能体管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:agentManage:query")
    @GetMapping("/{id}")
    public R<ChatAgentManageVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatAgentManageService.queryById(id));
    }

    /**
     * 新增智能体管理
     */
    @SaCheckPermission("system:agentManage:add")
    @Log(title = "智能体管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatAgentManageBo bo) {
        return toAjax(chatAgentManageService.insertByBo(bo));
    }

    /**
     * 修改智能体管理
     */
    @SaCheckPermission("system:agentManage:edit")
    @Log(title = "智能体管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatAgentManageBo bo) {
        return toAjax(chatAgentManageService.updateByBo(bo));
    }

    /**
     * 删除智能体管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:agentManage:remove")
    @Log(title = "智能体管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatAgentManageService.deleteWithValidByIds(List.of(ids), true));
    }
}
