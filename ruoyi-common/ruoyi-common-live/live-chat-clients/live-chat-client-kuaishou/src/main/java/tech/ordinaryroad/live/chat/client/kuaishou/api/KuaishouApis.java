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

package tech.ordinaryroad.live.chat.client.kuaishou.api;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.*;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatCookieUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg.OBJECT_MAPPER;

/**
 * @author mjz
 * @date 2024/1/5
 */
public class KuaishouApis {

    /**
     * 接口返回结果缓存
     * {@link #KEY_RESULT_CACHE_GIFT_ITEMS}：所有礼物信息
     */
    public static final TimedCache<String, Map<String, GiftInfo>> RESULT_CACHE = new TimedCache<>(TimeUnit.DAYS.toMillis(1));
    public static final String KEY_RESULT_CACHE_GIFT_ITEMS = "GIFT_ITEMS";

    public static final String PATTERN_LIVE_STREAM_ID = "\"liveStream\":\\{\"id\":\"([\\w\\d-_]+)\"";
    public static final String USER_AGENT = GlobalHeaders.INSTANCE.header(Header.USER_AGENT).replace("Hutool", "");

    public static RoomInitResult roomInit(Object roomId, String cookie) {
        @Cleanup
        HttpResponse response = createGetRequest("https://live.kuaishou.com/u/" + roomId, cookie)
                .execute();

        if (StrUtil.isBlank(cookie)) {
            cookie = OrLiveChatCookieUtil.toString(response.getCookies());
        }

        String body = response.body();
        String liveStreamId = ReUtil.getGroup1(PATTERN_LIVE_STREAM_ID, body);
        JsonNode websocketinfo = websocketinfo(roomId, liveStreamId, cookie);
        if (!websocketinfo.has("token")) {
            throwExceptionWithTip("主播未开播，token获取失败 " + websocketinfo);
        }
        ArrayNode websocketUrls = websocketinfo.withArray("websocketUrls");
        ArrayList<String> websocketUrlList = CollUtil.newArrayList();
        for (JsonNode websocketUrl : websocketUrls) {
            websocketUrlList.add(websocketUrl.asText());
        }
        return RoomInitResult.builder()
                .token(websocketinfo.required("token").asText())
                .websocketUrls(websocketUrlList)
                .liveStreamId(liveStreamId)
                .build();
    }

    public static RoomInitResult roomInit(Object roomId) {
        return roomInit(roomId, null);
    }

    public static JsonNode websocketinfo(Object roomId, String liveStreamId, String cookie) {
        if (StrUtil.isBlank(liveStreamId)) {
            throwExceptionWithTip("主播未开播，liveStreamId为空");
        }
        @Cleanup
        HttpResponse response = createGetRequest("https://live.kuaishou.com/live_api/liveroom/websocketinfo?liveStreamId=" + liveStreamId, cookie)
                .header(Header.REFERER, "https://live.kuaishou.com/u/" + roomId)
                .execute();
        return responseInterceptor(response.body());
    }

    public static Map<String, GiftInfo> allgifts() {
        Map<String, GiftInfo> map = new HashMap<>();
        @Cleanup
        HttpResponse response = createGetRequest("https://live.kuaishou.com/live_api/emoji/allgifts", null).execute();
        JsonNode jsonNode = responseInterceptor(response.body());
        jsonNode.fields().forEachRemaining(new Consumer<Map.Entry<String, JsonNode>>() {
            @Override
            public void accept(Map.Entry<String, JsonNode> stringJsonNodeEntry) {
                map.put(stringJsonNodeEntry.getKey(), OBJECT_MAPPER.convertValue(stringJsonNodeEntry.getValue(), GiftInfo.class));
            }
        });
        return map;
    }

    /**
     * 根据礼物ID获取礼物信息
     *
     * @param id 礼物ID
     * @return 礼物信息
     */
    public static GiftInfo getGiftInfoById(String id) {
        if (!RESULT_CACHE.containsKey(KEY_RESULT_CACHE_GIFT_ITEMS)) {
            RESULT_CACHE.put(KEY_RESULT_CACHE_GIFT_ITEMS, allgifts());
        }
        return RESULT_CACHE.get(KEY_RESULT_CACHE_GIFT_ITEMS).get(id);
    }

    @SneakyThrows
    public static JsonNode sendComment(String cookie, Object roomId, SendCommentRequest request) {
        @Cleanup
        HttpResponse response = createPostRequest("https://live.kuaishou.com/live_api/liveroom/sendComment", cookie)
                .body(OBJECT_MAPPER.writeValueAsString(request), ContentType.JSON.getValue())
                .header(Header.REFERER, "https://live.kuaishou.com/u/" + roomId)
                .execute();
        return responseInterceptor(response.body());
    }

    @SneakyThrows
    public static JsonNode clickLike(String cookie, Object roomId, String liveStreamId, int count) {
        @Cleanup
        HttpResponse response = createPostRequest("https://live.kuaishou.com/live_api/liveroom/like", cookie)
                .body(OBJECT_MAPPER.createObjectNode()
                        .put("liveStreamId", liveStreamId)
                        .put("count", count)
                        .toString(), ContentType.JSON.getValue()
                )
                .header(Header.ORIGIN, "https://live.kuaishou.com")
                .header(Header.REFERER, "https://live.kuaishou.com/u/" + roomId)
                .execute();
        return responseInterceptor(response.body());
    }

    public static HttpRequest createRequest(Method method, String url, String cookie) {
        return HttpUtil.createRequest(method, url)
                .cookie(cookie)
                .header(Header.HOST, URLUtil.url(url).getHost())
                .header(Header.USER_AGENT, USER_AGENT);
    }

    public static HttpRequest createGetRequest(String url, String cookie) {
        return createRequest(Method.GET, url, cookie);
    }

    public static HttpRequest createPostRequest(String url, String cookie) {
        return createRequest(Method.POST, url, cookie);
    }

    private static JsonNode responseInterceptor(String responseString) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(responseString);
            JsonNode data = jsonNode.required("data");
            if (data.has("result")) {
                int result = data.get("result").asInt();
                if (result != 1) {
                    String message = "";
                    switch (result) {
                        case 2: {
                            message = "请求过快，请稍后重试";
                            break;
                        }
                        case 400002: {
                            message = "需要进行验证";
                            break;
                        }
                        default: {
                            message = "";
                        }
                    }
                    throwExceptionWithTip("接口访问失败：" + message + "，返回结果：" + jsonNode);
                }
            }
            return data;
        } catch (JsonProcessingException e) {
            throw new BaseException(e);
        }
    }

    private static void throwExceptionWithTip(String message) {
        throw new BaseException("『可能已触发滑块验证，建议配置Cookie或打开浏览器进行滑块验证后重试』" + message);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SendCommentRequest {
        private String liveStreamId;
        private String content;
        private String color;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RoomInitResult {
        private String token;
        private String liveStreamId;
        private List<String> websocketUrls;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GiftInfo {
        private String giftName;
        private String giftUrl;
    }
}
