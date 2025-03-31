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

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mjz
 * @date 2024/1/31
 */
@Data
@NoArgsConstructor
public class BilibiliLikeReportV3Request {
    /**
     * 本次点赞次数
     */
    private int click_time = 1;
    /**
     * 房间真实ID
     */
    private long room_id;
    /**
     * Cookie中的DedeUserID
     */
    private String uid;
    /**
     * RoomInitResult中的uid
     */
    private long anchor_id;
    /**
     * Cookie中的bili_jct
     */
    private String csrf;
    /**
     * Cookie中的bili_jct
     */
    private String csrf_token;
    /**
     * 暂时留空
     */
    private String visit_id = StrUtil.EMPTY;

    public BilibiliLikeReportV3Request(long room_id, String uid, long anchor_id, String csrf, String csrf_token) {
        this.room_id = room_id;
        this.uid = uid;
        this.anchor_id = anchor_id;
        this.csrf = csrf;
        this.csrf_token = csrf_token;
    }
}
