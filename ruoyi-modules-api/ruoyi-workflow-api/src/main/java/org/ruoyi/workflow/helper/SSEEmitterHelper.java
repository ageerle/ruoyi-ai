package org.ruoyi.workflow.helper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.cosntant.AdiConstant;
import org.ruoyi.workflow.cosntant.RedisKeyConstant;
import org.ruoyi.workflow.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SSEEmitterHelper {

    private static final Cache<SseEmitter, Boolean> COMPLETED_SSE = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).build();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static void parseAndSendPartialMsg(SseEmitter sseEmitter, String name, String content) {
        if (Boolean.TRUE.equals(COMPLETED_SSE.getIfPresent(sseEmitter))) {
            log.warn("sseEmitter already completed,name:{}", name);
            return;
        }
        String[] lines = content.split("[\\r\\n]", -1);
        if (lines.length > 1) {
            sendPartial(sseEmitter, name, " " + lines[0]);
            for (int i = 1; i < lines.length; i++) {
                sendPartial(sseEmitter, name, "-_wrap_-");
                sendPartial(sseEmitter, name, " " + lines[i]);
            }
        } else {
            sendPartial(sseEmitter, name, " " + content);
        }
    }

    public static void sendPartial(SseEmitter sseEmitter, String name, String msg) {
        if (Boolean.TRUE.equals(COMPLETED_SSE.getIfPresent(sseEmitter))) {
            log.warn("sseEmitter already completed,name:{}", name);
            return;
        }
        try {
            if (StringUtils.isNotBlank(name)) {
                sseEmitter.send(SseEmitter.event().name(name).data(msg));
            } else {
                sseEmitter.send(msg);
            }
        } catch (IOException ioException) {
            log.error("stream onNext error", ioException);
        }
    }


    public boolean checkOrComplete(User user, SseEmitter sseEmitter) {
        //Check: If still waiting response
        String askingKey = MessageFormat.format(RedisKeyConstant.USER_ASKING, user.getId());
        String askingVal = stringRedisTemplate.opsForValue().get(askingKey);
        if (StringUtils.isNotBlank(askingVal)) {
            sendErrorAndComplete(user.getId(), sseEmitter, "正在回复中...");
            return false;
        }
        return true;
    }


    public void startSse(User user, SseEmitter sseEmitter, String data) {

        String askingKey = MessageFormat.format(RedisKeyConstant.USER_ASKING, user.getId());
        stringRedisTemplate.opsForValue().set(askingKey, "1", 15, TimeUnit.SECONDS);

        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event().name(AdiConstant.SSEEventName.START);
            if (StringUtils.isNotBlank(data)) {
                builder.data(data);
            }
            sseEmitter.send(builder);
        } catch (IOException e) {
            log.error("startSse error", e);
            sseEmitter.completeWithError(e);
            COMPLETED_SSE.put(sseEmitter, Boolean.TRUE);
            stringRedisTemplate.delete(askingKey);
        }
    }

    public void sendComplete(long userId, SseEmitter sseEmitter, String msg) {
        if (Boolean.TRUE.equals(COMPLETED_SSE.getIfPresent(sseEmitter))) {
            log.warn("sseEmitter already completed,userId:{}", userId);
            delSseRequesting(userId);
            return;
        }
        try {
            sseEmitter.send(SseEmitter.event().name(AdiConstant.SSEEventName.DONE).data(msg));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            COMPLETED_SSE.put(sseEmitter, Boolean.TRUE);
            delSseRequesting(userId);
            sseEmitter.complete();
        }
    }


    public void sendErrorAndComplete(long userId, SseEmitter sseEmitter, String errorMsg) {
        if (Boolean.TRUE.equals(COMPLETED_SSE.getIfPresent(sseEmitter))) {
            log.warn("sseEmitter already completed,ignore error:{}", errorMsg);
            delSseRequesting(userId);
            return;
        }
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event();
            event.name(AdiConstant.SSEEventName.ERROR);
            event.data(Objects.toString(errorMsg, ""));
            sseEmitter.send(event);
        } catch (IOException e) {
            log.warn("sendErrorAndComplete userId:{},errorMsg:{}", userId, errorMsg);
            throw new RuntimeException(e);
        } finally {
            COMPLETED_SSE.put(sseEmitter, Boolean.TRUE);
            delSseRequesting(userId);
            sseEmitter.complete();
        }
    }

    private void delSseRequesting(long userId) {
        String askingKey = MessageFormat.format(RedisKeyConstant.USER_ASKING, userId);
        stringRedisTemplate.delete(askingKey);
    }
}
