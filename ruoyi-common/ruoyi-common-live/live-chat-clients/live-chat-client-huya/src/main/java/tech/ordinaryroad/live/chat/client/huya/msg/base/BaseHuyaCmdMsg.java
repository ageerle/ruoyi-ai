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

package tech.ordinaryroad.live.chat.client.huya.msg.base;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaCmdEnum;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseHuyaCmdMsg extends TarsStructBase implements IHuyaCmdMsg {

    private long lUri;

    @Override
    public String getCmd() {
        return StrUtil.nullToEmpty(StrUtil.toStringOrNull(this.lUri));
    }

    @Override
    public void setCmd(String cmd) {
        this.lUri = NumberUtil.parseLong(cmd);
    }

    @Override
    public HuyaCmdEnum getCmdEnum() {
        return HuyaCmdEnum.getByCode(this.lUri);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }

    @Override
    public String toString() {
        try {
            return BaseMsg.OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new BaseException(e);
        }
    }
}
