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
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GiftListInfo {

    public static final GiftListInfo DEFAULT_GIFT = new GiftListInfo();

    private BasicInfo basicInfo;
    private JsonNode batchInfo;
    private int batchInfoFlag;
    private int countLimit;
    private int defaultSkinId;
    private int donateStatus;
    private JsonNode effectInfo;
    private int effectStatus;
    private int endTime;
    private JsonNode growthInfo;
    private long hitInterval;
    private int id;
    private int isBatchLimited;
    private int isFace;
    private String name = "未知礼物";
    private String picUrlPrefix;
    private PriceInfo priceInfo = new PriceInfo();
    private int showStatus;
    private List<String> skinIds;
    private int startTime;
    private List<Integer> tabIds;

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

    @Data
    public static class BasicInfo {
        private String bizTag;
        private String borderColor;
        private String chatPic;
        private String culture;
        private String desc1;
        private String desc2;
        private String focusPic;
        private String giftPic;
        private int giftType;
        private int guardLevel;
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

    @Data
    public static class PriceInfo {
        private int price = -1;
        private String priceType;
        private int returnNum;

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
}