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

package tech.ordinaryroad.live.chat.client.huya.msg.dto;

import cn.hutool.core.collection.CollUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author mjz
 * @date 2023/12/27
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DecorationInfoRsp extends TarsStructBase {

    private List<DecorationInfo> vDecorationPrefix = CollUtil.newArrayList(new DecorationInfo());
    private List<DecorationInfo> vDecorationSuffix = CollUtil.newArrayList(new DecorationInfo());
    private ContentFormat tFormat = new ContentFormat();
    private BulletFormat tBulletFormat = new BulletFormat();
    private List<ChannelPair> vForwardChannels = CollUtil.newArrayList(new ChannelPair());
    private int iModifyMask;
    private List<DecorationInfo> vBulletPrefix = CollUtil.newArrayList(new DecorationInfo());
    private SenderInfo tUserInfo = new SenderInfo();
    private List<DecorationInfo> vBulletSuffix = CollUtil.newArrayList(new DecorationInfo());

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.vDecorationPrefix, 0);
        os.write(this.vDecorationSuffix, 1);
        os.write(this.tFormat, 2);
        os.write(this.tBulletFormat, 3);
        os.write(this.vForwardChannels, 4);
        os.write(this.iModifyMask, 5);
        os.write(this.vBulletPrefix, 6);
        os.write(this.tUserInfo, 7);
        os.write(this.vBulletSuffix, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.vDecorationPrefix = is.readArray(this.vDecorationPrefix, 0, false);
        this.vDecorationSuffix = is.readArray(this.vDecorationSuffix, 1, false);
        this.tFormat = (ContentFormat) is.directRead(this.tFormat, 2, false);
        this.tBulletFormat = (BulletFormat) is.directRead(this.tBulletFormat, 3, false);
        this.vForwardChannels = is.readArray(this.vForwardChannels, 4, false);
        this.iModifyMask = is.read(this.iModifyMask, 5, false);
        this.vBulletPrefix = is.readArray(this.vBulletPrefix, 6, false);
        this.tUserInfo = (SenderInfo) is.directRead(this.tUserInfo, 7, false);
        this.vBulletSuffix = is.readArray(this.vBulletSuffix, 8, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
