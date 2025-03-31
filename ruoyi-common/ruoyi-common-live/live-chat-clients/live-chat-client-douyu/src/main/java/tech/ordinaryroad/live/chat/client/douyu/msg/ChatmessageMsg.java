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

package tech.ordinaryroad.live.chat.client.douyu.msg;

import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.util.OrLocalDateTimeUtil;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;

/**
 * @author mjz
 * @date 2023/9/7
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatmessageMsg extends BaseDouyuCmdMsg {

    private String pe = "0";
    /**
     * 弹幕内容
     */
    private String content;
    private String col = "0";
    /**
     * Cookie中的dy_did
     */
    private String dy;
    /**
     * Cookie中的acf_uid
     */
    private String sender;
    private String ifs = "0";
    private String nc = "0";
    private String dat = "0";
    private String rev = "0";
    /**
     * 当前时间戳（秒）
     */
    private String tts;
    private String admzq = "0";
    /**
     * tts往后加几秒的时间戳（毫秒）
     */
    private String cst;

    public ChatmessageMsg(String content, String dy, String sender) {
        this.content = content;
        this.dy = dy;
        this.sender = sender;
        this.tts = String.valueOf(OrLocalDateTimeUtil.zonedCurrentTimeSecs());
        this.cst = String.valueOf(OrLocalDateTimeUtil.zonedCurrentTimeMillis() + RandomUtil.randomLong(8000, 10000));
    }

    @Override
    public String getType() {
        return DouyuCmdEnum.chatmessage.name();
    }
}
