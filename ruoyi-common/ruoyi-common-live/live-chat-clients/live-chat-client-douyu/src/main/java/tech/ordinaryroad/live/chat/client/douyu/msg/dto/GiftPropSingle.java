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
package tech.ordinaryroad.live.chat.client.douyu.msg.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GiftPropSingle {

    public static final GiftPropSingle DEFAULT_GIFT = new GiftPropSingle();

    // key,value
    private JsonNode batchInfo;
    private String bizTag;
    private String borderColor;
    private String chatPic;
    private int count;
    private String description;
    private String description2;
    private int devote;
    // key,value
    private JsonNode effectInfo;
    private int exp;
    private int expiry;
    private String focusPic;
    private int hitInterval;
    private long id;
    private int intimate;
    private String intro;
    private int isClick;
    private int isFace;
    private int isValuable;
    private int level;
    private int levelTime;
    private int met;
    private String name = "未知礼物";
    private String picUrlPrefix;
    private int price = -1;
    private int priceType;
    private String propPic;
    private int propType;
    private int returnNum;
    private String sendPic;
    private String subscriptColor;
    private String subscriptText;
    private String webSubscriptBigPic;
    private String webSubscriptSmallPic;
    private String webSubscriptText;

    /**
     * 未知属性都放在这
     */
    private final Map<String, JsonNode> unknownProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, JsonNode> getUnknownProperties() {
        return unknownProperties;
    }

    @JsonAnySetter
    public void setOther(String key, JsonNode value) {
        this.unknownProperties.put(key, value);
    }
}