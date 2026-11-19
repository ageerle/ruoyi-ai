package org.ruoyi.chat.service.coding.harness.tool.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Continuously drains one child stream while retaining only its bounded prefix. */
final class BoundedOutputCollector implements Runnable {

    private final InputStream source;
    private final int limit;
    private final ByteArrayOutputStream retained;
    private volatile boolean truncated;
    private volatile IOException failure;

    BoundedOutputCollector(InputStream source, int limit) {
        this.source = source;
        this.limit = limit;
        this.retained = new ByteArrayOutputStream(Math.min(limit, 16 * 1024));
    }

    @Override
    public void run() {
        byte[] bytes = new byte[8 * 1024];
        try (source) {
            int read;
            while ((read = source.read(bytes)) != -1) {
                int remaining = limit - retained.size();
                int keep = Math.min(Math.max(remaining, 0), read);
                if (keep > 0) {
                    retained.write(bytes, 0, keep);
                }
                if (keep < read) {
                    truncated = true;
                }
            }
        } catch (IOException error) {
            failure = error;
        }
    }

    String content() {
        return retained.toString(StandardCharsets.UTF_8);
    }

    boolean truncated() {
        return truncated;
    }

    IOException failure() {
        return failure;
    }

    void close() {
        try {
            source.close();
        } catch (IOException ignored) {
            // Closing is only used to unblock a collector after the process has been terminated.
        }
    }
}
