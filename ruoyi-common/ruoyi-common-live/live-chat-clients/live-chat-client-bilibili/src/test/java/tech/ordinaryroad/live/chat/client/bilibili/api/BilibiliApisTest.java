package tech.ordinaryroad.live.chat.client.bilibili.api;

import org.junit.jupiter.api.Test;

/**
 * @author mjz
 * @date 2023/9/7
 */
class BilibiliApisTest {

    @Test
    void sendMsg() {
        String cookie = System.getenv("cookie");
        BilibiliApis.sendMsg("666", 545068, cookie);
    }
}