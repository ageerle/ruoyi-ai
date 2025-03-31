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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;

/**
 * type@=chatmsg/rid@=7750753/uid@=549977/nn@=一闪一闪亮品品/txt@=冷门歌手哎，声音独特哎/cid@=044c3bf3074b483275a44e0000000000/ic@=avatar@Sdefault@S08/level@=29/sahf@=0/nl@=7/cst@=1693107871294/bnn@=/bl@=0/brid@=0/hc@=/lk@=/dms@=8/pdg@=26/pdk@=86/ext@=/
 *
 * @see tech.ordinaryroad.live.chat.client.douyu.util.DouyuCodecUtil#getDouyuMsgClassByType(DouyuCmdEnum, short)
 * @author mjz
 * @date 2023/1/6
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DouyuCmdMsg extends BaseDouyuCmdMsg {

    private String tick;

    private String res;

}
