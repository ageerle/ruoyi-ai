package org.ruoyi.aihuman.protocol;

import lombok.Getter;

@Getter
public enum VersionBits {
    Version1((byte) 1),
    Version2((byte) 2),
    Version3((byte) 3),
    Version4((byte) 4),
    ;

    private final byte value;

    VersionBits(byte b) {
        this.value = b;
    }

    public static VersionBits fromValue(int value) {
        for (VersionBits type : VersionBits.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown VersionBits value: " + value);
    }
}
