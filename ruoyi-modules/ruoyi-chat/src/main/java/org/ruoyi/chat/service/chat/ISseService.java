package org.ruoyi.chat.service.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * 用户聊天管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface ISseService {

    /**
     * 客户端发送消息到服务端
     * @param chatRequest 请求对象
     */
    SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request);

    /**
     * 语音转文字
     * @param file 语音文件
     */
   WhisperResponse speechToTextTranscriptionsV2(MultipartFile file);

    /**
     * 文字转语音
     *
     * @param textToSpeech 文本信息
     * @return 流式语音
     */
    ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech);

    /**
     * 上传文件到服务器
     *
     * @param file 文件信息
     * @return 返回文件信息
     */
    UploadFileResponse upload(MultipartFile file);


}
