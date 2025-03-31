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

package tech.ordinaryroad.live.chat.client.bilibili.api;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.bilibili.api.request.BilibiliLikeReportV3Request;
import tech.ordinaryroad.live.chat.client.bilibili.api.request.BilibiliSendMsgRequest;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatCookieUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg.OBJECT_MAPPER;

/**
 * B站API简易版
 *
 * @author mjz
 * @date 2023/5/5
 */
@Slf4j
public class BilibiliApis {

    public static final TimedCache<Long, String> GIFT_IMG_CACHE = new TimedCache<>(TimeUnit.DAYS.toMillis(1));
    public static final String KEY_COOKIE_CSRF = "bili_jct";
    public static final String KEY_UID = "DedeUserID";

    @SneakyThrows
    public static RoomInitResult roomInit(long roomId, String cookie) {
        @Cleanup
        HttpResponse response = createGetRequest("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId, cookie).execute();
        JsonNode dataJsonNode = responseInterceptor(response.body());
        return OBJECT_MAPPER.readValue(dataJsonNode.toString(), RoomInitResult.class);
    }

    public static JsonNode roomGiftConfig(long roomId, String cookie) {
        @Cleanup
        HttpResponse response = createGetRequest("https://api.live.bilibili.com/xlive/web-room/v1/giftPanel/roomGiftConfig?platform=pc&source=live&build=0&global_version=0&room_id=" + roomId, cookie).execute();
        return responseInterceptor(response.body());
    }

    /**
     * @param roomId
     * @param type   直播间用0
     * @return <pre>{@code
     * {
     * 	"group": "live",
     * 	"business_id": 0,
     * 	"refresh_row_factor": 0.125,
     * 	"refresh_rate": 100,
     * 	"max_delay": 5000,
     * 	"token": "-wm5-Qo4BBAztd1qp5ZJpgyTMRBhCc7yikz5d9rAd63PV46G9BMwl0R10kMM8Ilb-UieZGjLtipPrz4Cvi0DdhGFwOi8PJpFN9K-LoXh6Z_4yjEIwgRerDiMIstHzJ80J3B7wnRisAYkWA==",
     * 	"host_list": [{
     * 		"host": "ali-bj-live-comet-09.chat.bilibili.com",
     * 		"port": 2243,
     * 		"wss_port": 443,
     * 		"ws_port": 2244
     *        }, {
     * 		"host": "ali-gz-live-comet-02.chat.bilibili.com",
     * 		"port": 2243,
     * 		"wss_port": 443,
     * 		"ws_port": 2244
     *    }, {
     * 		"host": "broadcastlv.chat.bilibili.com",
     * 		"port": 2243,
     * 		"wss_port": 443,
     * 		"ws_port": 2244
     *    }]
     * }
     * }</pre>
     */
    public static JsonNode getDanmuInfo(long roomId, int type, String cookie) {
        @Cleanup
        HttpResponse response = createGetRequest("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?id=" + roomId + "&type=" + type, cookie).execute();
        return responseInterceptor(response.body());
    }

    public static String getGiftImgById(long giftId, long roomId) {
        if (!GIFT_IMG_CACHE.containsKey(giftId)) {
            ThreadUtil.execAsync(() -> {
                updateGiftImgCache(roomId, null);
            });
        }

        return GIFT_IMG_CACHE.get(giftId);
    }

    /**
     * 更新礼物图片缓存
     */
    public static void updateGiftImgCache(long roomId, String cookie) {
        JsonNode jsonNode = roomGiftConfig(roomId, cookie);
        for (JsonNode node : jsonNode.get("global_gift").get("list")) {
            long giftId = node.get("id").asLong();
            String giftImgUrl = node.get("webp").asText();
            GIFT_IMG_CACHE.put(giftId, giftImgUrl);
        }
    }

    /**
     * 发送弹幕
     *
     * @param request {@link BilibiliSendMsgRequest}
     * @param cookie  Cookie
     */
    public static void sendMsg(BilibiliSendMsgRequest request, String cookie) {
        if (StrUtil.isBlank(cookie)) {
            throw new BaseException("发送弹幕接口cookie不能为空");
        }
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(request);
        @Cleanup HttpResponse execute = HttpUtil.createPost("https://api.live.bilibili.com/msg/send")
                .cookie(cookie)
                .form(stringObjectMap)
                .execute();
        responseInterceptor(execute.body());
    }

    /**
     * 发送弹幕
     *
     * @param msg        内容
     * @param realRoomId 真实房间id
     * @param cookie     Cookie
     */
    public static void sendMsg(String msg, long realRoomId, String cookie) {
        String biliJct = OrLiveChatCookieUtil.getCookieByName(cookie, KEY_COOKIE_CSRF, () -> {
            throw new BaseException("cookie中缺少参数" + KEY_COOKIE_CSRF);
        });
        BilibiliSendMsgRequest request = new BilibiliSendMsgRequest(msg, StrUtil.toString(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toEpochSecond()), realRoomId, biliJct, biliJct);
        sendMsg(request, cookie);
    }

    /**
     * 为主播点赞
     *
     * @param request {@link BilibiliLikeReportV3Request}
     * @param cookie  Cookie
     */
    public static void likeReportV3(BilibiliLikeReportV3Request request, String cookie) {
        if (StrUtil.isBlank(cookie)) {
            throw new BaseException("为主播点赞接口cookie不能为空");
        }
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(request);
        @Cleanup HttpResponse execute = HttpUtil.createPost("https://api.live.bilibili.com/xlive/app-ucenter/v1/like_info_v3/like/likeReportV3")
                .cookie(cookie)
                .form(stringObjectMap)
                .execute();
        responseInterceptor(execute.body());
    }

    /**
     * 为主播点赞
     *
     * @param anchor_id  主播Uid {@link RoomInitResult#uid}
     * @param realRoomId 真实房间Id {@link RoomInitResult#room_id}
     * @param cookie     Cookie
     */
    public static void likeReportV3(long anchor_id, long realRoomId, String cookie) {
        String uid = OrLiveChatCookieUtil.getCookieByName(cookie, KEY_UID, () -> {
            throw new BaseException("cookie中缺少参数" + KEY_UID);
        });
        String biliJct = OrLiveChatCookieUtil.getCookieByName(cookie, KEY_COOKIE_CSRF, () -> {
            throw new BaseException("cookie中缺少参数" + KEY_COOKIE_CSRF);
        });
        BilibiliLikeReportV3Request request = new BilibiliLikeReportV3Request(realRoomId, uid, anchor_id, biliJct, biliJct);
        likeReportV3(request, cookie);
    }

    public static HttpRequest createGetRequest(String url, String cookies) {
        return HttpUtil.createGet(url)
                .cookie(cookies);
    }

    private static JsonNode responseInterceptor(String responseString) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(responseString);
            int code = jsonNode.get("code").asInt();
            if (code == 0) {
                // 成功
                return jsonNode.get("data");
            } else {
                throw new BaseException(jsonNode.get("message").asText());
            }
        } catch (JsonProcessingException e) {
            throw new BaseException(e);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RoomInitResult {
        private long room_id;
        private int short_id;
        private long uid;
        private int need_p2p;
        private boolean is_hidden;
        private boolean is_locked;
        private boolean is_portrait;
        private int live_status;
        private int hidden_till;
        private int lock_till;
        private boolean encrypted;
        private boolean pwd_verified;
        private long live_time;
        private int room_shield;
        private int is_sp;
        private int special_type;
    }

}
