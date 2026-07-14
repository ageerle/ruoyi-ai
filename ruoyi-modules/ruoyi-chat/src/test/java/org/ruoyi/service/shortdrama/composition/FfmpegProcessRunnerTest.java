package org.ruoyi.service.shortdrama.composition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class FfmpegProcessRunnerTest {

    @Test
    void readsOnlyTheConfiguredOutputTail() throws Exception {
        Path log = Path.of("target", "test-process-" + UUID.randomUUID() + ".log");
        try {
            Files.writeString(log, "0123456789", StandardCharsets.UTF_8);
            assertEquals("56789", FfmpegProcessRunner.readTail(log, 5));
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
