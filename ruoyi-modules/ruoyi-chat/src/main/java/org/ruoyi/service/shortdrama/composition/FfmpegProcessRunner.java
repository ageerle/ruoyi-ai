package org.ruoyi.service.shortdrama.composition;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegProcessRunner {

    public MediaProcessResult run(List<String> command, Duration timeout, Path logPath, int maxOutputBytes) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(logPath, "logPath");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Process timeout must be positive");
        }
        if (maxOutputBytes <= 0) {
            throw new IllegalArgumentException("maxOutputBytes must be positive");
        }

        Process process = null;
        try {
            Path absoluteLog = logPath.toAbsolutePath().normalize();
            Files.createDirectories(absoluteLog.getParent());
            ProcessBuilder builder = new ProcessBuilder(List.copyOf(command));
            builder.redirectErrorStream(true);
            builder.redirectOutput(absoluteLog.toFile());
            process = builder.start();

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                stop(process);
                String tail = readTail(absoluteLog, maxOutputBytes);
                throw new MediaProcessException(command.get(0) + " timed out after " + timeout + formatOutput(tail));
            }

            int exitCode = process.exitValue();
            String output = readTail(absoluteLog, maxOutputBytes);
            if (exitCode != 0) {
                throw new MediaProcessException(command.get(0) + " exited with code " + exitCode + formatOutput(output));
            }
            return new MediaProcessResult(exitCode, output);
        } catch (InterruptedException ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new MediaProcessException(command.get(0) + " was interrupted", ex);
        } catch (IOException ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            throw new MediaProcessException("Unable to execute " + command.get(0) + ": " + ex.getMessage(), ex);
        }
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }
    }

    static String readTail(Path path, int maxBytes) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }
        long size = Files.size(path);
        int bytesToRead = (int) Math.min(size, maxBytes);
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            channel.position(Math.max(0, size - bytesToRead));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Keep reading until the selected tail is complete.
            }
        }
        return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8).trim();
    }

    private static String formatOutput(String output) {
        return output == null || output.isBlank() ? "" : System.lineSeparator() + output;
    }
}
