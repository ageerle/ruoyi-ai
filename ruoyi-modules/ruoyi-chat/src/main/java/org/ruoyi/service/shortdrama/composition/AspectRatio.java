package org.ruoyi.service.shortdrama.composition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AspectRatio {
    PORTRAIT("9:16", 1080, 1920),
    LANDSCAPE("16:9", 1920, 1080),
    LANDSCAPE_CLASSIC("4:3", 1440, 1080),
    SQUARE("1:1", 1080, 1080),
    PORTRAIT_CLASSIC("3:4", 1080, 1440),
    ULTRAWIDE("21:9", 2520, 1080);

    private final String value;
    private final int width;
    private final int height;

    AspectRatio(String value, int width, int height) {
        this.value = value;
        this.width = width;
        this.height = height;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @JsonCreator
    public static AspectRatio fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PORTRAIT;
        }
        for (AspectRatio ratio : values()) {
            if (ratio.value.equals(value.trim())) {
                return ratio;
            }
        }
        throw new IllegalArgumentException("Unsupported aspect ratio: " + value);
    }
}
