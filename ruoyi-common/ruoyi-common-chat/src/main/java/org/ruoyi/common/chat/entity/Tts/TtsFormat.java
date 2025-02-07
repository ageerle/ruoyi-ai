package org.ruoyi.common.chat.entity.Tts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TtsFormat {
    MP3("mp3"),
    OPUS("opus"),
    AAC("aac"),
    FLAC("flac"),
    ;
    private final String name;
}
