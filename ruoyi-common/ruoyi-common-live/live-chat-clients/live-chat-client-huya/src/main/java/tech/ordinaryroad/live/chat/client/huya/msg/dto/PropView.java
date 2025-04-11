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

import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mjz
 * @date 2023/10/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropView extends TarsStructBase {

    private int id = 0;
    private String name = "";
    private Map<Long, Short> uids = new HashMap<Long, Short>() {{
        put(-1L, (short) -1);
    }};
    private String tips = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.id, 0);
        os.write(this.name, 1);
        os.write(this.uids, 2);
        os.write(this.tips, 3);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.id = is.read(this.id, 0, true);
        this.name = is.read(this.name, 1, true);
        this.uids = is.readMap(this.uids, 2, true);
        this.tips = is.read(this.tips, 3, true);
    }
}
