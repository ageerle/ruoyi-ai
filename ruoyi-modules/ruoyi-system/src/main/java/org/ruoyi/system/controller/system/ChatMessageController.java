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
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.vo.ChatMessageVo;
import org.ruoyi.system.service.IChatMessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天消息
 *
 * @author Lion Li
 * @date 2024-04-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/message")
public class ChatMessageController extends BaseController {

    private final IChatMessageService chatMessageService;

    /**
     * 查询聊天消息列表
     */
    @SaCheckPermission("system:message:list")
    @GetMapping("/list")
    public TableDataInfo<ChatMessageVo> list(ChatMessageBo bo, PageQuery pageQuery) {
        pageQuery.setOrderByColumn("createTime");
        pageQuery.setIsAsc("desc");
        return chatMessageService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询我的聊天消息列表
     */
    @GetMapping("/listByUser")
    public R<TableDataInfo<ChatMessageVo>> listByUser(ChatMessageBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        pageQuery.setOrderByColumn("createTime");
        pageQuery.setIsAsc("desc");
        return R.ok(chatMessageService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出聊天消息列表
     */
    @SaCheckPermission("system:message:export")
    @Log(title = "聊天消息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatMessageBo bo, HttpServletResponse response) {
        List<ChatMessageVo> list = chatMessageService.queryList(bo);
        ExcelUtil.exportExcel(list, "聊天消息", ChatMessageVo.class, response);
    }

    /**
     * 获取聊天消息详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:message:query")
    @GetMapping("/{id}")
    public R<ChatMessageVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatMessageService.queryById(id));
    }

    /**
     * 新增聊天消息
     */
    @SaCheckPermission("system:message:add")
    @Log(title = "聊天消息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatMessageBo bo) {
        return toAjax(chatMessageService.insertByBo(bo));
    }

    /**
     * 修改聊天消息
     */
    @SaCheckPermission("system:message:edit")
    @Log(title = "聊天消息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatMessageBo bo) {
        return toAjax(chatMessageService.updateByBo(bo));
    }

    /**
     * 删除聊天消息
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:message:remove")
    @Log(title = "聊天消息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatMessageService.deleteWithValidByIds(List.of(ids), true));
    }
}
