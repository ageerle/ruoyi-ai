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

package tech.ordinaryroad.live.chat.client.douyu.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseCmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.douyu.config.DouyuLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuConnectionListener;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuMsgListener;
import tech.ordinaryroad.live.chat.client.douyu.msg.ChatmsgMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.DgbMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.UenterMsg;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuConnectionHandler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mjz
 * @date 2023/8/26
 */
@Slf4j
class DouyuLiveChatClientTest implements IDouyuConnectionListener, IDouyuMsgListener {

    static Object lock = new Object();
    DouyuLiveChatClient client;

    private final OkHttpClient okClient = new OkHttpClient();
    private static final String API_KEY = "sk-xxxxxx";
    private static final String URL = "https://api.pandarobot.chat/v1/chat/completions";

    private final Lock reentrantLock = new ReentrantLock();
    private boolean isProcessing = false;


    public String getChatGptResponse(String prompt){
        if (!reentrantLock.tryLock()) {
            // 如果无法立即获得锁，直接返回空字符串
            log.info("自动回复：我还没准备好");
            return "";
        }
        try {
            if (isProcessing) {
                log.info("自动回复：我还没准备好");
                // 如果已经在处理中，直接返回
                return "";
            }
            isProcessing = true;
            // 你的原始代码逻辑
            RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                    "{\"model\": \"gpt-4-0125-preview\", \"messages\": [{\"role\": \"system\", \"content\": \"扮演一个充满机智和幽默感的斗鱼直播弹幕助手，你的任务是用不超过30字的诙谐小段子来回复直播间用户的评论。你的回复应该机智幽默，不夸张能引发笑声，同时也要和谐适应直播间的气氛。当你收到用户的评论时，迅速给出一个既幽默又贴切的回复。例如：评论: 今天的直播怎么样？回复: 好看得让人忘记摸鱼，专心变成鱼粉！\"}, {\"role\": \"user\", \"content\": \"" + prompt + "\"}]}");
            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            long startTime = System.currentTimeMillis(); // 获取开始时间的毫秒数
            Thread.sleep(3000);
            long endTime = System.currentTimeMillis(); // 获取结束时间的毫秒数
            long timeDiff = endTime - startTime; // 计算时间差

            try (Response response = okClient.newCall(request).execute()) {
                if (response.body() != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    ChatCompletionResponse completionResponse = mapper.readValue(response.body().string(), ChatCompletionResponse.class);
                    return completionResponse.getChoices().get(0).getMessage().getContent();
                }
            } catch (Exception e) {
               log.info("调用出错了{}",e.getMessage());
            }
            return "";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            isProcessing = false;
            reentrantLock.unlock();
        }
    }

    @Test
    void example() throws InterruptedException {
        String cookie = "dy_did=621fbe7a636002f6e6cc42eb00091701; acf_did=621fbe7a636002f6e6cc42eb00091701; dy_teen_mode=%7B%22uid%22%3A%22145703733%22%2C%22status%22%3A0%2C%22birthday%22%3A%22%22%2C%22password%22%3A%22%22%7D; dy_did=621fbe7a636002f6e6cc42eb00091701; Hm_lvt_e99aee90ec1b2106afe7ec3b199020a7=1706978160,1707033428,1707109487; PHPSESSID=1orrfq0m0gfrbd58slgh6shqc7; acf_auth=c22dT%2BYJG1xQ3ZuNrfxYNS06lnIjzNl%2F58xziUwyAsNedpJWvbEtIBh%2Bq%2F%2FYxo%2FFYAp9aQr4i4iYqTp2kYdb7n2JNUFBwGEBgZ8GiGS6O%2FR0CNLy8kQ2iVw; dy_auth=d0d0Ps%2FEOPWjXoPGMenITKuiBIC7wTua3heP19LVKzOZ8qibQpmTA9Hpyx%2FdxtDei1Db6KUWkzq1fEC0BLSZVSUbDkuv%2FX%2F%2B0pMu1kgXnk%2FUzQ%2BhV3EiRf8; wan_auth37wan=f0a9656c2e00K9uFDlpgnTfrv3m8aZoYB0WQTWxGSrSccCMd9LUiEUD2oYpJ%2F5Oe5uD2tVpaUA69sGzlTAtl9p3uLSSqgrvH%2F8sy1gWyyrS4fxotQSU; acf_uid=145703733; acf_username=145703733; acf_nickname=%E5%9B%BE%E5%85%94%E5%85%94%E7%9A%84%E5%85%94%E5%85%94%E5%9B%BE; acf_own_room=0; acf_groupid=1; acf_phonestatus=1; acf_avatar=https%3A%2F%2Fapic.douyucdn.cn%2Fupload%2Favanew%2Fface%2F201706%2F25%2F21%2F82973545fd6b770fe03175783ffbc3d5_; acf_ct=0; acf_ltkid=28814433; acf_biz=1; acf_stk=1213d18dc8e6b5b1; Hm_lpvt_e99aee90ec1b2106afe7ec3b199020a7=1707141953";
        DouyuLiveChatClientConfig config = DouyuLiveChatClientConfig.builder()
                // TODO 修改房间id（支持短id）
                .roomId(7828414)
                .cookie(cookie)
                .build();

        client = new DouyuLiveChatClient(config, new IDouyuMsgListener() {
            @Override
            public void onMsg(DouyuBinaryFrameHandler binaryFrameHandler, IMsg msg) {
                IDouyuMsgListener.super.onMsg(binaryFrameHandler, msg);
               // log.debug("{} 收到{}消息 {}", binaryFrameHandler.getRoomId(), msg.getClass(), msg);
            }

            @Override
            public void onDanmuMsg(DouyuBinaryFrameHandler binaryFrameHandler, ChatmsgMsg msg) {
                if(Integer.parseInt(msg.getLevel())>5){
                    String chatGptResponse = getChatGptResponse(msg.getContent());
                    log.info("自动回复："+ "@"+msg.getUsername()+": "+chatGptResponse);
                    if(!Objects.equals(chatGptResponse, "")){
                        client.sendDanmu("@"+msg.getUsername()+": "+chatGptResponse);
                    }
                }
                log.info("{} 收到弹幕 {} {}({})：{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onGiftMsg(DouyuBinaryFrameHandler binaryFrameHandler, DgbMsg msg) {
                IDouyuMsgListener.super.onGiftMsg(binaryFrameHandler, msg);
              //  log.info("{} 收到礼物 {} {}({}) {} {}({})x{}({})", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), "赠送", msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice());
            }

            @Override
            public void onEnterRoomMsg(UenterMsg msg) {
//                if(msg.getLevel()>20){
//                    String chatGptResponse = getChatGptResponse(msg.getUsername()+"加入了直播间,请你帮忙编写简短有创意的欢迎语");
//                    if(!Objects.equals(chatGptResponse, "")){
//                        client.sendDanmu(chatGptResponse);
//                    }
//                }
               log.info("{} {}({}) 进入直播间", msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }

            @Override
            public void onCmdMsg(DouyuBinaryFrameHandler binaryFrameHandler, DouyuCmdEnum cmd, BaseCmdMsg<DouyuCmdEnum> cmdMsg) {
                IDouyuMsgListener.super.onCmdMsg(binaryFrameHandler, cmd, cmdMsg);
                //log.info("{} 收到CMD消息{} {}", binaryFrameHandler.getRoomId(), cmd, cmdMsg);
            }

            @Override
            public void onOtherCmdMsg(DouyuBinaryFrameHandler binaryFrameHandler, DouyuCmdEnum cmd, BaseCmdMsg<DouyuCmdEnum> cmdMsg) {
                IDouyuMsgListener.super.onOtherCmdMsg(binaryFrameHandler, cmd, cmdMsg);

               // log.debug("{} 收到其他CMD消息 {}", binaryFrameHandler.getRoomId(), cmd);
            }

            @Override
            public void onUnknownCmd(DouyuBinaryFrameHandler binaryFrameHandler, String cmdString, BaseMsg msg) {
                IDouyuMsgListener.super.onUnknownCmd(binaryFrameHandler, cmdString, msg);

               // log.debug("{} 收到未知CMD消息 {}", binaryFrameHandler.getRoomId(), cmdString);
            }
        }, new IDouyuConnectionListener() {
            @Override
            public void onConnected(DouyuConnectionHandler connectionHandler) {
                log.info("{} onConnected", connectionHandler.getRoomId());
            }

            @Override
            public void onConnectFailed(DouyuConnectionHandler connectionHandler) {
                log.info("{} onConnectFailed", connectionHandler.getRoomId());
            }

            @Override
            public void onDisconnected(DouyuConnectionHandler connectionHandler) {
                log.info("{} onDisconnected", connectionHandler.getRoomId());
            }
        });
        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void multipyListeners() throws InterruptedException {
        DouyuLiveChatClientConfig config = DouyuLiveChatClientConfig.builder()
                // TODO 修改房间id（支持短id）
                .roomId(74751)
                .build();

        client = new DouyuLiveChatClient(config, null, this);
        client.addMsgListener(new IDouyuMsgListener() {
            @Override
            public void onMsg(DouyuBinaryFrameHandler binaryFrameHandler, IMsg msg) {
                log.info("listener 1 onMsg {}", msg);
            }
        });
        IDouyuMsgListener msgListener2 = new IDouyuMsgListener() {
            @Override
            public void onMsg(DouyuBinaryFrameHandler binaryFrameHandler, IMsg msg) {
                log.info("listener 2 onMsg {}", msg);
            }
        };
        client.addMsgListener(msgListener2);
        AtomicBoolean removed = new AtomicBoolean(false);
        client.addMsgListener(new IDouyuMsgListener() {
            @Override
            public void onMsg(DouyuBinaryFrameHandler binaryFrameHandler, IMsg msg) {
                log.info("listener 3 onMsg {}", msg);
                if (!removed.get()) {
                    log.warn("remove listener 2 by listener 3");
                    removed.set(client.removeMsgListener(msgListener2));
                }
            }
        });
        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void multiplyClient() throws InterruptedException {
        DouyuLiveChatClientConfig config1 = DouyuLiveChatClientConfig.builder().roomId(890074).build();
        DouyuLiveChatClient client1 = new DouyuLiveChatClient(config1, DouyuLiveChatClientTest.this, DouyuLiveChatClientTest.this);

        DouyuLiveChatClientConfig config2 = DouyuLiveChatClientConfig.builder().roomId(718133).build();
        DouyuLiveChatClient client2 = new DouyuLiveChatClient(config2, DouyuLiveChatClientTest.this, DouyuLiveChatClientTest.this);

        client1.connect(() -> {
            log.warn("client1 connect successfully, start connecting client2");
            client2.connect(() -> {
                log.warn("client2 connect successfully");
            });
        });

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void sendDanmu() throws InterruptedException {
        String cookie = "dy_did=621fbe7a636002f6e6cc42eb00091701; acf_did=621fbe7a636002f6e6cc42eb00091701; Hm_lvt_e99aee90ec1b2106afe7ec3b199020a7=1706978160; PHPSESSID=5mg98bkoens5bsg7da6ooj74b7; acf_auth=2a246MCoU1hPn940VxZrs0wQVc8tY96s6rHocobTmUJF8XKjoxWmv51PLf1V4T5g8QiEAyy6u%2BHQfC%2FTNkJudUVdEowNqAF81eaUzHvo5i1MIXBRbDXYXwY; dy_auth=d066mz7%2BE83QjmEJfujfi%2FyOuib9Y2qfBcK7pINX6Gde4HGRaiWZxhNXkqdbLU1aV%2FXaQ0wbkyxV0DddiF9jBc4tMpCMjvi4a1C6EJTfpLHvMmaGBKlI%2FlQ; wan_auth37wan=b747c445309bcOqiaMF72xt346irN4v%2Fhi%2BgabHCNxe812jm9rR8kGws8cdJlsn0C6jxsVKY6We5xWlrjHKesoD2%2B0Av52%2FWzIxaZKT0f30GASOJz58; acf_uid=145703733; acf_username=145703733; acf_nickname=%E5%9B%BE%E5%85%94%E5%85%94%E7%9A%84%E5%85%94%E5%85%94%E5%9B%BE; acf_own_room=0; acf_groupid=1; acf_phonestatus=1; acf_avatar=https%3A%2F%2Fapic.douyucdn.cn%2Fupload%2Favanew%2Fface%2F201706%2F25%2F21%2F82973545fd6b770fe03175783ffbc3d5_; acf_ct=0; acf_ltkid=28814429; acf_biz=1; acf_stk=0fad5f2454193f86; dy_teen_mode=%7B%22uid%22%3A%22145703733%22%2C%22status%22%3A0%2C%22birthday%22%3A%22%22%2C%22password%22%3A%22%22%7D; dy_did=621fbe7a636002f6e6cc42eb00091701; acf_ccn=5ba506efe2d8a939cf09f8597c7d8030; Hm_lpvt_e99aee90ec1b2106afe7ec3b199020a7=1706978671";
       // log.error("cookie: {}", cookie);
        DouyuLiveChatClientConfig config = DouyuLiveChatClientConfig.builder()
                .cookie(cookie)
                // TODO 修改弹幕发送最短时间间隔，默认3s
                .minSendDanmuPeriod(10 * 1000)
                // TODO 修改房间id（支持短id）
                .roomId(4624967)
                .build();
        DouyuWsLiveChatClient client = new DouyuWsLiveChatClient(config, new IDouyuMsgListener() {
            @Override
            public void onMsg(IMsg msg) {
                IDouyuMsgListener.super.onMsg(msg);

//                log.debug("收到消息 {}", msg.getClass());
            }

            @Override
            public void onCmdMsg(DouyuCmdEnum cmd, BaseCmdMsg<DouyuCmdEnum> cmdMsg) {
                log.debug("收到CMD消息 {} {}", cmd, cmdMsg);
            }

            @Override
            public void onEnterRoomMsg(UenterMsg msg) {
                log.info("{} {}({}) 进入直播间", msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }
            @Override
            public void onUnknownCmd(String cmdString, BaseMsg msg) {
                IDouyuMsgListener.super.onUnknownCmd(cmdString, msg);

                log.debug("收到未知CMD消息 {} {}", cmdString, msg);
            }
        }, new IDouyuConnectionListener() {
            @Override
            public void onConnected(DouyuConnectionHandler connectionHandler) {
                log.error("{} onConnected", connectionHandler.getRoomId());
            }

            @Override
            public void onConnectFailed(DouyuConnectionHandler connectionHandler) {
                log.error("{} onConnectFailed", connectionHandler.getRoomId());
            }

            @Override
            public void onDisconnected(DouyuConnectionHandler connectionHandler) {
                log.error("{} onDisconnected", connectionHandler.getRoomId());
            }
        }, new NioEventLoopGroup());
        client.connect(() -> {
            client.sendDanmu("主播真好看");
        });

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void createAuthFrameFailedAndDisconnect() throws InterruptedException {
        DouyuLiveChatClientConfig config = DouyuLiveChatClientConfig.builder()
                // TODO 修改房间id（支持短id）
                .autoReconnect(false)
                .cookie("12323232'123'213'2'13'2")
                .roomId(22222)
                .build();

        client = new DouyuLiveChatClient(config, this, this);
        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void autoReconnect() throws InterruptedException {
        DouyuLiveChatClientConfig config = DouyuLiveChatClientConfig.builder()
                // TODO 修改房间id（支持短id）
                .autoReconnect(true)
                .cookie("12323232'123'213'2'13'2")
//                .websocketUri("wss://sa.asd.asd:12")
                .roomId(22222)
                .build();

        client = new DouyuLiveChatClient(config, this, this);
        client.addStatusChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Object oldStatus = evt.getOldValue();
                Object newStatus = evt.getNewValue();
                log.error("{} => {}", oldStatus, newStatus);
            }
        });
        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Override
    public void onConnected(DouyuConnectionHandler connectionHandler) {
        log.info("{} onConnected", connectionHandler.getRoomId());
    }

    @Override
    public void onConnectFailed(DouyuConnectionHandler connectionHandler) {
        log.info("{} onConnectFailed", connectionHandler.getRoomId());
    }

    @Override
    public void onDisconnected(DouyuConnectionHandler connectionHandler) {
        log.info("{} onDisconnected", connectionHandler.getRoomId());
    }

    @Override
    public void onDanmuMsg(DouyuBinaryFrameHandler binaryFrameHandler, ChatmsgMsg msg) {
        IDouyuMsgListener.super.onDanmuMsg(binaryFrameHandler, msg);

        log.info("{} 收到弹幕 {}({})：{}", binaryFrameHandler.getRoomId(), msg.getNn(), msg.getUid(), msg.getTxt());
    }
}
