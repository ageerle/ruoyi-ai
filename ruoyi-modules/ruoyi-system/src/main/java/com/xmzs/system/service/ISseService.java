package com.xmzs.system.service;


import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.domain.request.MjTaskRequest;
import com.xmzs.common.chat.entity.Tts.TextToSpeech;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.chat.entity.whisper.WhisperResponse;
import okhttp3.ResponseBody;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
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
    SseEmitter sseChat(ChatRequest chatRequest);

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
    String chat(ChatRequest chatRequest);

    /**
     * 绘画接口
     * @param request
     */
    List<Item> dall3(Dall3Request request);


    void mjTask(MjTaskRequest mjTaskRequest);

}
