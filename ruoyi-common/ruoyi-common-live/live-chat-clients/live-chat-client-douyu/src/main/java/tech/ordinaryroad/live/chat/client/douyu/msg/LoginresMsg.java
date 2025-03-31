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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;

/**
 * @author mjz
 * @date 2023/8/27
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginresMsg extends BaseDouyuCmdMsg {

    private long userid;
    private int roomgroup;
    private int pg;
    private long sessionid;
    private String username;
    private String nickname;
    private int live_stat;
    private boolean is_illegal;
    private String ill_ct;
    private String ill_ts;
    private String now;
    private String ps;
    private String es;
    private String it;
    private String its;
    private String npv;
    private String best_dlev;
    private String cur_lev;
    private String nrc;
    private String ih;
    private String sid;
    private String sahf;
    private String sceneid;
    private String newrg;
    private String regts;
    private String ip;
    private String rn;
    private String rct;

    @Override
    public String getType() {
        return DouyuCmdEnum.loginres.name();
    }
}
