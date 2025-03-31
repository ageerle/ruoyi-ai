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
public enum ProtoverEnum {
    /**
     * 普通包正文不使用压缩
     */
    NORMAL_NO_COMPRESSION(0),
    /**
     * 心跳及认证包正文不使用压缩
     */
    HEARTBEAT_AUTH_NO_COMPRESSION(1),
    /**
     * 普通包正文使用zlib压缩
     */
    NORMAL_ZLIB(2),
    /**
     * 普通包正文使用brotli压缩,解压为一个带头部的协议0普通包
     */
    NORMAL_BROTLI(3),
    ;

    private final int code;


    public static ProtoverEnum getByCode(int code) {
        for (ProtoverEnum value : ProtoverEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

}
