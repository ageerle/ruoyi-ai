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

package tech.ordinaryroad.live.chat.client.huya.msg.req;

import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.PropsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mjz
 * @date 2023/10/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetPropsListRsp extends TarsStructBase {

    private List<PropsItem> vPropsItemList = new ArrayList<PropsItem>() {{
        add(new PropsItem());
    }};
    private String sMd5 = "";
    private short iNewEffectSwitch = 0;
    private short iMirrorRoomShowNum = 0;
    private short iGameRoomShowNum = 0;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.vPropsItemList, 1);
        os.write(this.sMd5, 2);
        os.write(this.iNewEffectSwitch, 3);
        os.write(this.iMirrorRoomShowNum, 4);
        os.write(this.iGameRoomShowNum, 5);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.vPropsItemList = is.readArray(this.vPropsItemList, 1, true);
        this.sMd5 = is.read(this.sMd5, 2, true);
        this.iNewEffectSwitch = is.read(this.iNewEffectSwitch, 3, true);
        this.iMirrorRoomShowNum = is.read(this.iMirrorRoomShowNum, 4, true);
        this.iGameRoomShowNum = is.read(this.iGameRoomShowNum, 5, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
