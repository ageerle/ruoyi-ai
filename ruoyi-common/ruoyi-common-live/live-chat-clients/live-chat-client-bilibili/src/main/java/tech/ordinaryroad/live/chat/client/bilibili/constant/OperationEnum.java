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

package tech.ordinaryroad.live.chat.client.bilibili.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author mjz
 * @date 2023/1/5
 */
@Getter
@RequiredArgsConstructor
public enum OperationEnum {
    HANDSHAKE(0),
    HANDSHAKE_REPLY(1),
    /**
     * 心跳包
     */
    HEARTBEAT(2),
    /**
     * 心跳包回复（人气值）
     */
    HEARTBEAT_REPLY(3),
    SEND_MSG(4),

    /**
     * 普通包（命令）
     */
    SEND_SMS_REPLY(5),
    DISCONNECT_REPLY(6),

    /**
     * 认证包
     */
    AUTH(7),

    /**
     * 认证包回复
     */
    AUTH_REPLY(8),
    RAW(9),
    PROTO_READY(10),
    PROTO_FINISH(11),
    CHANGE_ROOM(12),
    CHANGE_ROOM_REPLY(13),
    REGISTER(14),
    REGISTER_REPLY(15),
    UNREGISTER(16),
    UNREGISTER_REPLY(17),
    ;

    private final int code;

    public static OperationEnum getByCode(int code) {
        for (OperationEnum value : OperationEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

}