package org.ruoyi.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatModeType enum
 */
class ChatModeTypeTest {

    @Test
    void minimaxEnumExists() {
        ChatModeType minimax = ChatModeType.MINIMAX;
        assertNotNull(minimax);
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
