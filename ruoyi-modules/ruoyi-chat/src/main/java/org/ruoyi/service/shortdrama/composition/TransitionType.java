package org.ruoyi.service.shortdrama.composition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TransitionType {
    NONE(null),
    DISSOLVE("dissolve"),
    FADE("fadeblack"),
    SLIDE("slideleft");

    private final String ffmpegName;

    TransitionType(String ffmpegName) {
        this.ffmpegName = ffmpegName;
    }

    public String ffmpegName() {
        return ffmpegName;
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static TransitionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported transition type: " + value, ex);
        }
    }
}
