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
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GiftPropInfo {

    public static final GiftPropInfo DEFAULT_GIFT = new GiftPropInfo();

    private String name = "未知礼物";
    private String bimg;
    private String himg;
    private String cimg;
    private int stay_time;
    private int show_combo;
    private String batch_preset;
    private List<String> hit;
    private int type;
    private int style;
    private int pc;
    private int ry;
    private int ef;
    private int effect;
    private int is_syn_fs;
    private int is_stay;
    private String drgb;
    private String urgb;
    private String grgb;
    private String brgb;
    private String small_effect_icon;
    private String big_effect_icon;
    private String effect_icon;
    private String gift_icon;
    private String gift_open_icon;
    private int bonus;
    private String web_flash;
    private String hit_interval;
    private int devote;
    private int attack;
    private int exp;
    private int active_type;
    private String pc_full_icon;
    private String pc_full_bg_icon;
    private int pc_show_combo;
    private String pc_urgb;
    private String pc_grgb;
    private int index;

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