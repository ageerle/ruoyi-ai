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

package tech.ordinaryroad.live.chat.client.commons.client.enums;

/**
 * @author mjz
 * @date 2023/8/26
 */
public enum ClientStatusEnums {
    /**
     * 新创建
     */
    NEW(0),

    /**
     * 已初始化
     */
    INITIALIZED(1),

    /**
     * 连接中
     */
    CONNECTING(100),

    /**
     * 重新连接中
     */
    RECONNECTING(101),

    /**
     * 已连接
     */
    CONNECTED(200),

    /**
     * 连接失败
     */
    CONNECT_FAILED(401),

    /**
     * 已断开连接
     */
    DISCONNECTED(400),

    /**
     * 已销毁
     */
    DESTROYED(-1),
    ;

    public int getCode() {
        return code;
    }

    ClientStatusEnums(int order) {
        this.code = order;
    }

    private final int code;

}
