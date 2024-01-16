package com.xmzs.controller;


import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.domain.vo.ChatMessageVo;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.SseService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-03-01
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SseService sseService;

    private final  IChatMessageService chatMessageService;

    /**
     * 聊天接口
     */
    @PostMapping("/chat")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest) {
        if("gpt-4-all".equals(chatRequest.getModel())
            || chatRequest.getModel().startsWith("gpt-4-gizmo")
            || chatRequest.getModel().startsWith("net-")
        ){
            return sseService.transitChat(chatRequest);
        }
        if("azure-gpt-3.5".equals(chatRequest.getModel())){
            return sseService.azureChat(chatRequest);
        }
        return sseService.sseChat(chatRequest);
    }

    @PostMapping("/dall3")
    @ResponseBody
    public R<List<Item>> dall3(@RequestBody @Valid Dall3Request request) {
       return R.ok(sseService.dall3(request));
    }

    @PostMapping("/mjTask")
    @ResponseBody
    public R<String> mjTask() {
        sseService.mjTask();
        return R.ok();
    }

    /**
     * 聊天记录
     */
    @PostMapping("/chatList")
    @ResponseBody
    public R<TableDataInfo<ChatMessageVo>> list(@RequestBody @Valid ChatMessageBo chatRequest,@RequestBody PageQuery pageQuery) {
        // 默认查询当前登录用户消息记录
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        chatRequest.setUserId(loginUser.getUserId());
        TableDataInfo<ChatMessageVo> chatMessageVoTableDataInfo = chatMessageService.queryPageList(chatRequest, pageQuery);
        return R.ok(chatMessageVoTableDataInfo);
    }

}
