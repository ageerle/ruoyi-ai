package org.ruoyi.enums;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatModeType enum
 */
@Tag("dev")
class ChatModeTypeTest {

    @Test
    void minimaxEnumExists() {
        ChatModeType minimax = ChatModeType.MINIMAX;
        assertNotNull(minimax);
    }

    @Test
    void difyEnumExists() {
        ChatModeType dify = ChatModeType.DIFY;
        assertNotNull(dify);
    }

    @Test
    void cozeEnumExists() {
        ChatModeType coze = ChatModeType.COZE;
        assertNotNull(coze);
    }

    @Test
    void minimaxCode_isMinimax() {
        assertEquals("minimax", ChatModeType.MINIMAX.getCode());
    }

    @Test
    void minimaxDescription_isMiniMax() {
        assertEquals("MiniMax", ChatModeType.MINIMAX.getDescription());
    }

    @Test
    void difyDescription_isDify() {
        assertEquals("Dify", ChatModeType.DIFY.getDescription());
    }

    @Test
    void cozeDescription_isCoze() {
        assertEquals("Coze", ChatModeType.COZE.getDescription());
    }

    @Test
    void allProviders_haveUniqueCode() {
        ChatModeType[] values = ChatModeType.values();
        long uniqueCodes = java.util.Arrays.stream(values)
            .map(ChatModeType::getCode)
            .distinct()
            .count();
        assertEquals(values.length, uniqueCodes, "All providers must have unique codes");
    }

    @Test
    void valueOf_minimax() {
        assertEquals(ChatModeType.MINIMAX, ChatModeType.valueOf("MINIMAX"));
    }
}
