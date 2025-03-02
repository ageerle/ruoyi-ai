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

package tech.ordinaryroad.live.chat.client.huya.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ByteUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaDecorationAppTypeEnum;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaDecorationViewTypeEnum;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaWupFunctionEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.*;
import tech.ordinaryroad.live.chat.client.huya.msg.base.IHuyaMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.BadgeInfo;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.DecorationInfo;
import tech.ordinaryroad.live.chat.client.huya.msg.req.WupReq;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author mjz
 * @date 2023/9/5
 */
@Slf4j
public class HuyaCodecUtil {

    public static List<IHuyaMsg> decode(ByteBuf in) {
        List<IHuyaMsg> msgList = new ArrayList<>();
        Queue<ByteBuf> pendingByteBuf = new LinkedList<>();

        do {
            msgList.addAll(doDecode(in, pendingByteBuf));
            in = pendingByteBuf.poll();
        } while (in != null);

        return msgList;
    }

    /**
     * 执行解码操作，有压缩则先解压，解压后可能得到多条消息
     *
     * @param in             handler收到的一条消息
     * @param pendingByteBuf 用于存放未读取完的ByteBuf
     */
    private static List<? extends IHuyaMsg> doDecode(ByteBuf in, Queue<ByteBuf> pendingByteBuf) {
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);

        WebSocketCommand webSocketCommand = new WebSocketCommand(newUtf8TarsInputStream(bytes));
        HuyaOperationEnum operationEnum = webSocketCommand.getOperationEnum();
        if (operationEnum == null) {
            throw new BaseException(String.format("未知operation: %d", webSocketCommand.getOperation()));
        }

        switch (operationEnum) {
            case EWSCmd_RegisterRsp: {
                return Collections.singletonList(new RegisterRsp(newUtf8TarsInputStream(webSocketCommand.getVData())));
            }
            case EWSCmdS2C_RegisterGroupRsp: {
                return Collections.singletonList(new RegisterGroupRsp(newUtf8TarsInputStream(webSocketCommand.getVData())));
            }
            case EWSCmd_WupRsp: {
                return Collections.singletonList(new WupRsp(webSocketCommand.getVData()));
            }
            case EWSCmdS2C_MsgPushReq: {
                return Collections.singletonList(new PushMessage(newUtf8TarsInputStream(webSocketCommand.getVData())));
            }
            case EWSCmdS2C_VerifyCookieRsp: {
                return Collections.singletonList(new VerifyCookieRsp(newUtf8TarsInputStream(webSocketCommand.getVData())));
            }
            case EWSCmdS2C_MsgPushReq_V2: {
                PushMessage_V2 pushMessageV2 = new PushMessage_V2(newUtf8TarsInputStream(webSocketCommand.getVData()));
                return pushMessageV2.getVMsgItem();
            }
            default: {
                return Collections.singletonList(webSocketCommand);
            }
        }
    }

    public static byte[] encode(String servantName, HuyaWupFunctionEnum function, TarsStructBase req) {
        WupReq wupReq = new WupReq();
        wupReq.getTarsServantRequest().setServantName(servantName);
        wupReq.getTarsServantRequest().setFunctionName(function.name());
        wupReq.getUniAttribute().put("tReq", req);
        return wupReq.encode();
    }

    public static TarsInputStream newUtf8TarsInputStream(byte[] bytes) {
        TarsInputStream tarsInputStream = new TarsInputStream(bytes);
        tarsInputStream.setServerEncoding(StandardCharsets.UTF_8.name());
        return tarsInputStream;
    }

    public static Optional<? extends TarsStructBase> decodeDecorationInfo(DecorationInfo decorationInfo) {
        int iViewType = decorationInfo.getIViewType();
        HuyaDecorationViewTypeEnum huyaDecorationViewTypeEnum = HuyaDecorationViewTypeEnum.getByCode(iViewType);
        if (huyaDecorationViewTypeEnum == null) {
            return Optional.empty();
        }

        switch (huyaDecorationViewTypeEnum) {
            case kDecorationViewTypeCustomized: {
                int iAppId = decorationInfo.getIAppId();
                HuyaDecorationAppTypeEnum huyaDecorationAppTypeEnum = HuyaDecorationAppTypeEnum.getByCode(iAppId);
                if (huyaDecorationAppTypeEnum == null) {
                    return Optional.empty();
                }

                switch (huyaDecorationAppTypeEnum) {
                    case kDecorationAppTypeFans: {
                        BadgeInfo badgeInfo = new BadgeInfo();
                        badgeInfo.readFrom(HuyaCodecUtil.newUtf8TarsInputStream(decorationInfo.getVData()));
                        return Optional.of(badgeInfo);
                    }
                    default: {
                        return Optional.empty();
                    }
                }
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public static String ab2str(byte[] bytes) {
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            int unsignedInt = ByteUtil.byteToUnsignedInt(bytes[i]);
            chars[i] = (char) unsignedInt;
        }
        return ArrayUtil.join(chars, "");
    }

    public static String btoa(String string) {
        return Base64.encode(string);
    }
}
