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

package tech.ordinaryroad.live.chat.client.kuaishou.msg;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IDanmuMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.base.IKuaishouMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.WebCommentFeedOuterClass;

import java.util.List;

/**
 * @author mjz
 * @date 2024/1/9
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KuaishouDanmuMsg implements IKuaishouMsg, IDanmuMsg {

    private WebCommentFeedOuterClass.WebCommentFeed msg;

    @Override
    public String getBadgeName() {
        String badgeName = null;
        try {
            UnknownFieldSet.Field field21 = msg.getSenderState().getUnknownFields().asMap().get(21);
            List<ByteString> lengthDelimitedList = field21.getLengthDelimitedList();
            if (!lengthDelimitedList.isEmpty()) {
                UnknownFieldSet.Field field21_1 = UnknownFieldSet.parseFrom(
                        lengthDelimitedList.size() > 1 ? lengthDelimitedList.get(1) : lengthDelimitedList.get(0)
                ).getField(1);
                List<ByteString> lengthDelimitedList1 = field21_1.getLengthDelimitedList();
                if (!lengthDelimitedList1.isEmpty()) {
                    UnknownFieldSet.Field field21_1_4 = UnknownFieldSet.parseFrom((lengthDelimitedList1.get(0))).getField(4);
                    List<ByteString> lengthDelimitedList2 = field21_1_4.getLengthDelimitedList();
                    if (!lengthDelimitedList2.isEmpty()) {
                        badgeName = lengthDelimitedList2.get(0).toStringUtf8();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return badgeName;
    }

    @Override
    public byte getBadgeLevel() {
        return (byte) msg.getSenderState().getLiveFansGroupState().getIntimacyLevel();
    }

    @Override
    public String getUid() {
        return msg.getUser().getPrincipalId();
    }

    @Override
    public String getUsername() {
        return msg.getUser().getUserName();
    }

    @Override
    public String getUserAvatar() {
        return msg.getUser().getHeadUrl();
    }

    @Override
    public String getContent() {
        return msg.getContent();
    }
}
