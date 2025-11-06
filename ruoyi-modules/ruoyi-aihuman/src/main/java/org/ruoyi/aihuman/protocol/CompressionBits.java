package com.speech.protocol;

import lombok.Getter;

@Getter
public enum CompressionBits {
    None_((byte) 0),
    Gzip((byte) 0b1),
    Custom((byte) 0b11),
    ;

    private final byte value;

    CompressionBits(byte b) {
        this.value = b;
    }

    public static CompressionBits fromValue(int value) {
        for (CompressionBits type : CompressionBits.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CompressionBits value: " + value);
    }
}
