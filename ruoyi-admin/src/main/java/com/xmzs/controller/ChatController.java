package com.xmzs.controller;


import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.domain.request.MjTaskRequest;
import com.xmzs.common.chat.entity.Tts.TextToSpeech;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.chat.entity.whisper.WhisperResponse;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.domain.vo.ChatMessageVo;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.ISseService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import retrofit2.Response;

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

    private final ISseService ISseService;

    private final IChatMessageService chatMessageService;

    /**
     * 聊天接口
     */
    @PostMapping("/chat")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest, HttpServletResponse response) {
        return ISseService.sseChat(chatRequest);
    }

    /**
     * 语音转文本
     *
     * @param file
     */
    @PostMapping("/audio")
    @ResponseBody
    public WhisperResponse audio(@RequestParam("file") MultipartFile file) {
        WhisperResponse whisperResponse = ISseService.speechToTextTranscriptionsV2(file);
        return whisperResponse;
    }

    /**
     * 文本转语音
     *
     * @param textToSpeech
     */
    @PostMapping("/speech")
    @ResponseBody
    public ResponseEntity<Resource> speech(@RequestBody TextToSpeech textToSpeech) {
        return ISseService.textToSpeed(textToSpeech);
    }


    @PostMapping("/dall3")
    @ResponseBody
    public R<List<Item>> dall3(@RequestBody @Valid Dall3Request request) {
        return R.ok(ISseService.dall3(request));
    }

    /**
     * 扣除mj绘图费用
     *
     * @return
     */
    @PostMapping("/mjTask")
    @ResponseBody
    public R<String> mjTask(@RequestBody MjTaskRequest mjTaskRequest) {
        ISseService.mjTask(mjTaskRequest);
        return R.ok();
    }

    /**
     * 聊天记录
     */
    @PostMapping("/chatList")
    @ResponseBody
    public R<TableDataInfo<ChatMessageVo>> list(@RequestBody @Valid ChatMessageBo chatRequest, @RequestBody PageQuery pageQuery) {
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
