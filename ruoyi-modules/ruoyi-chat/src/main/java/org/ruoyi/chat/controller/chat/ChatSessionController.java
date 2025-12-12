package org.ruoyi.chat.controller.chat;

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
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatSessionBo;
import org.ruoyi.domain.vo.ChatSessionVo;
import org.ruoyi.service.IChatSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/session")
public class ChatSessionController extends BaseController {

    private final IChatSessionService chatSessionService;

    /**
     * 查询会话管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<ChatSessionVo> list(ChatSessionBo bo, PageQuery pageQuery) {
        if (!LoginHelper.isLogin()) {
            // 如果用户没有登录,返回空会话列表
            return TableDataInfo.build();
        }
        // 默认查询当前用户会话
        bo.setUserId(LoginHelper.getUserId());
        return chatSessionService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出会话管理列表
     */
    @Log(title = "会话管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatSessionBo bo, HttpServletResponse response) {
        List<ChatSessionVo> list = chatSessionService.queryList(bo);
        ExcelUtil.exportExcel(list, "会话管理", ChatSessionVo.class, response);
    }

    /**
     * 获取会话管理详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<ChatSessionVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable Long id) {
        return R.ok(chatSessionService.queryById(id));
    }

    /**
     * 新增会话管理
     */
    @Log(title = "会话管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ChatSessionBo bo) {
        chatSessionService.insertByBo(bo);
        // 返回会话id
        return R.ok(bo.getId());
    }

    /**
     * 修改会话管理
     */
    @Log(title = "会话管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatSessionBo bo) {
        return toAjax(chatSessionService.updateByBo(bo));
    }

    /**
     * 删除会话管理
     *
     * @param ids 主键串
     */
    @Log(title = "会话管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatSessionService.deleteWithValidByIds(List.of(ids), true));
    }
}
