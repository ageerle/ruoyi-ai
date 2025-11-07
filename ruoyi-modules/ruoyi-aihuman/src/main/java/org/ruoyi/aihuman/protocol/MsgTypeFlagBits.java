package org.ruoyi.aihuman.protocol;

import lombok.Getter;

@Getter
public enum MsgTypeFlagBits {
    NO_SEQ((byte) 0),           // Non-terminating packet without sequence number
    POSITIVE_SEQ((byte) 0b1),   // Non-terminating packet with positive sequence number
    LAST_NO_SEQ((byte) 0b10),   // Terminating packet without sequence number
    NEGATIVE_SEQ((byte) 0b11),  // Terminating packet with negative sequence number
    WITH_EVENT((byte) 0b100);   // Packet containing event number

    private final byte value;

    MsgTypeFlagBits(byte value) {
        this.value = value;
    }

    public static MsgTypeFlagBits fromValue(int value) {
        for (MsgTypeFlagBits flag : MsgTypeFlagBits.values()) {
            if (flag.value == value) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown MsgTypeFlagBits value: " + value);
    }
} 