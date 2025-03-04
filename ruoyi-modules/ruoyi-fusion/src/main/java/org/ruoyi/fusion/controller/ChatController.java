package org.ruoyi.fusion.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.domain.request.Dall3Request;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.images.Item;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.knowledge.service.EmbeddingService;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.request.translation.TranslationRequest;
import org.ruoyi.system.domain.vo.ChatMessageVo;
import org.ruoyi.system.service.IChatMessageService;
import org.ruoyi.system.service.ISseService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 描述：聊天管理
 *
 * @author ageerle@163.com
 * @date 2023-03-01
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ISseService ISseService;

    private final IChatMessageService chatMessageService;

    private final EmbeddingService embeddingService;
    /**
     * 聊天接口
     */
    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest, HttpServletRequest request) {
        if (chatRequest.getModel().startsWith("ollama")) {
            return ISseService.ollamaChat(chatRequest);
        }
        return ISseService.sseChat(chatRequest,request);
    }


    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @ResponseBody
    public UploadFileResponse upload(@RequestPart("file") MultipartFile file) {
        return ISseService.upload(file);
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

    /**
     * 文本翻译
     *
     * @param
     */
    @PostMapping("/translation")
    @ResponseBody
    public String translation(@RequestBody TranslationRequest translationRequest) {
        return ISseService.translation(translationRequest);
    }

    @PostMapping("/dall3")
    @ResponseBody
    public R<List<Item>> dall3(@RequestBody @Valid Dall3Request request) {
        return R.ok(ISseService.dall3(request));
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
