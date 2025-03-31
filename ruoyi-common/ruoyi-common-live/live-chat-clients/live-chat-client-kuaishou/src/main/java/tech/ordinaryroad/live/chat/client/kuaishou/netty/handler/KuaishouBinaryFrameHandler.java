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

package tech.ordinaryroad.live.chat.client.kuaishou.netty.handler;

import cn.hutool.core.util.ZipUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.client.KuaishouLiveChatClient;
import tech.ordinaryroad.live.chat.client.kuaishou.listener.IKuaishouMsgListener;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouDanmuMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouGiftMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouLikeMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.base.IKuaishouMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.*;
import tech.ordinaryroad.live.chat.client.servers.netty.client.handler.BaseNettyClientBinaryFrameHandler;

import java.util.Collections;
import java.util.List;

/**
 * @author mjz
 * @date 2024/1/5
 */
@Slf4j
@ChannelHandler.Sharable
public class KuaishouBinaryFrameHandler extends BaseNettyClientBinaryFrameHandler<KuaishouLiveChatClient, KuaishouBinaryFrameHandler, PayloadTypeOuterClass.PayloadType, IKuaishouMsg, IKuaishouMsgListener> {

    public KuaishouBinaryFrameHandler(List<IKuaishouMsgListener> iKuaishouMsgListeners, KuaishouLiveChatClient client) {
        super(iKuaishouMsgListeners, client);
    }

    public KuaishouBinaryFrameHandler(List<IKuaishouMsgListener> iKuaishouMsgListeners, long roomId) {
        super(iKuaishouMsgListeners, roomId);
    }

    @SneakyThrows
    @Override
    public void onCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
        if (super.msgListeners.isEmpty()) {
            return;
        }

        SocketMessageOuterClass.SocketMessage socketMessage = (SocketMessageOuterClass.SocketMessage) cmdMsg;
        ByteString payloadByteString = socketMessage.getPayload();
        switch (socketMessage.getPayloadType()) {
            case SC_FEED_PUSH: {
                SCWebFeedPushOuterClass.SCWebFeedPush scWebFeedPush = SCWebFeedPushOuterClass.SCWebFeedPush.parseFrom(payloadByteString);
                if (scWebFeedPush.getCommentFeedsCount() > 0) {
                    for (WebCommentFeedOuterClass.WebCommentFeed webCommentFeed : scWebFeedPush.getCommentFeedsList()) {
                        iteratorMsgListeners(msgListener -> msgListener.onDanmuMsg(KuaishouBinaryFrameHandler.this, new KuaishouDanmuMsg(webCommentFeed)));
                    }
                }
                if (scWebFeedPush.getGiftFeedsCount() > 0) {
                    for (WebGiftFeedOuterClass.WebGiftFeed webGiftFeed : scWebFeedPush.getGiftFeedsList()) {
                        iteratorMsgListeners(msgListener -> msgListener.onGiftMsg(KuaishouBinaryFrameHandler.this, new KuaishouGiftMsg(webGiftFeed)));
                    }
                }
                if (scWebFeedPush.getLikeFeedsCount() > 0) {
                    for (WebLikeFeedOuterClass.WebLikeFeed webLikeFeed : scWebFeedPush.getLikeFeedsList()) {
                        iteratorMsgListeners(msgListener -> msgListener.onLikeMsg(KuaishouBinaryFrameHandler.this, new KuaishouLikeMsg(webLikeFeed)));
                    }
                }
                break;
            }
            default: {
                iteratorMsgListeners(msgListener -> msgListener.onOtherCmdMsg(KuaishouBinaryFrameHandler.this, cmd, socketMessage));
            }
        }
    }

    @Override
    protected List<IKuaishouMsg> decode(ByteBuf byteBuf) {
        try {
            SocketMessageOuterClass.SocketMessage socketMessage = SocketMessageOuterClass.SocketMessage.parseFrom(byteBuf.nioBuffer());
            SocketMessageOuterClass.SocketMessage.CompressionType compressionType = socketMessage.getCompressionType();
            ByteString payloadByteString = socketMessage.getPayload();
            byte[] payload;
            switch (compressionType) {
                case NONE: {
                    payload = payloadByteString.toByteArray();
                    break;
                }
                case GZIP: {
                    payload = ZipUtil.unGzip(payloadByteString.newInput());
                    break;
                }
                default: {
                    if (log.isWarnEnabled()) {
                        log.warn("暂不支持的压缩方式 " + compressionType);
                    }
                    return Collections.emptyList();
                }
            }
            return Collections.singletonList(socketMessage.toBuilder().setPayload(ByteString.copyFrom(payload)).build());
        } catch (InvalidProtocolBufferException e) {
            throw new BaseException(e);
        }
    }
}
