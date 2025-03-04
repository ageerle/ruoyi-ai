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
import tech.ordinaryroad.live.chat.client.commons.base.msg.IEnterRoomMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mjz
 * @date 2023/12/26
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InteractWordMsg extends BaseBilibiliMsg implements IEnterRoomMsg {

    private Data data;

    @Override
    public OperationEnum getOperationEnum() {
        return OperationEnum.SEND_SMS_REPLY;
    }

    @Override
    public String getBadgeName() {
        if (data == null || data.fans_medal == null) {
            return null;
        }
        return data.fans_medal.medal_name;
    }

    @Override
    public byte getBadgeLevel() {
        if (data == null || data.fans_medal == null) {
            return 0;
        }
        return data.fans_medal.medal_level;
    }

    @Override
    public String getUid() {
        if (data == null) {
            return null;
        }
        return Long.toString(data.uid);
    }

    @Override
    public String getUsername() {
        if (data == null) {
            return null;
        }
        return data.uname;
    }

    @Override
    public String getUserAvatar() {
        if (data == null || data.uinfo == null || data.uinfo.base == null) {
            return null;
        }
        return data.uinfo.base.face;
    }

    @lombok.Data
    public static class Data {

        private Contribution contribution;
        private Contribution_v2 contribution_v2;
        private int core_user_type;
        private int dmscore;
        private Fans_medal fans_medal;
        private String group_medal;
        private List<Integer> identities;
        private boolean is_mystery;
        private int is_spread;
        private int msg_type;
        private int privilege_type;
        private long roomid;
        private long score;
        private String spread_desc;
        private String spread_info;
        private int tail_icon;
        private String tail_text;
        private long timestamp;
        private long trigger_time;
        private long uid;
        private Uinfo uinfo;
        private String uname;
        private String uname_color;

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
    public static class Contribution {

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

    @lombok.Data
    public static class Contribution_v2 {

        private int grade;
        private String rank_type;
        private String text;

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
    public static class Fans_medal {

        private long anchor_roomid;
        private int guard_level;
        private int icon_id;
        private int is_lighted;
        private long medal_color;
        private long medal_color_border;
        private long medal_color_end;
        private long medal_color_start;
        private byte medal_level;
        private String medal_name;
        private long score;
        private String special;
        private long target_id;

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
    public static class Origin_info {

        private String face;
        private String name;

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
    public static class Risk_ctrl_info {

        private String face;
        private String name;

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
    public static class Official_info {

        private int role;
        private String title;
        private String desc;
        private int type;

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
    public static class Base {

        private String face;
        private boolean is_mystery;
        private String name;
        private int name_color;
        private Origin_info origin_info;
        private Risk_ctrl_info risk_ctrl_info;
        private Official_info official_info;

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
    public static class Medal {

        private String name;
        private int level;
        private long color_start;
        private long color_end;
        private long color_border;
        private long color;
        private int id;
        private int typ;
        private int is_light;
        private long ruid;
        private int guard_level;
        private int score;
        private String guard_icon;
        private String honor_icon;

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
    public static class Guard {

        private int level;
        private String expired_str;

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
    public static class Uinfo {

        private long uid;
        private Base base;
        private Medal medal;
        private String wealth;
        private String title;
        private Guard guard;

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
