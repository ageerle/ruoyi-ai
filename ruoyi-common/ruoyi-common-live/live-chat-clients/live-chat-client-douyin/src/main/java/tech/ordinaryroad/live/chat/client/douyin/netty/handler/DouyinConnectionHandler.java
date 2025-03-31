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

package tech.ordinaryroad.live.chat.client.douyin.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.douyin.client.DouyinLiveChatClient;
import tech.ordinaryroad.live.chat.client.douyin.config.DouyinLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.servers.netty.client.handler.BaseNettyClientConnectionHandler;

/**
 * @author mjz
 * @date 2024/1/2
 */
@Slf4j
@ChannelHandler.Sharable
public class DouyinConnectionHandler extends BaseNettyClientConnectionHandler<DouyinLiveChatClient, DouyinConnectionHandler> {

    /**
     * 以ClientConfig为主
     */
    private final Object roomId;
    /**
     * 以ClientConfig为主
     */
    private String cookie;

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, DouyinLiveChatClient client, IBaseConnectionListener<DouyinConnectionHandler> listener) {
        super(handshaker, client, listener);
        this.roomId = client.getConfig().getRoomId();
        this.cookie = client.getConfig().getCookie();
    }

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, DouyinLiveChatClient client) {
        this(handshaker, client, null);
    }

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, IBaseConnectionListener<DouyinConnectionHandler> listener, String cookie) {
        super(handshaker, listener);
        this.roomId = roomId;
        this.cookie = cookie;
    }

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, IBaseConnectionListener<DouyinConnectionHandler> listener) {
        this(handshaker, roomId, listener, null);
    }

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, String cookie) {
        this(handshaker, roomId, null, cookie);
    }

    public DouyinConnectionHandler(WebSocketClientHandshaker handshaker, long roomId) {
        this(handshaker, roomId, null, null);
    }

    @Override
    protected void sendHeartbeat(ChannelHandlerContext ctx) {
        // ignore
    }

    @Override
    public void sendAuthRequest(Channel channel) {
        // ignore
    }

    @Override
    protected long getHeartbeatPeriod() {
        if (client == null) {
            return DouyinLiveChatClientConfig.DEFAULT_HEARTBEAT_PERIOD;
        } else {
            return client.getConfig().getHeartbeatPeriod();
        }
    }

    @Override
    protected long getHeartbeatInitialDelay() {
        if (client == null) {
            return DouyinLiveChatClientConfig.DEFAULT_HEARTBEAT_INITIAL_DELAY;
        } else {
            return client.getConfig().getHeartbeatInitialDelay();
        }
    }

    public Object getRoomId() {
        return client != null ? client.getConfig().getRoomId() : roomId;
    }

    private String getCookie() {
        return client != null ? client.getConfig().getCookie() : cookie;
    }
}
