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

package tech.ordinaryroad.live.chat.client.douyin.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.GlobalHeaders;
import cn.hutool.http.Header;
import cn.hutool.http.HttpUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.douyin.api.DouyinApis;
import tech.ordinaryroad.live.chat.client.douyin.config.DouyinLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.douyin.constant.DouyinCmdEnum;
import tech.ordinaryroad.live.chat.client.douyin.listener.IDouyinConnectionListener;
import tech.ordinaryroad.live.chat.client.douyin.listener.IDouyinMsgListener;
import tech.ordinaryroad.live.chat.client.douyin.msg.base.IDouyinMsg;
import tech.ordinaryroad.live.chat.client.douyin.netty.handler.DouyinBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.douyin.netty.handler.DouyinConnectionHandler;
import tech.ordinaryroad.live.chat.client.servers.netty.client.base.BaseNettyClient;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author mjz
 * @date 2024/1/2
 */
@Slf4j
public class DouyinLiveChatClient extends BaseNettyClient<
        DouyinLiveChatClientConfig,
        DouyinCmdEnum,
        IDouyinMsg,
        IDouyinMsgListener,
        DouyinConnectionHandler,
        DouyinBinaryFrameHandler> {

    private DouyinApis.RoomInitResult roomInitResult = new DouyinApis.RoomInitResult();

    public DouyinLiveChatClient(DouyinLiveChatClientConfig config, List<IDouyinMsgListener> msgListeners, IDouyinConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        addMsgListeners(msgListeners);

        // 初始化
        this.init();
    }

    public DouyinLiveChatClient(DouyinLiveChatClientConfig config, IDouyinMsgListener msgListener, IDouyinConnectionListener connectionListener, EventLoopGroup workerGroup) {
        super(config, workerGroup, connectionListener);
        addMsgListener(msgListener);

        // 初始化
        this.init();
    }

    public DouyinLiveChatClient(DouyinLiveChatClientConfig config, IDouyinMsgListener msgListener, IDouyinConnectionListener connectionListener) {
        this(config, msgListener, connectionListener, new NioEventLoopGroup());
    }

    public DouyinLiveChatClient(DouyinLiveChatClientConfig config, IDouyinMsgListener msgListener) {
        this(config, msgListener, null, new NioEventLoopGroup());
    }

    public DouyinLiveChatClient(DouyinLiveChatClientConfig config) {
        this(config, null);
    }

    @Override
    public void init() {
        roomInitResult = DouyinApis.roomInit(getConfig().getRoomId(), getConfig().getCookie());
        super.init();
    }

    @Override
    public DouyinConnectionHandler initConnectionHandler(IBaseConnectionListener<DouyinConnectionHandler> clientConnectionListener) {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add(Header.COOKIE.name(), DouyinApis.KEY_COOKIE_TTWID + "=" + roomInitResult.getTtwid());
        headers.add(Header.USER_AGENT.name(), GlobalHeaders.INSTANCE.header(Header.USER_AGENT));
        return new DouyinConnectionHandler(
                WebSocketClientHandshakerFactory.newHandshaker(getWebsocketUri(), WebSocketVersion.V13, null, true, headers, getConfig().getMaxFramePayloadLength()),
                DouyinLiveChatClient.this, clientConnectionListener
        );
    }

    @Override
    public DouyinBinaryFrameHandler initBinaryFrameHandler() {
        return new DouyinBinaryFrameHandler(super.msgListeners, DouyinLiveChatClient.this);
    }

    @Override
    protected String getWebSocketUriString() {
        long realRoomId = roomInitResult.getRealRoomId();
        String userUniqueId = roomInitResult.getUserUniqueId();

        String webSocketUriString = super.getWebSocketUriString();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("app_name", "douyin_web");
        queryParams.put("version_code", getConfig().getVersionCode());
        queryParams.put("webcast_sdk_version", getConfig().getWebcastSdkVersion());
        queryParams.put("update_version_code", getConfig().getUpdateVersionCode());
        queryParams.put("compress", "gzip");
        queryParams.put("device_platform", "web");
        queryParams.put("cookie_enabled", "true");
        queryParams.put("screen_width", "800");
        queryParams.put("screen_height", "1280");
        queryParams.put("browser_language", "zh-CN");
        queryParams.put("browser_platform", "MacIntel");
        queryParams.put("browser_name", "Mozilla");
        queryParams.put("browser_version", "5.0%20(Macintosh;%20Intel%20Mac%20OS%20X%2010_15_7)%20AppleWebKit/537.36%20(KHTML,%20like%20Gecko)%20Chrome/116.0.0.0%20Safari/537.36");
        queryParams.put("browser_online", "true");
        queryParams.put("tz_name", "Asia/Shanghai");
        queryParams.put("host", "https://live.douyin.com");
        queryParams.put("im_path", "/webcast/im/fetch/");
        queryParams.put("endpoint", "live_pc");
        queryParams.put("identity", "audience");

        queryParams.put("support_wrds", "1");
        queryParams.put("heartbeatDuration ", "0");
        queryParams.put("live_id", "1");
        queryParams.put("did_rule", "3");
        queryParams.put("aid", "6383");

        queryParams.put("room_id", Long.toString(realRoomId));
        queryParams.put("user_unique_id", userUniqueId);
        // TODO 生成signature
        queryParams.put("signature", "00000000");
        queryParams.put("cursor", "t-" + System.currentTimeMillis() + "_r-1_d-1_u-1_h-1");
        queryParams.put("internal_ext", "internal_src:dim|" +
                "wss_push_room_id:" + realRoomId + "|" +
                "wss_push_did:" + userUniqueId + "|" +
                "dim_log_id:" + DateUtil.format(new Date(), "yyyy-MM-dd") + RandomUtil.randomNumbers(6) + RandomUtil.randomString("0123456789ABCDEF", 20) + "|" +
                "first_req_ms:" + System.currentTimeMillis() + "|" +
                "fetch_time:" + System.currentTimeMillis() + "|" +
                "seq:1|" +
                "wss_info:0-" + System.currentTimeMillis() + "-0-0|" +
                "wrds_kvs:WebcastRoomStatsMessage-" + System.nanoTime() + "_WebcastRoomRankMessage-" + System.nanoTime() + "_LotteryInfoSyncData-" + System.nanoTime() + "_WebcastActivityEmojiGroupsMessage-" + System.nanoTime());
        return webSocketUriString + "?" + HttpUtil.toParams(queryParams);
    }

    public void sendDanmu(Object danmu, Runnable success, Consumer<Throwable> failed) {
        super.sendDanmu(danmu, success, failed);
    }

}
