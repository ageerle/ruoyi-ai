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

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.bilibili.constant.OperationEnum;
import tech.ordinaryroad.live.chat.client.bilibili.msg.base.BaseBilibiliMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IDanmuMsg;

/**
 * @author mjz
 * @date 2023/9/8
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DanmuMsgMsg extends BaseBilibiliMsg implements IDanmuMsg {

    private JsonNode info;
    private String dm_v2;

    @Override
    public OperationEnum getOperationEnum() {
        return OperationEnum.SEND_SMS_REPLY;
    }

    @Override
    public String getBadgeName() {
        JsonNode jsonNode3 = info.get(3);
        if (jsonNode3.isEmpty()) {
            return "";
        }
        return jsonNode3.get(1).asText();
    }

    @Override
    public byte getBadgeLevel() {
        JsonNode jsonNode3 = info.get(3);
        if (jsonNode3.isEmpty()) {
            return 0;
        }
        return (byte) jsonNode3.get(0).asInt();
    }

    @Override
    public String getUid() {
        JsonNode jsonNode2 = info.get(2);
        return jsonNode2.get(0).asText();
    }

    @Override
    public String getUsername() {
        JsonNode jsonNode2 = info.get(2);
        return jsonNode2.get(1).asText();
    }

    @Override
    public String getUserAvatar() {
        String avatar = null;
        try {
            tech.ordinaryroad.live.chat.client.bilibili.protobuf.dm_v2 dmV2 = tech.ordinaryroad.live.chat.client.bilibili.protobuf.dm_v2.parseFrom(Base64.decode(dm_v2));
            avatar = dmV2.getDmV220().getAvatar();
        } catch (Exception e) {
            // ignore
        }
        return avatar;
    }

    @Override
    public String getContent() {
        JsonNode jsonNode1 = info.get(1);
        return jsonNode1.asText();
    }
}
