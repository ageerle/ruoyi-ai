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

package tech.ordinaryroad.live.chat.client.huya.api;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.PropsItem;

import java.util.HashMap;
import java.util.Map;

import static tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg.OBJECT_MAPPER;

/**
 * API简易版
 * <br/>
 * <a href="https://a.msstatic.com/huya/h5player/room/2309271152/vplayerUI.js">vplayerUI.js</a>
 * <br/>
 * <a href="https://hd2.huya.com/fedbasic/huyabaselibs/taf-signal/taf-signal.global.0.0.4.prod.js">taf-signal.global.0.0.4.prod.js</a>
 *
 * @author mjz
 * @date 2023/9/5
 */
@Slf4j
public class HuyaApis {

    // TODO TimedCache
    public static final Map<Integer, PropsItem> GIFT_ITEMS = new HashMap<>();

    public static JsonNode roomInit(Object roomId) {
        @Cleanup
        HttpResponse response = createGetRequest("https://www.huya.com/" + roomId, null).execute();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            throw new BaseException("获取" + roomId + "真实房间ID失败");
        }
        String body = response.body();
        String lSubChannelId = ReUtil.getGroup1("\"lp\"\\D+(\\d+)", body);
        String lChannelId = ReUtil.getGroup1("\"lp\"\\D+(\\d+)", body);
        String lYyid = ReUtil.getGroup1("\"yyid\"\\D+(\\d+)", body);
        ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
        objectNode.put("lSubChannelId", StrUtil.emptyToDefault(lSubChannelId, "0"));
        objectNode.put("lChannelId", StrUtil.emptyToDefault(lChannelId, "0"));
        objectNode.put("lYyid", lYyid);
        return objectNode;
    }

    public static HttpRequest createGetRequest(String url, String cookies) {
        return HttpUtil.createGet(url)
                .cookie(cookies);
    }

}
