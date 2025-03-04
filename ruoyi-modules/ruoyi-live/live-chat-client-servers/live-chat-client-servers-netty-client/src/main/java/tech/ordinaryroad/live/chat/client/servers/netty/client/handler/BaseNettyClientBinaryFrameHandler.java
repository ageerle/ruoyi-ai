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

package tech.ordinaryroad.live.chat.client.servers.netty.client.handler;

import lombok.Getter;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseMsgListener;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.servers.netty.client.base.BaseNettyClient;
import tech.ordinaryroad.live.chat.client.servers.netty.handler.base.BaseBinaryFrameHandler;

import java.util.List;

/**
 * BaseClientBinaryFrameHandler
 *
 * @author mjz
 * @date 2023/8/30
 */
public abstract class BaseNettyClientBinaryFrameHandler<
        Client extends BaseNettyClient<?, ?, ?, ?, ?, ?>,
        BinaryFrameHandler extends BaseBinaryFrameHandler<BinaryFrameHandler, CmdEnum, Msg, MsgListener>,
        CmdEnum extends Enum<CmdEnum>,
        Msg extends IMsg,
        MsgListener extends IBaseMsgListener<BinaryFrameHandler, CmdEnum>>
        extends BaseBinaryFrameHandler<BinaryFrameHandler, CmdEnum, Msg, MsgListener> {

    @Getter
    protected final Client client;

    public BaseNettyClientBinaryFrameHandler(List<MsgListener> msgListeners, Client client, long roomId) {
        super(msgListeners, roomId);
        this.client = client;
    }

    public BaseNettyClientBinaryFrameHandler(List<MsgListener> msgListeners, Client client) {
        super(msgListeners, client.getConfig().getRoomId());
        this.client = client;
    }

    public BaseNettyClientBinaryFrameHandler(List<MsgListener> msgListeners, long roomId) {
        super(msgListeners, roomId);
        this.client = null;
    }
}
