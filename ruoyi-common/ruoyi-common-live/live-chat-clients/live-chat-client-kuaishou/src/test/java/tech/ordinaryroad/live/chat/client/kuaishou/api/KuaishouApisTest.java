package tech.ordinaryroad.live.chat.client.kuaishou.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author mjz
 * @date 2024/1/6
 */
class KuaishouApisTest {

    @Test
    void allgifts() {
        Map<String, KuaishouApis.GiftInfo> allgifts = KuaishouApis.allgifts();
        assertNotEquals(0, allgifts.size());
    }

    @Test
    void getGiftInfoById() {
        KuaishouApis.GiftInfo giftInfoById = KuaishouApis.getGiftInfoById("1");
        assertEquals("荧光棒", giftInfoById.getGiftName());

    }

    @Test
    void sendComment() {
        System.out.println(KuaishouApis.sendComment(System.getenv("cookie"),
                "3x6pb6bcmjrarvs",
                KuaishouApis.SendCommentRequest
                        .builder()
                        .liveStreamId("XKLoBv2mAEo")
                        .content("666666a")
                        .build()
        ));
    }
}