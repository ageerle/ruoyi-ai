package tech.ordinaryroad.live.chat.client.huya.api;

import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author mjz
 * @date 2023/10/1
 */
class HuyaApisTest {

    @Test
    void roomInit() {
        assertEquals(HuyaApis.roomInit(189201).size(), 3);
        assertThrows(BaseException.class, () -> HuyaApis.roomInit(-1));
    }
}