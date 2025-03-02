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

package tech.ordinaryroad.live.chat.client.bilibili.api.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mjz
 * @date 2023/9/7
 */
@Data
@NoArgsConstructor
public class BilibiliSendMsgRequest {
    private String bubble = "0";
    /**
     * 弹幕内容
     */
    private String msg;
    /**
     * 弹幕颜色
     */
    private String color = "16777215";
    private String mode = "1";
    private String room_type = "0";
    private String jumpfrom = "0";
    /**
     * 字体大小
     */
    private String fontsize = "25";
    /**
     * 时间戳（秒）
     */
    private String rnd;
    /**
     * 房间真实ID
     */
    private long roomid;
    /**
     * Cookie中的bili_jct
     */
    private String csrf;
    /**
     * Cookie中的bili_jct
     */
    private String csrf_token;

    public BilibiliSendMsgRequest(String msg, String rnd, long roomid, String csrf, String csrf_token) {
        this.msg = msg;
        this.rnd = rnd;
        this.roomid = roomid;
        this.csrf = csrf;
        this.csrf_token = csrf_token;
    }
}
