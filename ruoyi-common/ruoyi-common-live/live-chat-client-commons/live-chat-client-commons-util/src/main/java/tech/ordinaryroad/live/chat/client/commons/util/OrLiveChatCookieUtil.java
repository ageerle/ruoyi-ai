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

package tech.ordinaryroad.live.chat.client.commons.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author mjz
 * @date 2023/8/27
 */
public class OrLiveChatCookieUtil {

    public static String toString(List<HttpCookie> cookies) {
        if (CollUtil.isEmpty(cookies)) {
            return StrUtil.EMPTY;
        }

        return cookies.stream().map(httpCookie -> {
            httpCookie.setVersion(0);
            return httpCookie.toString();
        }).collect(Collectors.joining("; "));
    }

    public static Map<String, String> parseCookieString(String cookies) {
        Map<String, String> map = new HashMap<>();
        if (StrUtil.isNotBlank(cookies) && !StrUtil.isNullOrUndefined(cookies)) {
            try {
                String[] split = cookies.split("; ");
                for (String s : split) {
                    String[] split1 = s.split("=");
                    map.put(split1[0], split1[1]);
                }
            } catch (Exception e) {
                throw new RuntimeException("cookie解析失败 " + cookies, e);
            }
        }
        return map;
    }

    public static String getCookieByName(Map<String, String> cookieMap, String name, Supplier<String> supplier) {
        String str = MapUtil.getStr(cookieMap, name);
        return str == null ? supplier.get() : str;
    }

    public static String getCookieByName(String cookie, String name, Supplier<String> supplier) {
        String str = MapUtil.getStr(parseCookieString(cookie), name);
        return str == null ? supplier.get() : str;
    }
}
