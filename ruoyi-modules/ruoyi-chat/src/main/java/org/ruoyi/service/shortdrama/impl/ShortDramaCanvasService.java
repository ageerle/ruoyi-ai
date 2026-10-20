package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.util.*;

/** Editable production documents. Media submission and business ownership remain in their existing services. */
@Service
public class ShortDramaCanvasService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ShortDramaSoundService owners;
    private final Path root;
    public record Canvas(long revision, JsonNode document) { }
    @Autowired public ShortDramaCanvasService(ShortDramaSoundService owners) { this(owners, Path.of("data", "short-drama-canvases")); }
    ShortDramaCanvasService(ShortDramaSoundService owners, Path root) { this.owners = owners; this.root = root.toAbsolutePath(); }
    private Path file(Long project) { return root.resolve(project + ".json"); }
    public synchronized Canvas read(Long project, Long user) throws Exception {
        owners.owner(project, user);
        return Files.exists(file(project)) ? JSON.readValue(file(project).toFile(), Canvas.class) : new Canvas(0, null);
    }
    public synchronized Canvas save(Long project, Long user, Canvas request) throws Exception {
        owners.owner(project, user);
        validate(request.document());
        Canvas previous = read(project, user);
        if (previous.revision() != request.revision()) throw new IllegalArgumentException("画布已在另一窗口更新，请重新加载后继续编辑");
        Canvas saved = new Canvas(previous.revision() + 1, request.document().deepCopy());
        Files.createDirectories(root);
        Path temporary = Files.createTempFile(root, "canvas-", ".tmp");
        try {
            JSON.writeValue(temporary.toFile(), saved);
            try { Files.move(temporary, file(project), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file(project), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        return saved;
    }
    static void validate(JsonNode doc) throws Exception {
        if (doc == null || !doc.isObject() || doc.path("schema").asInt() != 1 || !doc.path("nodes").isArray() || !doc.path("edges").isArray())
            throw new IllegalArgumentException("画布格式无效");
        if (JSON.writeValueAsBytes(doc).length > 2 * 1024 * 1024 || doc.path("nodes").size() > 500 || doc.path("edges").size() > 1500)
            throw new IllegalArgumentException("画布超过保存上限");
        Set<String> ids = new HashSet<>(); Map<String,String> kinds = new HashMap<>();
        Set<String> supported = Set.of("text", "script", "image", "video", "audio", "group", "previs");
        for (JsonNode n : doc.path("nodes")) {
            String id = n.path("id").asText();
            if (id.isBlank() || id.length() > 180 || !ids.add(id) || !supported.contains(n.path("kind").asText()) || !n.path("data").isObject()
                || !n.path("position").path("x").isNumber() || !n.path("position").path("y").isNumber()
                || !Double.isFinite(n.path("position").path("x").asDouble()) || !Double.isFinite(n.path("position").path("y").asDouble()))
                throw new IllegalArgumentException("节点格式或编号无效");
            kinds.put(id, n.path("kind").asText());
        }
        Map<String,List<String>> paths = new HashMap<>(); Set<String> edgeIds = new HashSet<>();
        for (JsonNode e : doc.path("edges")) {
            String source = e.path("source").asText(), target = e.path("target").asText();
            if (!edgeIds.add(e.path("id").asText()) || !ids.contains(source) || !ids.contains(target) || source.equals(target)
                || "group".equals(kinds.get(source)) || "group".equals(kinds.get(target))) throw new IllegalArgumentException("连线端点无效");
            paths.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
        }
        Set<String> visited = new HashSet<>(), active = new HashSet<>();
        for (String id : ids) visit(id, paths, visited, active);
        for (JsonNode n : doc.path("nodes")) {
            String parent = n.path("parentId").asText();
            if (!parent.isEmpty() && (!"group".equals(kinds.get(parent)) || parent.equals(n.path("id").asText()) || "group".equals(n.path("kind").asText())))
                throw new IllegalArgumentException("分组关系无效");
        }
    }
    private static void visit(String id, Map<String,List<String>> paths, Set<String> visited, Set<String> active) {
        if (active.contains(id)) throw new IllegalArgumentException("连线不能形成循环");
        if (!visited.add(id)) return;
        active.add(id); for (String next : paths.getOrDefault(id, List.of())) visit(next, paths, visited, active); active.remove(id);
    }
}
