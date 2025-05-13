package org.ruoyi.chat.service.chat.impl;

import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.config.Consts;
import com.coze.openapi.service.service.CozeAPI;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 扣子聊天管理
 *
 * @author ageer
 */
@Service
@Slf4j
public class CozeServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());

        TokenAuth authCli = new TokenAuth(chatModelVo.getApiKey());
        CozeAPI coze =
                new CozeAPI.Builder()
                        .baseURL(chatModelVo.getApiHost())
                        .auth(authCli)
                        .readTimeout(10000)
                        .build();
        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(chatModelVo.getModelName())
                        .userID(chatRequest.getUserId().toString())
                        .messages(Collections.singletonList(Message.buildUserQuestionText("What can you do?")))
                        .build();

        Flowable<ChatEvent> resp = coze.chat().stream(req);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.submit(() -> {
            resp.blockingForEach(
                    event -> {
                        if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                            emitter.send(event.getMessage().getContent());
                            log.info("coze: {}", event.getMessage().getContent());
                        }
                        if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                            emitter.complete();
                            log.info("Token usage: {}", event.getChat().getUsage().getTokenCount());
                        }
                    }
            );
            coze.shutdownExecutor();
        });


        return emitter;
    }

    @Override
    public String getCategory() {
        return ChatModeType.COZE.getCode();
    }
}
