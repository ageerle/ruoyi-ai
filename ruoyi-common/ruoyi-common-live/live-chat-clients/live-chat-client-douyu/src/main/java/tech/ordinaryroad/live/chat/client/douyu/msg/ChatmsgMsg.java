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

package tech.ordinaryroad.live.chat.client.douyu.msg;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IDanmuMsg;
import tech.ordinaryroad.live.chat.client.douyu.api.DouyuApis;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;

import java.util.List;

/**
 * <pre>{@code
 * {
 * 	"type": "chatmsg",
 * 	"cmdEnum": "chatmsg",
 * 	"cmd": "chatmsg",
 * 	"nn": "宋老二929",
 * 	"ext": null,
 * 	"bnn": null,
 * 	"level": "1",
 * 	"cst": "1693213418102",
 * 	"brid": "0",
 * 	"bl": "0",
 * 	"dms": "5",
 * 	"rid": "3168536",
 * 	"uid": "396023456",
 * 	"txt": "666",
 * 	"pdg": "47",
 * 	"pdk": "89",
 * 	"sahf": "0",
 * 	"ic": ["avatar_v3", "202101", "45daf5ceb475414293e3da4559552655"],
 * 	"hb": ["2719"],
 * 	"hc": null,
 * 	"cid": "0b37e26cccd54f7c4d73590000000000",
 * 	"lk": null
 * }
 * }</pre>
 *
 * @author mjz
 * @date 2023/8/28
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatmsgMsg extends BaseDouyuCmdMsg implements IDanmuMsg {

    private String nn;
    private String ext;
    private String bnn;
    private String level;
    private String cst;
    private String brid;
    private byte bl;
    private String dms;
    private long rid;
    private String uid;
    private String txt;
    private String pdg;
    private String pdk;
    private String sahf;
    private List<String> ic;
    private List<String> hb;
    private String hc;
    private String cid;
    private JsonNode lk;

    @Override
    public String getType() {
        return DouyuCmdEnum.chatmsg.name();
    }

    @Override
    public String getBadgeName() {
        return this.bnn;
    }

    @Override
    public byte getBadgeLevel() {
        return this.bl;
    }

    @Override
    public String getUsername() {
        return this.nn;
    }

    @Override
    public String getUserAvatar() {
        return DouyuApis.getSmallAvatarUrl(ic);
    }

    @Override
    public String getContent() {
        return this.txt;
    }
}
