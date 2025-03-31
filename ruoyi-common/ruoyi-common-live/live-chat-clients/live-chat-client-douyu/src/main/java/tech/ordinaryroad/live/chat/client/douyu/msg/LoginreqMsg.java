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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.util.OrLocalDateTimeUtil;
import tech.ordinaryroad.live.chat.client.douyu.api.DouyuApis;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;

import java.util.Arrays;
import java.util.List;

/**
 * @author mjz
 * @date 2023/8/27
 */
@Getter
@Setter
@RequiredArgsConstructor
public class LoginreqMsg extends BaseDouyuCmdMsg {

    public static List<String> SHOULD_IGNORE_NEW_LOGIN_PROPERTIES = Arrays.asList("roomid", "dfl", "username", "uid", "ver", "aver", "ct", "type");
    public static List<String> SHOULD_IGNORE_OLD_LOGIN_PROPERTIES = Arrays.asList("type", "roomid", "dfl", "username", "password", "ltkid", "biz", "stk", "devid", "ct", "pt", "cvr", "tvr", "apd", "rt", "vk", "ver", "aver", "dmbt", "dmbv");

    private long roomid;
    private String dfl;
    private String username;
    private long uid;
    private String ver;
    private String aver;
    private int ct = 0;

    private String password;
    /**
     * Cookie中的acf_ltkid
     */
    private String ltkid;
    private String biz;
    /**
     * Cookie中的acf_stk
     */
    private String stk;
    /**
     * Cookie中的dy_did
     */
    private String devid;
    private String pt = "2";
    private String cvr = "0";
    private String tvr = "7";
    private String apd = "";
    private long rt = OrLocalDateTimeUtil.zonedCurrentTimeSecs();
    private String vk;
    private String dmbt = "chrome";
    private String dmbv = "116";

    public LoginreqMsg(long roomid, String dfl, String username, long uid, String ver, String aver) {
        this.roomid = roomid;
        this.dfl = dfl;
        this.username = username;
        this.uid = uid;
        this.ver = ver;
        this.aver = aver;
    }

    public LoginreqMsg(long roomid, String dfl, String username, String ver, String aver, String ltkid, String biz, String stk, String devid) {
        this.roomid = roomid;
        this.dfl = dfl;
        this.username = username;
        this.ver = ver;
        this.aver = aver;
        this.ltkid = ltkid;
        this.biz = biz;
        this.stk = stk;
        this.devid = devid;
        this.vk = DouyuApis.generateVk(devid);
    }

    @Override
    public String getType() {
        return DouyuCmdEnum.loginreq.name();
    }
}
