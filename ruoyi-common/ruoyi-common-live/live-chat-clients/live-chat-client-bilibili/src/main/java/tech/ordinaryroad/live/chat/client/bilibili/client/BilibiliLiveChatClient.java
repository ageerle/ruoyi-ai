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

package tech.ordinaryroad.live.chat.client.bilibili.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.bilibili.api.BilibiliApis;
import tech.ordinaryroad.live.chat.client.bilibili.config.BilibiliLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.bilibili.constant.BilibiliCmdEnum;
import tech.ordinaryroad.live.chat.client.bilibili.listener.IBilibiliConnectionListener;
import tech.ordinaryroad.live.chat.client.bilibili.listener.IBilibiliMsgListener;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.IBilibiliMsg;
import tech.ordinaryroad.live.chat.client.bilibili.netty.handler.BilibiliBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.bilibili.netty.handler.BilibiliConnectionHandler;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.servers.netty.client.base.BaseNettyClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * B站直播间弹幕客户端
 *
 * @author mjz
 * @date 2023/8/20
 */
@Slf4j
public class BilibiliLiveChatClient extends BaseNettyClient<
        BilibiliLiveChatClientConfig,
        BilibiliCmdEnum,
        IBilibiliMsg,
        IBilibiliMsgListener,
        BilibiliConnectionHandler,
        BilibiliBinaryFrameHandler
        > {

    private BilibiliApis.RoomInitResult roomInitResult = new BilibiliApis.RoomInitResult();

    public BilibiliLiveChatClient(BilibiliLiveChatClientConfig config, List<IBilibiliMsgListener> msgListeners, IBilibiliConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        addMsgListeners(msgListeners);

        // 初始化
        this.init();
    }

    public BilibiliLiveChatClient(BilibiliLiveChatClientConfig config, IBilibiliMsgListener msgListener, IBilibiliConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        addMsgListener(msgListener);

        // 初始化
        this.init();
    }

    public BilibiliLiveChatClient(BilibiliLiveChatClientConfig config, IBilibiliMsgListener msgListener, IBilibiliConnectionListener connectionListener) {
        this(config, msgListener, connectionListener, new NioEventLoopGroup());
    }

    public BilibiliLiveChatClient(BilibiliLiveChatClientConfig config, IBilibiliMsgListener msgListener) {
        this(config, msgListener, null, new NioEventLoopGroup());
    }

    public BilibiliLiveChatClient(BilibiliLiveChatClientConfig config) {
        this(config, null);
    }

    @Override
    public void init() {
        roomInitResult = BilibiliApis.roomInit(getConfig().getRoomId(), getConfig().getCookie());
        super.init();
    }

    @Override
    public BilibiliConnectionHandler initConnectionHandler(IBaseConnectionListener<BilibiliConnectionHandler> clientConnectionListener) {
        return new BilibiliConnectionHandler(
                WebSocketClientHandshakerFactory.newHandshaker(getWebsocketUri(), WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), getConfig().getMaxFramePayloadLength()),
                BilibiliLiveChatClient.this, clientConnectionListener
        );
    }

    @Override
    public BilibiliBinaryFrameHandler initBinaryFrameHandler() {
        return new BilibiliBinaryFrameHandler(super.msgListeners, BilibiliLiveChatClient.this);
    }

    @Override
    public void sendDanmu(Object danmu, Runnable success, Consumer<Throwable> failed) {
        if (!checkCanSendDanmu(false)) {
            return;
        }
        if (danmu instanceof String) {
            String msg = (String) danmu;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("{} bilibili发送弹幕 {}", getConfig().getRoomId(), danmu);
                }

                boolean sendSuccess = false;
                try {
                    BilibiliApis.sendMsg(msg, roomInitResult.getRoom_id(), getConfig().getCookie());
                    sendSuccess = true;
                } catch (Exception e) {
                    log.error("bilibili弹幕发送失败", e);
                    if (failed != null) {
                        failed.accept(e);
                    }
                }
                if (!sendSuccess) {
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("bilibili弹幕发送成功 {}", danmu);
                }
                if (success != null) {
                    success.run();
                }
                finishSendDanmu();
            } catch (Exception e) {
                log.error("bilibili弹幕发送失败", e);
                if (failed != null) {
                    failed.accept(e);
                }
            }
        } else {
            super.sendDanmu(danmu, success, failed);
        }
    }

    @Override
    public void clickLike(int count, Runnable success, Consumer<Throwable> failed) {
        if (count <= 0) {
            throw new BaseException("点赞次数必须大于0");
        }

        boolean successfullyClicked = false;
        try {
            BilibiliApis.likeReportV3(roomInitResult.getUid(), roomInitResult.getRoom_id(), getConfig().getCookie());
            successfullyClicked = true;
        } catch (Exception e) {
            log.error("Bilibili为直播间点赞失败", e);
            if (failed != null) {
                failed.accept(e);
            }
        }
        if (!successfullyClicked) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Bilibili为直播间点赞成功");
        }
        if (success != null) {
            success.run();
        }
    }
}
