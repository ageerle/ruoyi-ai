package org.ruoyi.system.listener;


import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.ruoyi.common.chat.config.LocalCache;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.utils.TikTokensUtil;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.vo.SysModelVo;
import org.ruoyi.system.service.ISysModelService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Objects;

/**
 * 描述：OpenAIEventSourceListener
 *
 * @author https:www.unfbx.com
 * @date 2023-02-22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SSEEventSourceListener extends EventSourceListener {

    private ResponseBodyEmitter emitter;

    private StringBuilder stringBuffer = new StringBuilder();

    @Autowired(required = false)
    public SSEEventSourceListener(ResponseBodyEmitter emitter) {
        this.emitter = emitter;
    }
    private static final ISysModelService sysModelService = SpringUtils.getBean(ISysModelService.class);
    private String modelName;
    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("OpenAI建立sse连接...");
    }

    /**
     * {@inheritDoc}
     */
    @SneakyThrows
    @Override
    public void onEvent(@NotNull EventSource eventSource, String id, String type, String data) {
        try {
            if ("[DONE]".equals(data)) {
                //成功响应
                emitter.complete();
                if(StringUtils.isNotEmpty(modelName)){
                    IChatCostService IChatCostService = SpringUtils.context().getBean(IChatCostService.class);
                    IChatMessageService chatMessageService = SpringUtils.context().getBean(IChatMessageService.class);
                    ChatMessageBo chatMessageBo = new ChatMessageBo();
                    chatMessageBo.setModelName(modelName);
                    chatMessageBo.setContent(stringBuffer.toString());
                    Long userId = (Long)LocalCache.CACHE.get("userId");
                    if(userId == null){
                        return;
                    }
                    chatMessageBo.setUserId(userId);
                    //查询按次数扣费的模型
                    SysModelBo sysModelBo = new SysModelBo();
                    sysModelBo.setModelType("2");
                    sysModelBo.setModelName(modelName);
                    List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
                    if (CollectionUtil.isNotEmpty(sysModelList)){
                        chatMessageBo.setDeductCost(0d);
                        chatMessageBo.setRemark("提问时扣费");
                        // 保存消息记录
                        chatMessageService.insertByBo(chatMessageBo);
                    }else{
                        int tokens = TikTokensUtil.tokens(modelName,stringBuffer.toString());
                        chatMessageBo.setTotalTokens(tokens);
                        // 按token扣费并且保存消息记录
                        IChatCostService.deductToken(chatMessageBo);
                    }
                }
                return;
            }
            // 解析返回内容
            ObjectMapper mapper = new ObjectMapper();
            ChatCompletionResponse completionResponse = mapper.readValue(data, ChatCompletionResponse.class);
            if(completionResponse == null || CollectionUtil.isEmpty(completionResponse.getChoices())){
                return;
            }
            Object content = completionResponse.getChoices().get(0).getDelta().getContent();
            if(content == null){
                content = completionResponse.getChoices().get(0).getDelta().getReasoningContent();
                if(content == null) return;
            }
            if(StringUtils.isEmpty(modelName)){
                modelName = completionResponse.getModel();
            }
            stringBuffer.append(content);
            emitter.send(data);
        } catch (Exception e) {
            log.error("sse信息推送失败{}内容：{}",e.getMessage(),data);
            eventSource.cancel();
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.info("OpenAI关闭sse连接...");
    }

    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (Objects.isNull(response)) {
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", body.string(), t);
        } else {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }

}
