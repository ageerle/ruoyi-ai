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

package tech.ordinaryroad.live.chat.client.douyu.netty.frame.factory;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatCookieUtil;
import tech.ordinaryroad.live.chat.client.douyu.api.DouyuApis;
import tech.ordinaryroad.live.chat.client.douyu.config.DouyuLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuClientModeEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.*;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;
import tech.ordinaryroad.live.chat.client.douyu.netty.frame.AuthWebSocketFrame;
import tech.ordinaryroad.live.chat.client.douyu.netty.frame.HeartbeatWebSocketFrame;
import tech.ordinaryroad.live.chat.client.douyu.netty.frame.KeepliveWebSocketFrame;
import tech.ordinaryroad.live.chat.client.douyu.util.DouyuCodecUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mjz
 * @date 2023/1/5
 */
public class DouyuWebSocketFrameFactory {

    private static final ConcurrentHashMap<Long, DouyuWebSocketFrameFactory> CACHE = new ConcurrentHashMap<>();
    /**
     * 浏览器地址中的房间id，暂不支持短id
     */
    private final long roomId;
    private volatile static HeartbeatMsg heartbeatMsg;

    public DouyuWebSocketFrameFactory(long roomId) {
        this.roomId = roomId;
    }

    public synchronized static DouyuWebSocketFrameFactory getInstance(long roomId) {
        return CACHE.computeIfAbsent(roomId, aLong -> new DouyuWebSocketFrameFactory(roomId));
    }

    /**
     * 创建认证包
     *
     * @param mode   {@link DouyuClientModeEnum#DANMU}, {@link DouyuClientModeEnum#WS}
     * @param ver    {@link DouyuLiveChatClientConfig#getVer()}
     * @param aver   {@link DouyuLiveChatClientConfig#getAver()}
     * @param cookie 浏览器Cookie，发送弹幕时必传
     * @return AuthWebSocketFrame
     */
    public AuthWebSocketFrame createAuth(DouyuClientModeEnum mode, String ver, String aver, String cookie) {
        try {
            // type@=loginreq/roomid@=7750753/dfl@=/username@=visitor10424697/uid@=1168052601/ver@=20220825/aver@=218101901/ct@=0/
            LoginreqMsg loginreqMsg;
            long realRoomId = DouyuApis.getRealRoomId(roomId);
            long uid;
            String username;
            Map<String, String> cookieMap = OrLiveChatCookieUtil.parseCookieString(cookie);

            if (cookieMap.isEmpty()) {
                // 视为未登录
                if (mode == DouyuClientModeEnum.DANMU) {
                    uid = RandomUtil.randomLong(10000000, 19999999);
                    username = "visitor" + RandomUtil.randomLong(10000000, 19999999);
                    loginreqMsg = new LoginreqMsg(realRoomId, "", username, uid, ver, aver);
                    return new AuthWebSocketFrame(DouyuCodecUtil.encode(loginreqMsg, LoginreqMsg.SHOULD_IGNORE_NEW_LOGIN_PROPERTIES));
                } else {
                    loginreqMsg = new LoginreqMsg(realRoomId, "", "", ver, aver, "", "", "", UUID.fastUUID().toString(true));
                    return new AuthWebSocketFrame(DouyuCodecUtil.encode(loginreqMsg, LoginreqMsg.SHOULD_IGNORE_OLD_LOGIN_PROPERTIES));
                }
            }
            // 视为登录
            else {
                String acfUid = OrLiveChatCookieUtil.getCookieByName(cookieMap, DouyuApis.KEY_COOKIE_ACF_UID, () -> {
                    throw new BaseException("Cookie中缺少字段" + DouyuApis.KEY_COOKIE_ACF_UID);
                });
                uid = NumberUtil.parseLong(acfUid);
                username = acfUid;
                String dfl = "sn@A=105@Sss@A=1";
                if (mode == DouyuClientModeEnum.DANMU) {
                    loginreqMsg = new LoginreqMsg(realRoomId, dfl, username, uid, ver, aver);
                    return new AuthWebSocketFrame(DouyuCodecUtil.encode(loginreqMsg, LoginreqMsg.SHOULD_IGNORE_NEW_LOGIN_PROPERTIES));
                } else {
                    String acfLtkid = OrLiveChatCookieUtil.getCookieByName(cookieMap, DouyuApis.KEY_COOKIE_ACF_LTKID, () -> {
                        throw new BaseException("Cookie中缺少字段" + DouyuApis.KEY_COOKIE_ACF_LTKID);
                    });
                    String acfStk = OrLiveChatCookieUtil.getCookieByName(cookieMap, DouyuApis.KEY_COOKIE_ACF_STK, () -> {
                        throw new BaseException("Cookie中缺少字段" + DouyuApis.KEY_COOKIE_ACF_STK);
                    });
                    String dyDid = OrLiveChatCookieUtil.getCookieByName(cookieMap, DouyuApis.KEY_COOKIE_DY_DID, () -> {
                        throw new BaseException("Cookie中缺少字段" + DouyuApis.KEY_COOKIE_DY_DID);
                    });
                    loginreqMsg = new LoginreqMsg(realRoomId, dfl, username, ver, aver, acfLtkid, "1", acfStk, dyDid);
                    return new AuthWebSocketFrame(DouyuCodecUtil.encode(loginreqMsg, LoginreqMsg.SHOULD_IGNORE_OLD_LOGIN_PROPERTIES));
                }
            }
        } catch (Exception e) {
            throw new BaseException(String.format("认证包创建失败，请检查房间号是否正确。roomId: %d, msg: %s", roomId, e.getMessage()));
        }
    }

    public AuthWebSocketFrame createAuth(DouyuClientModeEnum mode, String ver, String aver) {
        return this.createAuth(mode, ver, aver, null);
    }

    public HeartbeatWebSocketFrame createHeartbeat() {
        return new HeartbeatWebSocketFrame(DouyuCodecUtil.encode(this.getHeartbeatMsg()));
    }

    public KeepliveWebSocketFrame createKeeplive(String cookie) {
        return new KeepliveWebSocketFrame(DouyuCodecUtil.encode(this.getKeepliveMsg(StrUtil.isNotBlank(cookie) ? "hs-h5" : "")));
    }

    /**
     * 心跳包单例模式
     *
     * @return HeartbeatWebSocketFrame
     */
    public HeartbeatMsg getHeartbeatMsg() {
        if (heartbeatMsg == null) {
            synchronized (DouyuWebSocketFrameFactory.this) {
                if (heartbeatMsg == null) {
                    heartbeatMsg = new HeartbeatMsg();
                }
            }
        }
        return heartbeatMsg;
    }

    private BaseDouyuCmdMsg getKeepliveMsg(String cnd) {
        return new KeepliveMsg(cnd);
    }

    public WebSocketFrame createJoingroup() {
        JoingroupMsg joingroupMsg = new JoingroupMsg();
        joingroupMsg.setRid(roomId);
        return new BinaryWebSocketFrame(DouyuCodecUtil.encode(joingroupMsg));
    }

    public WebSocketFrame createSub() {
        return new BinaryWebSocketFrame(DouyuCodecUtil.encode(new SubMsg()));
    }

    public WebSocketFrame createDanmu(String msg, String cookie) {
        String dyDid = OrLiveChatCookieUtil.getCookieByName(cookie, DouyuApis.KEY_COOKIE_DY_DID, () -> {
            throw new BaseException("cookie中缺少参数" + DouyuApis.KEY_COOKIE_DY_DID);
        });
        String acfUid = OrLiveChatCookieUtil.getCookieByName(cookie, DouyuApis.KEY_COOKIE_ACF_UID, () -> {
            throw new BaseException("cookie中缺少参数" + DouyuApis.KEY_COOKIE_ACF_UID);
        });
        ChatmessageMsg chatmessageMsg = new ChatmessageMsg(msg, dyDid, acfUid);
        return new BinaryWebSocketFrame(DouyuCodecUtil.encode(chatmessageMsg));
    }
}
