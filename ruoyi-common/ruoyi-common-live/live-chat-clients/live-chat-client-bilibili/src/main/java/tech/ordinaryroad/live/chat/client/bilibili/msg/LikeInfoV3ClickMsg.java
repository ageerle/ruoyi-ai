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

package tech.ordinaryroad.live.chat.client.bilibili.msg;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.bilibili.constant.OperationEnum;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.BaseBilibiliMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ILikeMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mjz
 * @date 2024/1/31
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LikeInfoV3ClickMsg extends BaseBilibiliMsg implements ILikeMsg {

    private Data data;

    @Override
    public OperationEnum getOperationEnum() {
        return OperationEnum.SEND_SMS_REPLY;
    }

    @Override
    public String getBadgeName() {
        if (this.data == null || this.data.getFans_medal() == null) {
            return null;
        }

        return this.data.getFans_medal().getMedal_name();
    }

    @Override
    public byte getBadgeLevel() {
        if (this.data == null || this.data.getFans_medal() == null) {
            return 0;
        }

        return this.data.getFans_medal().getMedal_level();
    }

    @Override
    public String getUid() {
        if (this.data == null) {
            return null;
        }

        return Long.toString(this.data.getUid());
    }

    @Override
    public String getUsername() {
        if (this.data == null) {
            return "";
        }

        return this.data.getUname();
    }

    @Override
    public String getUserAvatar() {
        if (this.data == null || this.data.getUinfo() == null || this.data.getUinfo().getBase() == null) {
            return "";
        }

        return this.data.getUinfo().getBase().getFace();
    }

    @lombok.Data
    public static class Data {

        private int show_area;
        private int msg_type;
        private String like_icon;
        private long uid;
        private String like_text;
        private String uname;
        private String uname_color;
        private List<Integer> identities;
        private InteractWordMsg.Fans_medal fans_medal;
        private Contribution_info contribution_info;
        private int dmscore;
        private String group_medal;
        private boolean is_mystery;
        private InteractWordMsg.Uinfo uinfo;

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

    @lombok.Data
    public static class Contribution_info {

        private int grade;

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
