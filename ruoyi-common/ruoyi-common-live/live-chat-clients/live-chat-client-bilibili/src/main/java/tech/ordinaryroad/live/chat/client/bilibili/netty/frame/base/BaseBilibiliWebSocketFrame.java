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

package tech.ordinaryroad.live.chat.client.bilibili.netty.frame.base;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import tech.ordinaryroad.live.chat.client.bilibili.constant.OperationEnum;
import tech.ordinaryroad.live.chat.client.bilibili.constant.ProtoverEnum;

/**
 * 实现Bilibili协议的BinaryWebSocketFrame
 * <a href="https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/live/message_stream.md#数据包格式">数据包格式</a>
 *
 * @author mjz
 * @date 2023/1/5
 */
public abstract class BaseBilibiliWebSocketFrame extends BinaryWebSocketFrame {

    public static int sequence = 0;

    public ProtoverEnum getProtoverEnum() {
        return ProtoverEnum.getByCode(super.content().getShort(6));
    }

    public OperationEnum getOperationEnum() {
        return OperationEnum.getByCode(super.content().getInt(8));
    }

    public BaseBilibiliWebSocketFrame(ByteBuf byteBuf) {
        super(byteBuf);
    }
}
