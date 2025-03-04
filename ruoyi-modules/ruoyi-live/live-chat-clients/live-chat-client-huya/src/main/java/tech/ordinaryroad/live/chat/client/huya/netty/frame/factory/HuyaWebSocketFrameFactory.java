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

package tech.ordinaryroad.live.chat.client.huya.netty.frame.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatCookieUtil;
import tech.ordinaryroad.live.chat.client.huya.api.HuyaApis;
import tech.ordinaryroad.live.chat.client.huya.config.HuyaLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaClientTemplateTypeEnum;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaLiveSource;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaWupFunctionEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.WebSocketCommand;
import tech.ordinaryroad.live.chat.client.huya.msg.req.*;
import tech.ordinaryroad.live.chat.client.huya.util.HuyaCodecUtil;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mjz
 * @date 2023/1/5
 */
public class HuyaWebSocketFrameFactory {

    private static final ConcurrentHashMap<Object, HuyaWebSocketFrameFactory> CACHE = new ConcurrentHashMap<>();
    public static final String KEY_COOKIE_GUID = "guid";
    public static final String KEY_COOKIE_YYUID = "yyuid";

    /**
     * 浏览器地址中的房间id，支持短id
     */
    private final Object roomId;
    private final JsonNode roomInfo;
    private volatile static byte[] heartbeatMsg;
    private volatile static byte[] giftListReqMsg;

    public HuyaWebSocketFrameFactory(Object roomId) {
        this.roomId = roomId;
        this.roomInfo = HuyaApis.roomInit(roomId);
    }

    public synchronized static HuyaWebSocketFrameFactory getInstance(Object roomId) {
        return CACHE.computeIfAbsent(roomId, aLong -> new HuyaWebSocketFrameFactory(roomId));
    }

    /**
     * 创建弹幕包
     *
     * @param msg    弹幕内容
     * @param ver    {@link HuyaLiveChatClientConfig#getVer()}
     * @param cookie {@link HuyaLiveChatClientConfig#getCookie()} ()}
     * @return BinaryWebSocketFrame
     */
    public BinaryWebSocketFrame createSendMessageReq(String msg, String ver, String cookie) {
        String yyuid = OrLiveChatCookieUtil.getCookieByName(cookie, KEY_COOKIE_YYUID, () -> {
            throw new BaseException("cookie中缺少参数" + KEY_COOKIE_YYUID);
        });
        String guid = OrLiveChatCookieUtil.getCookieByName(cookie, KEY_COOKIE_GUID, () -> {
            throw new BaseException("cookie中缺少参数" + KEY_COOKIE_GUID);
        });
        SendMessageReq sendMessageReq = new SendMessageReq();
        sendMessageReq.getTUserId().setLUid(NumberUtil.parseLong(yyuid));
        sendMessageReq.getTUserId().setSGuid(guid);
        sendMessageReq.getTUserId().setSHuYaUA("webh5&" + ver + "&websocket");
        sendMessageReq.getTUserId().setSCookie(cookie);
        sendMessageReq.getTUserId().setSDeviceInfo("chrome");
        sendMessageReq.setSContent(msg);
        sendMessageReq.setLPid(roomInfo.get("lChannelId").asLong());

        WupReq wupReq = new WupReq();
        wupReq.getTarsServantRequest().setServantName("liveui");
        wupReq.getTarsServantRequest().setFunctionName("sendMessage");
        wupReq.getUniAttribute().put("tReq", sendMessageReq);

        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmd_WupReq.getCode());
        webSocketCommand.setVData(wupReq.encode());
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    /**
     * 创建认证包
     * 1. doLaunch
     * 2. registerGroup
     * 3. updateUserInfo
     *
     * @return AuthWebSocketFrame
     */
    public BinaryWebSocketFrame createAuth(String ver, String cookie) {
        // sFuncName
        // getLivingInfo, huyaliveui
        // getPresenterLiveScheduleInfo, presenterui
        // doLaunch, liveui
        // chat:1724691, live:1724691
        // getWebdbUserInfo, liveui
        // OnUserHeartBeat, onlineuif
//        try {
//            UserInfo wsUserInfo = new UserInfo();
//            wsUserInfo.setLUid(roomInfo.get("lYyid").asLong());
//            wsUserInfo.setBAnonymous(roomInfo.get("lYyid").asLong() == 0);
//            wsUserInfo.setLTid(roomInfo.get("lChannelId").asLong());
//            wsUserInfo.setLSid(roomInfo.get("lSubChannelId").asLong());
//            wsUserInfo.setLGroupId(roomInfo.get("lYyid").asLong());
//            wsUserInfo.setLGroupType(3);
//            wsUserInfo.setSAppId("");
//            wsUserInfo.setSUA("webh5&%s&websocket".formatted(HuyaLiveChatClientConfig.VER));
//
//            WebSocketCommand webSocketCommand = new WebSocketCommand();
//            webSocketCommand.setOperation(HuyaOperationEnum.EWSCmd_RegisterReq.getCode());
//            webSocketCommand.setVData(wsUserInfo.toByteArray());
//            return new AuthWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
//        } catch (Exception e) {
//            throw new BaseException("认证包创建失败，请检查房间号是否正确。roomId: %d, msg: %s".formatted(roomId, e.getMessage()));
//        }
        return createLiveLaunchReq(ver, cookie);
    }

    public BinaryWebSocketFrame createGetLivingInfoReq(String ver, String cookie) {
        GetLivingInfoReq getLivingInfoReq = new GetLivingInfoReq();
//        getLivingInfoReq.getTId().setSGuid("0a7dca72a3ce1b654001dd2ade2ae857");
        getLivingInfoReq.getTId().setSHuYaUA("webh5&" + ver + "&websocket");
        getLivingInfoReq.getTId().setSDeviceInfo("chrome");
        getLivingInfoReq.getTId().setSCookie(StrUtil.nullToEmpty(cookie));
//        getLivingInfoReq.getTId().setSCookie("vplayer_sbanner_1724691_1724691=1; SoundValue=0.50; alphaValue=0.80; game_did=R24J2g0mBzvdXJf7a9nheSl3zIci2BOp0-t; isInLiveRoom=true; guid=0a7dca72a3ce1b654001dd2ade2ae857; __yamid_tt1=0.8029935065011269; __yamid_new=CA75D4CD5C100001165B82B52140C900; guid=0a7dca72a3ce1b654001dd2ade2ae857; udb_guiddata=af5dbdbf76254a6e8a0a293ccae9b688; udb_deviceid=w_761623483861815296; udb_passdata=3; __yasmid=0.8029935065011269; _yasids=__rootsid%3DCA767045C1100001EC5A145C1E90FE00; Hm_lvt_51700b6c722f5bb4cf39906a596ea41f=1696473760,1696476745,1696483565,1696484212; Hm_lpvt_51700b6c722f5bb4cf39906a596ea41f=1696484212; huya_ua=webh5&0.0.1&activity; _rep_cnt=2; sdid=0UnHUgv0/qmfD4KAKlwzhqX98QrnPCcck6fN494iawS5Kmymgreu89o7gYta/QvQsBDeSeSRp4/grQ5fEuFAYrL59coAKuxuukiwgTLjfjEDWVkn9LtfFJw/Qo4kgKr8OZHDqNnuwg612sGyflFn1dkUeZYTToCzzl4GCHq7MUDahxGuPR8mUddfImFtjccs1; huya_flash_rep_cnt=74; huya_web_rep_cnt=125; rep_cnt=44");
        getLivingInfoReq.setLPresenterUid(roomInfo.get("lChannelId").asLong());

        WupReq wupReq = new WupReq();
        wupReq.getTarsServantRequest().setServantName("huyaliveui");
        wupReq.getTarsServantRequest().setFunctionName("getLivingInfo");
        wupReq.getUniAttribute().put("tReq", getLivingInfoReq);

        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmd_WupReq.getCode());
        webSocketCommand.setVData(wupReq.encode());
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    public BinaryWebSocketFrame createLiveLaunchReq(String ver, String cookie) {
        LiveLaunchReq liveLaunchReq = new LiveLaunchReq();
        liveLaunchReq.setBSupportDomain(true);
//        liveLaunchReq.getTId().setSGuid(UUID.fastUUID().toString(true));
        liveLaunchReq.getTId().setSHuYaUA("webh5&" + ver + "&websocket");
        liveLaunchReq.getTId().setSDeviceInfo("chrome");
        liveLaunchReq.getTId().setSCookie(StrUtil.nullToEmpty(cookie));
        liveLaunchReq.getTLiveUB().setESource(HuyaLiveSource.WEB_HUYA.getCode());

        WupReq wupReq = new WupReq();
        wupReq.getTarsServantRequest().setServantName("liveui");
        wupReq.getTarsServantRequest().setFunctionName("doLaunch");
        wupReq.getUniAttribute().put("tReq", liveLaunchReq);

        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmd_WupReq.getCode());
        webSocketCommand.setVData(wupReq.encode());
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    public BinaryWebSocketFrame createRegisterGroupReq() {
        String lYyid = roomInfo.get("lChannelId").asText();

        RegisterGroupReq registerGroupReq = new RegisterGroupReq();
        registerGroupReq.setVGroupId(CollUtil.newArrayList("live:" + lYyid, "chat:" + lYyid));

        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmdC2S_RegisterGroupReq.getCode());
        webSocketCommand.setVData(registerGroupReq.toByteArray());
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    public BinaryWebSocketFrame createUpdateUserInfoReq() {
        UpdateUserInfoReq updateUserInfoReq = new UpdateUserInfoReq();
        updateUserInfoReq.setSAppSrc("HUYA&ZH&2052");
        updateUserInfoReq.getTWSMsgStatInfo().setISupportAckMsgStat(1);

        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmdC2S_UpdateUserInfoReq.getCode());
        webSocketCommand.setVData(updateUserInfoReq.toByteArray());
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    /**
     * 创建获取礼物列表请求包
     *
     * @return BinaryWebSocketFrame
     */
    public BinaryWebSocketFrame createGiftListReq(String ver) {
        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmd_WupReq.getCode());
        webSocketCommand.setVData(this.getGiftListReqMsg(ver));
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    public BinaryWebSocketFrame createHeartbeat(String ver, String cookie) {
        WebSocketCommand webSocketCommand = new WebSocketCommand();
        webSocketCommand.setOperation(HuyaOperationEnum.EWSCmdC2S_HeartBeatReq.getCode());
        webSocketCommand.setVData(this.getHeartbeatMsg(ver, cookie));
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(webSocketCommand.toByteArray()));
    }

    /**
     * 心跳包单例模式
     */
    public byte[] getHeartbeatMsg(String ver, String cookie) {
        if (heartbeatMsg == null) {
            synchronized (HuyaWebSocketFrameFactory.this) {
                if (heartbeatMsg == null) {
                    UserHeartBeatReq userHeartBeatReq = new UserHeartBeatReq();
//                    userHeartBeatReq.getTId().setSGuid("");
                    userHeartBeatReq.getTId().setSHuYaUA("webh5&" + ver + "&websocket");
                    userHeartBeatReq.getTId().setSDeviceInfo("chrome");
                    userHeartBeatReq.getTId().setSCookie(StrUtil.nullToEmpty(cookie));
//                    userHeartBeatReq.setLSid(roomInfo.get("lSubChannelId").asLong());
                    userHeartBeatReq.setLPid(roomInfo.get("lChannelId").asLong());
//                    userHeartBeatReq.setELineType(HuyaStreamLineTypeEnum.STREAM_LINE_WS.getCode());
                    userHeartBeatReq.setELineType(-1);

                    heartbeatMsg = HuyaCodecUtil.encode("onlineui", HuyaWupFunctionEnum.OnUserHeartBeat, userHeartBeatReq);
                }
            }
        }
        return heartbeatMsg;
    }

    /**
     * 礼物列表请求包单例模式
     */
    public byte[] getGiftListReqMsg(String ver) {
        if (giftListReqMsg == null) {
            synchronized (HuyaWebSocketFrameFactory.this) {
                if (giftListReqMsg == null) {
                    GetPropsListReq getPropsListReq = new GetPropsListReq();
                    getPropsListReq.getTUserId().setLUid(roomInfo.get("lYyid").asLong());
                    getPropsListReq.getTUserId().setSHuYaUA("webh5&" + ver + "&websocket");
                    getPropsListReq.setITemplateType(HuyaClientTemplateTypeEnum.TPL_MIRROR.getCode());

                    giftListReqMsg = HuyaCodecUtil.encode("PropsUIServer", HuyaWupFunctionEnum.getPropsList, getPropsListReq);
                }
            }
        }
        return giftListReqMsg;
    }

}
