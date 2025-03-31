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

package tech.ordinaryroad.live.chat.client.douyu.client.base;

import cn.hutool.core.collection.CollUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.douyu.config.DouyuLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuClientModeEnum;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuConnectionListener;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuMsgListener;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.IDouyuMsg;
import tech.ordinaryroad.live.chat.client.douyu.netty.frame.factory.DouyuWebSocketFrameFactory;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuConnectionHandler;
import tech.ordinaryroad.live.chat.client.servers.netty.client.base.BaseNettyClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author mjz
 * @date 2023/9/15
 */
@Slf4j
public abstract class BaseDouyuLiveChatClient extends BaseNettyClient<
        DouyuLiveChatClientConfig,
        DouyuCmdEnum,
        IDouyuMsg,
        IDouyuMsgListener,
        DouyuConnectionHandler,
        DouyuBinaryFrameHandler
        > {

    private final DouyuClientModeEnum mode;

    public BaseDouyuLiveChatClient(DouyuClientModeEnum mode, DouyuLiveChatClientConfig config, List<IDouyuMsgListener> msgListeners, IDouyuConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        this.mode = mode;
        addMsgListeners(msgListeners);

        // 初始化
        this.init();
    }

    public BaseDouyuLiveChatClient(DouyuClientModeEnum mode, DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener, IDouyuConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        this.mode = mode;
        addMsgListener(msgListener);

        // 初始化
        this.init();
    }

    public BaseDouyuLiveChatClient(DouyuClientModeEnum mode, DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener, IDouyuConnectionListener connectionListener) {
        this(mode, config, msgListener, connectionListener, new NioEventLoopGroup());
    }

    public BaseDouyuLiveChatClient(DouyuClientModeEnum mode, DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener) {
        this(mode, config, msgListener, null, new NioEventLoopGroup());
    }

    public BaseDouyuLiveChatClient(DouyuClientModeEnum mode, DouyuLiveChatClientConfig config) {
        this(mode, config, CollUtil.newArrayList(), null, new NioEventLoopGroup());
    }

    @Override
    public void sendDanmu(Object danmu, Runnable success, Consumer<Throwable> failed) {
        if (!checkCanSendDanmu()) {
            return;
        }
        if (mode == DouyuClientModeEnum.WS && danmu instanceof String) {
            String msg = (String) danmu;
            if (log.isDebugEnabled()) {
                log.debug("{} douyu发送弹幕 {}", getConfig().getRoomId(), danmu);
            }

            WebSocketFrame webSocketFrame = null;
            try {
                webSocketFrame = getWebSocketFrameFactory(getConfig().getRoomId()).createDanmu(msg, getConfig().getCookie());
            } catch (Exception e) {
                log.error("douyu弹幕包创建失败", e);
                if (failed != null) {
                    failed.accept(e);
                }
            }
            if (webSocketFrame == null) {
                return;
            }

            send(webSocketFrame, () -> {
                if (log.isDebugEnabled()) {
                    log.debug("douyu弹幕发送成功 {}", danmu);
                }
                if (success != null) {
                    success.run();
                }
                finishSendDanmu();
            }, throwable -> {
                log.error("douyu弹幕发送失败", throwable);
                if (failed != null) {
                    failed.accept(throwable);
                }
            });
        } else {
            super.sendDanmu(danmu);
        }
    }

    protected static DouyuWebSocketFrameFactory getWebSocketFrameFactory(long roomId) {
        return DouyuWebSocketFrameFactory.getInstance(roomId);
    }

}
