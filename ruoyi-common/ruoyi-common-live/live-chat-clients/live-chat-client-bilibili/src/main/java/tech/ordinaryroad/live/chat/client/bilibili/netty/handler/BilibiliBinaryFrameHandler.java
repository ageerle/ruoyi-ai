/*
 * MIT License
 *
 * Copyright (c) 2023 OrdinaryRoad
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tech.ordinaryroad.live.chat.client.bilibili.netty.handler;

import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.bilibili.client.BilibiliLiveChatClient;
import tech.ordinaryroad.live.chat.client.bilibili.constant.BilibiliCmdEnum;
import tech.ordinaryroad.live.chat.client.bilibili.listener.IBilibiliMsgListener;
import tech.ordinaryroad.live.chat.client.bilibili.msg.*;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.BaseBilibiliMsg;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.IBilibiliMsg;
import tech.ordinaryroad.live.chat.client.bilibili.util.BilibiliCodecUtil;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.servers.netty.client.handler.BaseNettyClientBinaryFrameHandler;

import java.util.List;


/**
 * 消息处理器
 *
 * @author mjz
 * @date 2023/1/4
 */
@Slf4j
@ChannelHandler.Sharable
public class BilibiliBinaryFrameHandler extends BaseNettyClientBinaryFrameHandler<BilibiliLiveChatClient, BilibiliBinaryFrameHandler, BilibiliCmdEnum, IBilibiliMsg, IBilibiliMsgListener> {

    public BilibiliBinaryFrameHandler(List<IBilibiliMsgListener> msgListeners, BilibiliLiveChatClient client) {
        super(msgListeners, client);
    }

    public BilibiliBinaryFrameHandler(List<IBilibiliMsgListener> msgListeners, long roomId) {
        super(msgListeners, roomId);
    }

    @SneakyThrows
    @Override
    public void onCmdMsg(BilibiliCmdEnum cmd, ICmdMsg<BilibiliCmdEnum> cmdMsg) {
        if (super.msgListeners.isEmpty()) {
            return;
        }

        SendSmsReplyMsg sendSmsReplyMsg = (SendSmsReplyMsg) cmdMsg;
        switch (cmd) {
            case DANMU_MSG: {
                DanmuMsgMsg danmuMsgMsg = new DanmuMsgMsg();
                danmuMsgMsg.setProtover(sendSmsReplyMsg.getProtover());
                danmuMsgMsg.setInfo(sendSmsReplyMsg.getInfo());
                danmuMsgMsg.setDm_v2(StrUtil.toStringOrNull(sendSmsReplyMsg.getUnknownProperties().get("dm_v2")));
                iteratorMsgListeners(msgListener -> msgListener.onDanmuMsg(BilibiliBinaryFrameHandler.this, danmuMsgMsg));
                break;
            }

            case SEND_GIFT: {
                SendGiftMsg sendGiftMsg = new SendGiftMsg();
                sendGiftMsg.setRoomId(getRoomIdAsLong());
                sendGiftMsg.setProtover(sendSmsReplyMsg.getProtover());
                SendGiftMsg.Data data = BaseBilibiliMsg.OBJECT_MAPPER.treeToValue(sendSmsReplyMsg.getData(), SendGiftMsg.Data.class);
                sendGiftMsg.setData(data);
                iteratorMsgListeners(msgListener -> {
                    msgListener.onGiftMsg(BilibiliBinaryFrameHandler.this, sendGiftMsg);
                    msgListener.onSendGift(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg);
                });
                break;
            }

            case SUPER_CHAT_MESSAGE: {
                SuperChatMessageMsg superChatMessageMsg = new SuperChatMessageMsg();
                superChatMessageMsg.setProtover(sendSmsReplyMsg.getProtover());
                superChatMessageMsg.setRoomid(sendSmsReplyMsg.getRoomid());
                SuperChatMessageMsg.Data data = BaseBilibiliMsg.OBJECT_MAPPER.treeToValue(sendSmsReplyMsg.getData(), SuperChatMessageMsg.Data.class);
                superChatMessageMsg.setData(data);
                iteratorMsgListeners(msgListener -> msgListener.onSuperChatMsg(BilibiliBinaryFrameHandler.this, superChatMessageMsg));
                break;
            }

            case INTERACT_WORD: {
                InteractWordMsg interactWordMsg = new InteractWordMsg();
                interactWordMsg.setProtover(sendSmsReplyMsg.getProtover());
                InteractWordMsg.Data data = BaseBilibiliMsg.OBJECT_MAPPER.treeToValue(sendSmsReplyMsg.getData(), InteractWordMsg.Data.class);
                interactWordMsg.setData(data);
                iteratorMsgListeners(msgListener -> {
                    msgListener.onEnterRoomMsg(BilibiliBinaryFrameHandler.this, interactWordMsg);
                    msgListener.onEnterRoom(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg);
                });
                break;
            }

            case ENTRY_EFFECT: {
                iteratorMsgListeners(msgListener -> msgListener.onEntryEffect(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg));
                break;
            }

            case WATCHED_CHANGE: {
                iteratorMsgListeners(msgListener -> msgListener.onWatchedChange(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg));
                break;
            }

            case LIKE_INFO_V3_CLICK: {
                LikeInfoV3ClickMsg likeInfoV3ClickMsg = new LikeInfoV3ClickMsg();
                likeInfoV3ClickMsg.setProtover(sendSmsReplyMsg.getProtover());
                LikeInfoV3ClickMsg.Data data = BaseBilibiliMsg.OBJECT_MAPPER.treeToValue(sendSmsReplyMsg.getData(), LikeInfoV3ClickMsg.Data.class);
                likeInfoV3ClickMsg.setData(data);
                iteratorMsgListeners(msgListener -> {
                    msgListener.onLikeMsg(BilibiliBinaryFrameHandler.this, likeInfoV3ClickMsg);
                    msgListener.onClickLike(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg);
                });
                break;
            }

            case LIKE_INFO_V3_UPDATE: {
                iteratorMsgListeners(msgListener -> msgListener.onClickUpdate(BilibiliBinaryFrameHandler.this, sendSmsReplyMsg));
                break;
            }

            default: {
                iteratorMsgListeners(msgListener -> msgListener.onOtherCmdMsg(BilibiliBinaryFrameHandler.this, cmd, cmdMsg));
            }
        }
    }

    @Override
    protected List<IBilibiliMsg> decode(ByteBuf byteBuf) {
        return BilibiliCodecUtil.decode(byteBuf);
    }
}
