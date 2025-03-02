package org.ruoyi.system.service;


import jakarta.servlet.http.HttpServletRequest;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.domain.request.Dall3Request;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.images.Item;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.system.domain.request.translation.TranslationRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-04-08
 */
public interface ISseService {

    /**
     * 客户端发送消息到服务端
     * @param chatRequest
     */
    SseEmitter sseChat(ChatRequest chatRequest,HttpServletRequest request);

    /**
     * 语音转文字
     * @param file
     */
   WhisperResponse speechToTextTranscriptionsV2(MultipartFile file);

    /**
     * 文字转语音
     */
    ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech);

    /**
     * 客户端发送消息到服务端
     * @param chatRequest
     */
    String chat(ChatRequest chatRequest,String userId);

    /**
     * 客户端发送消息到服务端
     */
    List<Item> wxDall(String prompt,String userId);

    /**
     * 绘画接口
     * @param request
     */
    List<Item> dall3(Dall3Request request);


    UploadFileResponse upload(MultipartFile file);

    /**
     * 文本翻译
     * @param
     */
    String translation(TranslationRequest translationRequest);

    /**
     * 调用本地模型
     * @param chatRequest
     */
    SseEmitter ollamaChat(ChatRequest chatRequest);

    /**
     * 企业应用回复
     * @param prompt 提示词
     * @return 回复内容
     */
    String wxCpChat(String prompt);
}
