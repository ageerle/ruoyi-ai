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

package tech.ordinaryroad.live.chat.client.douyu.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.douyu.api.DouyuApis;
import tech.ordinaryroad.live.chat.client.douyu.client.base.BaseDouyuLiveChatClient;
import tech.ordinaryroad.live.chat.client.douyu.config.DouyuLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuClientModeEnum;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuConnectionListener;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuMsgListener;
import tech.ordinaryroad.live.chat.client.douyu.msg.LoginresMsg;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuConnectionHandler;

import java.util.List;

/**
 * 直播间弹幕客户端{@link DouyuClientModeEnum#WS}
 *
 * @author mjz
 * @date 2023/8/20
 */
@Slf4j
public class DouyuWsLiveChatClient extends BaseDouyuLiveChatClient implements IDouyuMsgListener {

    public DouyuWsLiveChatClient(DouyuLiveChatClientConfig config, List<IDouyuMsgListener> msgListeners, IDouyuConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(DouyuClientModeEnum.WS, config, msgListeners, connectionListener, workerGroup);
        addMsgListener(DouyuWsLiveChatClient.this);
    }

    public DouyuWsLiveChatClient(DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener, IDouyuConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(DouyuClientModeEnum.WS, config, msgListener, connectionListener, workerGroup);
        addMsgListener(DouyuWsLiveChatClient.this);
    }

    public DouyuWsLiveChatClient(DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener, IDouyuConnectionListener connectionListener) {
        this(config, msgListener, connectionListener, new NioEventLoopGroup());
    }

    public DouyuWsLiveChatClient(DouyuLiveChatClientConfig config, IDouyuMsgListener msgListener) {
        this(config, msgListener, null);
    }

    public DouyuWsLiveChatClient(DouyuLiveChatClientConfig config) {
        this(config, null);
    }

    @Override
    protected String getWebSocketUriString() {
        return DouyuApis.getRandomWssUri(getConfig().getRoomId());
    }

    @Override
    public DouyuConnectionHandler initConnectionHandler(IBaseConnectionListener<DouyuConnectionHandler> clientConnectionListener) {
        return new DouyuConnectionHandler(DouyuClientModeEnum.WS,
                WebSocketClientHandshakerFactory.newHandshaker(getWebsocketUri(), WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), getConfig().getMaxFramePayloadLength()),
                DouyuWsLiveChatClient.this, clientConnectionListener
        );
    }

    @Override
    public DouyuBinaryFrameHandler initBinaryFrameHandler() {
        return new DouyuBinaryFrameHandler(msgListeners, DouyuWsLiveChatClient.this);
    }

    @Override
    public void onMsg(DouyuBinaryFrameHandler binaryFrameHandler, IMsg msg) {
        if (msg instanceof LoginresMsg) {
            send(getWebSocketFrameFactory(getConfig().getRoomId()).createKeeplive(getConfig().getCookie()));
        }
    }

    @Override
    public boolean removeMsgListener(IDouyuMsgListener msgListener) {
        if (msgListener == DouyuWsLiveChatClient.this) {
            return false;
        }
        return super.removeMsgListener(msgListener);
    }

    @Override
    public void removeAllMsgListeners() {
        super.removeAllMsgListeners();
        addMsgListener(DouyuWsLiveChatClient.this);
    }
}
