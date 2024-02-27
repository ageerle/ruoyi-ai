package com.xmzs.system.handler;

import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.entity.chat.ChatCompletion;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.wechat.api.MessageTools;
import com.xmzs.common.wechat.beans.BaseMsg;
import com.xmzs.common.wechat.core.Core;
import com.xmzs.common.wechat.face.IMsgHandlerFace;
import com.xmzs.system.cofing.KeywordConfig;
import com.xmzs.system.service.ISseService;

import java.util.List;

/**
 * 微信消息处理程序
 *
 * @author ashinnotfound
 * @date 2023/03/19
 */
public class WechatMessageHandler implements IMsgHandlerFace {
    private final ISseService sseService;
    private final KeywordConfig keywordConfig;

    public WechatMessageHandler(ISseService sseService, KeywordConfig keywordConfig) {
        this.sseService = sseService;
        this.keywordConfig = keywordConfig;
    }

    @Override
    public String textMsgHandle(BaseMsg baseMsg) {
        //如果是在群聊
        if (baseMsg.isGroupMsg()){
            //存在@机器人的消息就向ChatGPT提问
            if (baseMsg.getText().contains("@"+ Core.getInstance().getNickName())){
                //去除@再提问
                String prompt = baseMsg.getText().replace("@"+ Core.getInstance().getNickName() + " ", "").trim();
                return textResponse(baseMsg.getFromUserName(), prompt);
            }
        }else {
            ChatRequest chatBO = new ChatRequest();
            chatBO.setPrompt(baseMsg.getText());
            chatBO.setModel(ChatCompletion.Model.GPT_3_5_TURBO.getName());
            return sseService.chat(chatBO);
        }
        return null;
    }


    private String textResponse(String userName, String content) {
        if (keywordConfig.getReset().equals(content)){
            return "重置会话成功";
        }else {
            ChatRequest chatBO = new ChatRequest();
            chatBO.setPrompt(content);
            chatBO.setUserId(userName);
            if (content.startsWith(keywordConfig.getImage())) {
                Dall3Request dall3Request = new Dall3Request();
                dall3Request.setPrompt(content.replaceFirst(keywordConfig.getImage() + " ", ""));
                List<Item> items = sseService.dall3(dall3Request);
                MessageTools.sendPicMsgByUserId(userName, items.get(0).getUrl());
            } else {
                chatBO.setPrompt(content);
            }
        }
        return "这个问题我还没学会呢";
    }

    @Override
    public String picMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String voiceMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String viedoMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String nameCardMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public void sysMsgHandle(BaseMsg baseMsg) {
    }

    @Override
    public String verifyAddFriendMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String mediaMsgHandle(BaseMsg baseMsg) {
        return null;
    }
}
