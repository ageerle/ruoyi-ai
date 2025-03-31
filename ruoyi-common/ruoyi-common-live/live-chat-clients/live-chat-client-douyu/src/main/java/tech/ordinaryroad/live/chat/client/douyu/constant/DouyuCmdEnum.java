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

package tech.ordinaryroad.live.chat.client.douyu.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tech.ordinaryroad.live.chat.client.douyu.msg.*;

/**
 * <a href='https://open.douyu.com/source/api/63'>文档</a>
 *
 * @author mjz
 * @date 2023/1/6
 */
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public enum DouyuCmdEnum {
    /**
     * 登录请求消息
     * <pre>{@code
     * type@=loginreq/roomid@=7750753/dfl@=/username@=visitor10424697/uid@=1168052601/ver@=20220825/aver@=218101901/ct@=0/
     * type@=loginreq/roomid@=58839/aid@=yihanTest/token@=4c8421535f9639d8c1ad35d1fa421f36/time@=1574850339/auth@=45619bb990e6b76db06a66d5a8a446d7/
     * }</pre>
     */
    loginreq(LoginreqMsg.class),
    /**
     * 登录响应消息
     * <pre>{@code
     * type@=loginres/userid@=1168052601/roomgroup@=0/pg@=0/sessionid@=0/username@=/nickname@=/live_stat@=0/is_illegal@=0/ill_ct@=/ill_ts@=0/now@=0/ps@=0/es@=0/it@=0/its@=0/npv@=0/best_dlev@=0/cur_lev@=0/nrc@=964715377/ih@=0/sid@=76494/sahf@=0/sceneid@=0/newrg@=0/regts@=0/ip@=112.43.93.229/rn@=0/rct@=0/
     * type@=loginresp/msg@=ok/rid@=77614265/
     * }</pre>
     */
    loginres(LoginresMsg.class),
    /**
     * 入组
     * type@=joingroup/rid@=59872/aid@=yourapplicaitonID/token@=4c8421535f9639d8c1ad35d1fa421f36/time@=1574850339/auth@=xxxxxxxxxxxx/
     * Auth 生成方式为 md5({secret}_{aid}_{time}_{token}), secret为aid对应的秘钥
     */
    joingroup(JoingroupMsg.class),
    /**
     * 客户端旧版心跳、心跳回复
     * <pre>{@code
     * type@=keeplive/tick@=1439802131/
     * }</pre>
     */
    keeplive(KeepliveMsg.class),
    /**
     * 客户端新版心跳、心跳回复
     * <pre>{@code
     * type@=mrkl/
     * }</pre>
     */
    mrkl,
    /**
     * 订阅贵族排行变动
     * type@=sub/mt@=online_vip_list/
     */
    sub(SubMsg.class),
    subres,
    noble_num_info,
    oni,
    postLogin,
    /**
     * 弹幕
     * type@=chatmsg/rid@=7750753/uid@=549977/nn@=一闪一闪亮品品/txt@=冷门歌手哎，声音独特哎/cid@=044c3bf3074b483275a44e0000000000/ic@=avatar@Sdefault@S08/level@=29/sahf@=0/nl@=7/cst@=1693107871294/bnn@=/bl@=0/brid@=0/hc@=/lk@=/dms@=8/pdg@=26/pdk@=86/ext@=/
     * type@=chatmsg/rid@=58839/ct@=8/hashid@=9LA18ePx4dqW/nn@=test/txt@=666/cid@=1111/ic@=icon/sahf@=0/level@=1/nl@=0/nc@=0/cmt@=0/gt@=0/col@=0/rg@=0/pg@=0/dlv@=0/dc@=0/bdlv@=0/gatin@=0/ chtin@=0/repin@=0/bnn@=test/bl@=0/brid@=58839/hc@=0/ol@=0/rev@=0/hl@=0/ifs@=0/p2p@=0/el@=eid@AA=1@ASetp@AA=1@ASsc@AA=1@AS/
     */
    chatmsg(ChatmsgMsg.class),
    pingreq,
    /**
     * 登出
     * type@=logout/
     */
    logout,
    /**
     * 登录响应
     * type@=loginresp/msg@=ok/rid@=77614265/
     */
    loginresp,
    /**
     * 礼物
     * type@=dgb/gfid@=1/gs@=59872/gfcnt@=1/hashid@=1/rid@=1/nn@=someone/level@=1/dw@=1/
     */
    dgb(DgbMsg.class),
    /**
     * 用户进房通知消息
     * type@=uenter/rid@=1/ uid@=1/nn@=someone/str@=1/level@=1/el@=eid@AA=1@ASetp@AA=1@ASsc@AA=1@AS@S/
     */
    uenter(UenterMsg.class),
    /**
     * 房间开关播提醒
     * type@=rss/rid@=1/ss@=1/code@=1/rt@=0/notify@=1/endtime@=1/
     */
    rss,
    /**
     * 超级弹幕消息
     * type@=ssd/rid@=1/trid@=1/content@=test/cli tp@=1/url@=test_url/jmptp@=1/
     */
    ssd,
    /**
     * 房间内礼物广播
     * type@=spbc/rid@=1/gfid@=1/sn@=name/dn@=name/gn@=1/gc@=1/gb@=1/es@=1/ eid@=1/
     */
    spbc,
    /**
     * 房间宝箱消息
     * type@=tsgs/rid@=1/gid@=1/gfid@=1/sn@=name/dn@=name/gn@=1/gc@=1/gb@=1/es@=1/gfid@=1/eid@=1/
     */
    tsgs,
    /**
     * 房间内 top10 变化消息
     * type@=rankup/uid@=1/rn@=3/rid@=1/rkt@=1/gid@=-9999/rt@=0/ nk@=test/sz@=3/drid@=1/bt@=1/
     */
    rankup,
    /**
     * 主播离开提醒
     * type@=al/rid@=10111/aid@=3044114/
     */
    al,
    /**
     * 主播回来继续直播提醒
     * type@=ab/rid@=10111/gid@=-9999/aid@=3044114/
     */
    ab,
    /**
     * 用户等级提升消息
     * type@=upgrade/rid@=1/gid@=-9999/uid@=12001/nn@=test/level@=3/ic@=icon/
     */
    upgrade,
    /**
     * 主播等级提升广播
     * type@=upbc/rid@=1/gid@=-9999/lev@=20/pu@=0/
     */
    upbc,
    /**
     * 禁言操作结果
     * type@=newblackres/rid@=1/gid@=-9999/ret@=0/otype@=2/sid@=10002/did@=10003/snic@=stest/dnic@=dtest/endtime@=1501920157/
     */
    newblackres,
    /**
     * 徽章等级提升通知
     * type@=blab/rid@=1/gid@=-9999/uid@=10002/nn@=test/lbl@=2/bl@=3/ba@=1/bnn@=ttt/
     */
    blab,
    /**
     * 用户分享了直播间通知
     * type@=srres/rid@=1/gid@=-9999/uid@=12001/nickname@=test/exp@=3/
     */
    srres,
    /**
     * 栏目排行榜变更通知
     * type@=rri/rid@=1/rn@=cate_rank/cate_id@=5/uid@=10005/sc@=100 00/idx@=10/bcr@=1/ibc@=1/an@=test/rktype@=1/tag_id@=1200/gif t_id@=100/
     */
    rri,
    mapkb(MapkbMsg.class),
    /**
     * 发送弹幕
     */
    chatmessage(ChatmessageMsg.class),
    h5ckreq(H5ckreqMsg.class),
    h5gkcreq(H5gkcreqMsg.class),
    h5cs(H5csMsg.class),
    msgrepeaterproxylist(MsgrepeaterproxylistMsg.class),
    ;

    private Class<?> tClass;

    public static DouyuCmdEnum getByString(String cmd) {
        try {
            return DouyuCmdEnum.valueOf(cmd);
        } catch (Exception e) {
            return null;
        }
    }
}
