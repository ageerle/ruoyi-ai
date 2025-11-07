package org.ruoyi.aihuman.protocol;

import lombok.Getter;

@Getter
public enum MsgType {
    INVALID((byte) 0),
    FULL_CLIENT_REQUEST((byte) 0b1),
    AUDIO_ONLY_CLIENT((byte) 0b10),
    FULL_SERVER_RESPONSE((byte) 0b1001),
    AUDIO_ONLY_SERVER((byte) 0b1011),
    FRONT_END_RESULT_SERVER((byte) 0b1100),
    ERROR((byte) 0b1111);

    private final byte value;

    MsgType(byte value) {
        this.value = value;
    }

    public static MsgType fromValue(int value) {
        for (MsgType type : MsgType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MsgType value: " + value);
    }
}