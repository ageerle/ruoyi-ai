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

package tech.ordinaryroad.live.chat.client.huya.msg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.support.TarsMethodInfo;
import com.qq.tars.protocol.util.TarsHelper;
import com.qq.tars.rpc.protocol.tars.TarsServantRequest;
import com.qq.tars.rpc.protocol.tup.UniAttribute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaMsg;
import tech.ordinaryroad.live.chat.client.huya.util.HuyaCodecUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseWup extends BaseHuyaMsg {

    private TarsServantRequest tarsServantRequest = new TarsServantRequest(null) {{
        setMethodInfo(new TarsMethodInfo());
    }};
    private UniAttribute uniAttribute = new UniAttribute();

    public BaseWup(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
//        os.write(this.tarsServantRequest.getVersion(), 1);
        os.write(TarsHelper.VERSION3, 1);
        os.write(this.tarsServantRequest.getPacketType(), 2);
        os.write(this.tarsServantRequest.getMessageType(), 3);
        os.write(this.tarsServantRequest.getRequestId(), 4);
        os.write(this.tarsServantRequest.getServantName(), 5);
        os.write(this.tarsServantRequest.getFunctionName(), 6);
        os.write(this.uniAttribute.encode(), 7);
        os.write(this.tarsServantRequest.getTimeout(), 8);
        os.write(this.tarsServantRequest.getContext(), 9);
        os.write(this.tarsServantRequest.getStatus(), 10);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.tarsServantRequest.setVersion(is.read(this.tarsServantRequest.getVersion(), 1, false));
        this.tarsServantRequest.setPacketType(is.read(this.tarsServantRequest.getPacketType(), 2, false));
        this.tarsServantRequest.setMessageType(is.read(this.tarsServantRequest.getMessageType(), 3, false));
        this.tarsServantRequest.setRequestId(is.read(this.tarsServantRequest.getRequestId(), 4, false));
        this.tarsServantRequest.setServantName(is.read(this.tarsServantRequest.getServantName(), 5, false));
        this.tarsServantRequest.setFunctionName(is.read(this.tarsServantRequest.getFunctionName(), 6, false));
        this.uniAttribute.decode(is.read(new byte[]{}, 7, false));
        this.tarsServantRequest.setTimeout(is.read(this.tarsServantRequest.getTimeout(), 8, false));
        this.tarsServantRequest.setContext(is.readMap(this.tarsServantRequest.getContext(), 9, false));
        this.tarsServantRequest.setStatus(is.readMap(this.tarsServantRequest.getStatus(), 10, false));
    }

    public byte[] encode() {
        TarsOutputStream wupTarsOutputStream = new TarsOutputStream();
        this.writeTo(wupTarsOutputStream);

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(4 + wupTarsOutputStream.getByteBuffer().position());
        buffer.writeBytes(wupTarsOutputStream.toByteArray());

        return ByteBufUtil.getBytes(buffer);
    }

    public void decode(byte[] bytes) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        int size = byteBuf.readInt();
        if (size < 4) {
            return;
        }

        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        this.readFrom(HuyaCodecUtil.newUtf8TarsInputStream(bytes));
    }

    @Override
    public String toString() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", this.tarsServantRequest.getVersion());
        map.put("packetType", this.tarsServantRequest.getPacketType());
        map.put("messageType", this.tarsServantRequest.getMessageType());
        map.put("requestId", this.tarsServantRequest.getRequestId());
        map.put("servantName", this.tarsServantRequest.getServantName());
        map.put("functionName", this.tarsServantRequest.getFunctionName());
        map.put("timeout", this.tarsServantRequest.getTimeout());
        map.put("context", this.tarsServantRequest.getContext());
        map.put("status", this.tarsServantRequest.getStatus());
        try {
            return BaseMsg.OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BaseException(e);
        }
    }
}
