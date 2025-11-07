package org.ruoyi.aihuman.protocol;

import lombok.Getter;

@Getter
public enum SerializationBits {
    Raw((byte) 0),
    JSON((byte) 0b1),
    Thrift((byte) 0b11),
    Custom((byte) 0b1111),
    ;

    private final byte value;

    SerializationBits(byte b) {
        this.value = b;
    }

    public static SerializationBits fromValue(int value) {
        for (SerializationBits type : SerializationBits.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SerializationBits value: " + value);
    }
}
