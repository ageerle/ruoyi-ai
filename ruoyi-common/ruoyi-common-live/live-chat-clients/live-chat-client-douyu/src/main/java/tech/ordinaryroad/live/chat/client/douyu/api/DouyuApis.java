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

package tech.ordinaryroad.live.chat.client.douyu.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;
import tech.ordinaryroad.live.chat.client.commons.util.OrLocalDateTimeUtil;
import tech.ordinaryroad.live.chat.client.douyu.msg.dto.GiftPropSingle;

import java.util.List;
import java.util.Map;

/**
 * API简易版
 *
 * @author mjz
 * @date 2023/5/5
 */
@Slf4j
public class DouyuApis {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String PATTERN_BODY_ROOM_ID = "\\$ROOM\\.room_id\\D+(\\d+)";
    public static final String KEY_REDIRECT_LOCATION_RID = "rid";
    public static final String KEY_COOKIE_DY_DID = "dy_did";
    public static final String KEY_COOKIE_ACF_UID = "acf_uid";
    public static final String KEY_COOKIE_ACF_STK = "acf_stk";
    public static final String KEY_COOKIE_ACF_LTKID = "acf_ltkid";
    public static final String API_AVATAR = "https://apic.douyucdn.cn/upload/";
    // https://webconf.douyucdn.cn/resource/common/gift/flash/gift_effect.json
    // https://webconf.douyucdn.cn/resource/common/gift/common_config_v2.json
    // https://webconf.douyucdn.cn/resource/common/prop_gift_list/prop_gift_config.json
    // 用PID查询礼物信息：https://gift.douyucdn.cn/api/prop/v1/web/single?pid=
    // 查询房间礼物列表：https://gift.douyucdn.cn/api/gift/v3/web/list?rid=
    public static final String API_GIFT_LIST = "https://gift.douyucdn.cn/api/gift/v3/web/list?rid=";
    public static final String API_PROP_SINGLE = "https://gift.douyucdn.cn/api/prop/v1/web/single?pid=";
    public static final String API_AVATAR_PREFIX_SMALL = "_small.jpg";
    public static final String API_AVATAR_PREFIX_MIDDLE = "_middle.jpg";
    public static final String API_AVATAR_PREFIX_BIG = "_big.jpg";

    public static String getAvatarUrl(List<String> list, String prefix) {
        if (CollUtil.isEmpty(list) || list.size() < 3) {
            return StrUtil.EMPTY;
        }
        return API_AVATAR + CollUtil.join(list, "/") + prefix;
    }

    public static String getSmallAvatarUrl(List<String> list) {
        return getAvatarUrl(list, API_AVATAR_PREFIX_SMALL);
    }

    public static long getRealRoomId(long roomId, String cookie) {
        String realRoomIdString = null;
        @Cleanup
        HttpResponse execute = createGetRequest("https://www.douyu.com/" + roomId, cookie).execute();
        if (execute.getStatus() == HttpStatus.HTTP_NOT_FOUND) {
            throw new BaseException("获取" + roomId + "真实房间ID失败");
        }
        if (HttpStatus.isRedirected(execute.getStatus())) {
            String location = execute.header(Header.LOCATION);
            Map<String, String> paramMap = HttpUtil.decodeParamMap(location, null);
            if (paramMap.containsKey(KEY_REDIRECT_LOCATION_RID)) {
                realRoomIdString = paramMap.get(KEY_REDIRECT_LOCATION_RID);
            }
        }
        if (StrUtil.isBlank(realRoomIdString)) {
            String body = execute.body();
            String matchString = ReUtil.get(PATTERN_BODY_ROOM_ID, body, 1);
            if (StrUtil.isNotBlank(matchString)) {
                realRoomIdString = matchString;
            }
        }
        long realRoomId = roomId;
        if (!StrUtil.isBlank(realRoomIdString)) {
            try {
                realRoomId = NumberUtil.parseLong(realRoomIdString);
            } catch (Exception e) {
                throw new BaseException("获取" + roomId + "真实房间ID失败");
            }
        }
        return realRoomId;
    }

    public static long getRealRoomId(long roomId) {
        return getRealRoomId(roomId, null);
    }

    public static JsonNode getServerInfo(long roomId, String cookie) {
        @Cleanup
        HttpResponse execute = createPostRequest("https://www.douyu.com/lapi/live/gateway/web/" + roomId + "?isH5=1", cookie).execute();
        return responseInterceptor(execute.body());
    }

    public static JsonNode getServerInfo(long roomId) {
        return getServerInfo(roomId, null);
    }

    public static String getRandomWssUri(long roomId) {
        JsonNode serverInfo = getServerInfo(roomId);
        JsonNode wss = serverInfo.get("wss");
        JsonNode jsonNode = wss.get(RandomUtil.randomInt(0, wss.size()));
        return "wss://" + jsonNode.get("domain").asText() + ":" + jsonNode.get("port").asInt();
    }

    public static JsonNode getGiftList(long roomId) {
        @Cleanup
        HttpResponse execute = createGetRequest(API_GIFT_LIST + roomId, null).execute();
        return responseInterceptor(execute.body());
    }

    @SneakyThrows
    public static GiftPropSingle getGiftPropSingleByPid(String pid) {
        @Cleanup
        HttpResponse execute = createGetRequest(API_PROP_SINGLE + pid, null).execute();
        JsonNode jsonNode = responseInterceptor(execute.body());
        return BaseMsg.OBJECT_MAPPER.readValue(jsonNode.toString(), GiftPropSingle.class);
    }

    public static final String vk_secret = "r5*^5;}2#${XF[h+;'./.Q'1;,-]f'p[";

    /**
     * 参考： <a href="https://cjting.me/2020/07/01/douyu-crawler-and-font-anti-crawling">斗鱼关注人数爬取 ── 字体反爬的攻与防</a>
     *
     * @param currentTimeSecs
     * @param did
     * @return
     */
    public static String generateVk(long currentTimeSecs, String did) {
        return MD5.create().digestHex(currentTimeSecs + vk_secret + did);
    }

    public static String generateVk(String did) {
        return generateVk(OrLocalDateTimeUtil.zonedCurrentTimeSecs(), did);
    }

    public static HttpRequest createGetRequest(String url, String cookie) {
        return HttpUtil.createGet(url)
                .cookie(cookie);
    }

    public static HttpRequest createPostRequest(String url, String cookie) {
        return HttpUtil.createPost(url)
                .cookie(cookie);
    }

    private static JsonNode responseInterceptor(String responseString) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(responseString);
            int code = jsonNode.get("error").asInt();
            if (code == 0) {
                // 成功
                return jsonNode.get("data");
            } else {
                throw new BaseException(jsonNode.get("msg").asText());
            }
        } catch (JsonProcessingException e) {
            throw new BaseException(e);
        }
    }

}
