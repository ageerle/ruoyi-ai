package org.ruoyi.chat.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatMessageImageStore {

    private final NamedParameterJdbcTemplate jdbc;

    public void insert(Long messageId, String mimeType, byte[] imageData) {
        jdbc.update("""
            INSERT INTO chat_message_image (message_id, mime_type, image_data)
            VALUES (:messageId, :mimeType, :imageData)
            """, Map.of("messageId", messageId, "mimeType", mimeType, "imageData", imageData));
    }

    public Optional<StoredImage> find(Long messageId) {
        return jdbc.query("""
            SELECT mime_type, image_data FROM chat_message_image WHERE message_id = :id
            """, Map.of("id", messageId),
            (rs, index) -> new StoredImage(rs.getString("mime_type"), rs.getBytes("image_data")))
            .stream().findFirst();
    }

    public Set<Long> findMessageIds(Collection<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.queryForList("""
            SELECT message_id FROM chat_message_image WHERE message_id IN (:ids)
            """, Map.of("ids", messageIds), Long.class));
    }

    public Map<Long, String> findRecognizedText(Collection<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("""
            SELECT message_id, recognized_text FROM chat_message_image WHERE message_id IN (:ids)
            """, Map.of("ids", messageIds), rs -> {
            Map<Long, String> results = new LinkedHashMap<>();
            while (rs.next()) {
                results.put(rs.getLong("message_id"), rs.getString("recognized_text"));
            }
            return results;
        });
    }

    public record StoredImage(String mimeType, byte[] data) {}
}
