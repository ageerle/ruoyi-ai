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

package tech.ordinaryroad.live.chat.client.bilibili.msg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.bilibili.constant.OperationEnum;
import tech.ordinaryroad.live.chat.client.bilibili.constant.ProtoverEnum;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.BaseBilibiliMsg;

/**
 * @author mjz
 * @date 2023/1/6
 */
@Getter
@Setter
@RequiredArgsConstructor
public class AuthMsg extends BaseBilibiliMsg {

    /**
     * 用户uid，0代表游客
     */
    private long uid;

    /**
     * 房间id room_id，不是短id short_id
     * 可以通过将url参数id改为直播地址中的数字来查询房间真实id
     * example: <a href="https://api.live.bilibili.com/room/v1/Room/room_init?id=6">https://api.live.bilibili.com/room/v1/Room/room_init?id=6</a>
     */
    private final long roomid;

    /**
     * 协议版本
     *
     * @see ProtoverEnum#getCode()
     */
    private final int protover;

    /**
     * 平台标识
     */
    private String platform = "web";
    private int type = 2;

    /**
     * 必须字段
     *
     * @since 2023-08-19
     */
    private final String buvid;

    /**
     * 认证秘钥（必须字段）
     *
     * @since @since 2023-08-19
     */
    private final String key;

    @Override
    public ProtoverEnum getProtoverEnum() {
        return ProtoverEnum.getByCode(this.protover);
    }

    @Override
    public OperationEnum getOperationEnum() {
        return OperationEnum.AUTH;
    }

}
