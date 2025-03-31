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

package tech.ordinaryroad.live.chat.client.servers.netty.handler.base;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;

import java.util.concurrent.TimeUnit;


/**
 * 连接处理器
 *
 * @author mjz
 * @date 2023/8/21
 */
@Slf4j
public abstract class BaseConnectionHandler<ConnectionHandler extends BaseConnectionHandler<?>> extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final WebSocketClientHandshaker handshaker;
    @Getter
    private ChannelPromise handshakeFuture;
    private final IBaseConnectionListener<ConnectionHandler> listener;
    /**
     * 客户端发送心跳包
     */
    private ScheduledFuture<?> scheduledFuture = null;

    public BaseConnectionHandler(WebSocketClientHandshaker handshaker, IBaseConnectionListener<ConnectionHandler> listener) {
        this.handshaker = handshaker;
        this.listener = listener;
    }

    public BaseConnectionHandler(WebSocketClientHandshaker handshaker) {
        this(handshaker, null);
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.handshaker.handshake(ctx.channel());
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        // 判断是否正确握手
        if (this.handshaker.isHandshakeComplete()) {
            handshakeSuccessfully(ctx, msg);
        } else {
            try {
                handshakeSuccessfully(ctx, msg);
            } catch (WebSocketHandshakeException e) {
                handshakeFailed(msg, e);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("userEventTriggered {}", evt.getClass());
        }
        if (evt instanceof SslHandshakeCompletionEvent) {
            heartbeatCancel();
            heartbeatStart(ctx);
            if (this.listener != null) {
                listener.onConnected((ConnectionHandler) BaseConnectionHandler.this);
            }
        } else if (evt instanceof SslCloseCompletionEvent) {
            heartbeatCancel();
            if (this.listener != null) {
                listener.onDisconnected((ConnectionHandler) BaseConnectionHandler.this);
            }
        } else {
            log.error("待处理 {}", evt.getClass());
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 开始发送心跳包
     */
    private void heartbeatStart(ChannelHandlerContext ctx) {
        scheduledFuture = ctx.executor().scheduleAtFixedRate(() -> {
            sendHeartbeat(ctx);
        }, getHeartbeatInitialDelay(), getHeartbeatPeriod(), TimeUnit.SECONDS);
    }

    /**
     * 取消发送心跳包
     */
    private void heartbeatCancel() {
        if (null != scheduledFuture && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    protected abstract void sendHeartbeat(ChannelHandlerContext ctx);

    public abstract void sendAuthRequest(Channel channel);

    protected abstract long getHeartbeatPeriod();

    protected abstract long getHeartbeatInitialDelay();

    private void handshakeSuccessfully(ChannelHandlerContext ctx, FullHttpResponse msg) {
        if (log.isDebugEnabled()) {
            log.debug("握手完成!");
        }
        this.handshaker.finishHandshake(ctx.channel(), msg);
        this.handshakeFuture.setSuccess();
    }

    private void handshakeFailed(FullHttpResponse msg, WebSocketHandshakeException e) {
        log.error("握手失败！status:" + msg.status(), e);
        this.handshakeFuture.setFailure(e);
        if (listener != null) {
            this.listener.onConnectFailed((ConnectionHandler) this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
        if (!this.handshakeFuture.isDone()) {
            this.handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
