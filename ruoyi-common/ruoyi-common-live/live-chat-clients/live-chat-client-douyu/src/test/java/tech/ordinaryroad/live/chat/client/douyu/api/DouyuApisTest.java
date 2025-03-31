package tech.ordinaryroad.live.chat.client.douyu.api;

import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.douyu.msg.dto.GiftPropSingle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author mjz
 * @date 2023/8/30
 */
class DouyuApisTest {

    @Test
    void getRealRoomId() {
        assertEquals(2947432, DouyuApis.getRealRoomId(92000));
        assertEquals(3168536, DouyuApis.getRealRoomId(3168536));
        assertEquals(290935, DouyuApis.getRealRoomId(22222));
        assertEquals(290935, DouyuApis.getRealRoomId(290935));
        assertEquals(520, DouyuApis.getRealRoomId(520));
        assertThrows(RuntimeException.class, () -> DouyuApis.getRealRoomId(-1));
    }

    @Test
    void getGiftInfo() {
        GiftPropSingle giftByPid = DouyuApis.getGiftPropSingleByPid("4");
        assertEquals("赞", giftByPid.getName());
    }
}