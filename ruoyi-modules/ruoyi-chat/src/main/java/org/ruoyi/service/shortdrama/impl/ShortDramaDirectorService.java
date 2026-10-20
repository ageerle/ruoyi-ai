package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.util.*;

/** Small, owned previs documents; saving a document never submits a media task. */
@Service
public class ShortDramaDirectorService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> ACTIONS = Set.of("idle", "walk", "run", "wave", "sit", "reach", "point", "nod", "shakeHead", "crouch", "bow", "talk");
    private static final Set<String> JOINTS = Set.of("pelvis", "torso", "head", "leftShoulder", "rightShoulder", "leftArm", "leftElbow", "rightArm", "rightElbow", "leftHand", "rightHand", "leftLeg", "leftKnee", "rightLeg", "rightKnee", "leftFoot", "rightFoot");
    private final ShortDramaSoundService owners;
    private final Path root;
    public record Director(long revision, JsonNode document) { }
    @Autowired public ShortDramaDirectorService(ShortDramaSoundService owners) { this(owners, Path.of("data", "short-drama-directors")); }
    ShortDramaDirectorService(ShortDramaSoundService owners, Path root) { this.owners = owners; this.root = root.toAbsolutePath().normalize(); }
    private Path file(Long project, String id) {
        if (project == null || project <= 0 || id == null || !id.matches("[a-zA-Z0-9_-]{1,100}")) throw new IllegalArgumentException("工程编号无效");
        return root.resolve(project.toString()).resolve(id + ".json");
    }
    public synchronized Director read(Long project, String id, Long user) throws Exception {
        owners.owner(project, user); Path target = file(project, id);
        return Files.exists(target) ? JSON.readValue(target.toFile(), Director.class) : new Director(0, null);
    }
    public synchronized Director save(Long project, String id, Long user, Director request) throws Exception {
        owners.owner(project, user); Path target = file(project, id);
        if (request == null) throw new IllegalArgumentException("工程不能为空");
        validate(request.document());
        var previous = read(project, id, user);
        if (previous.revision() != request.revision()) throw new IllegalArgumentException("导演工程已在另一窗口更新，请先导出备份，再重新加载");
        var saved = new Director(previous.revision() + 1, request.document().deepCopy());
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "director-", ".tmp");
        try {
            JSON.writeValue(temporary.toFile(), saved);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        return saved;
    }
    static void validate(JsonNode doc) throws Exception {
        if (doc == null || !doc.isObject() || !doc.path("schema").isIntegralNumber() || doc.path("schema").asInt() != 1 || !doc.path("fps").isIntegralNumber() || doc.path("fps").asInt() != 24
            || !doc.path("duration").isIntegralNumber() || doc.path("duration").asInt() < 2 || doc.path("duration").asInt() > 60
            || !Set.of("16:9", "9:16").contains(doc.path("aspect").asText()) || !doc.path("objects").isArray() || doc.path("objects").size() > 80
            || JSON.writeValueAsBytes(doc).length > 1024 * 1024) throw new IllegalArgumentException("工程格式无效或超过上限");
        int frames = doc.path("duration").asInt() * 24;
        Set<String> ids = new HashSet<>();
        for (JsonNode o : doc.path("objects")) {
            if (!o.path("id").asText().matches("[a-zA-Z0-9_-]{1,100}") || !ids.add(o.path("id").asText())
                || !Set.of("actor", "box", "wall", "door", "castle", "cannon").contains(o.path("kind").asText()) || !o.path("name").isTextual()
                || o.path("name").asText().isBlank() || o.path("name").asText().length() > 100 || !o.path("color").asText().matches("#[a-fA-F0-9]{6}")
                || !o.path("visible").isBoolean() || !o.path("locked").isBoolean()) throw new IllegalArgumentException("场景对象无效");
            vector(o.path("scale"), true); keys(o.path("keys"), frames);
            if (o.has("fireFrame") && (!o.path("kind").asText().equals("cannon") || !o.path("fireFrame").isIntegralNumber() || o.path("fireFrame").asInt() < 0 || o.path("fireFrame").asInt() >= frames)) throw new IllegalArgumentException("开炮时刻超出大炮的时间轴范围");
            if (o.has("autoFace") && !o.path("autoFace").isBoolean()) throw new IllegalArgumentException("朝向选项无效");
            for (String option : List.of("grounded", "quaternionPose")) if (o.has(option) && (!o.path(option).isBoolean() || o.path(option).asBoolean() && !o.path("kind").asText().equals("actor"))) throw new IllegalArgumentException("人物姿态选项无效");
            for (JsonNode k : o.path("keys")) {
                vector(k.path("position"), false); number(k.path("yaw"), -360, 360);
                if (!ACTIONS.contains(k.path("action").asText())) throw new IllegalArgumentException("白模动作无效");
                ease(k);
                if (k.has("pose")) {
                    JsonNode pose = k.path("pose");
                    if (!pose.isObject()) throw new IllegalArgumentException("关节姿态无效");
                    var fields = pose.fields();
                    while (fields.hasNext()) {
                        var entry = fields.next();
                        if (entry.getKey().equals("height")) number(entry.getValue(), -.9, 1);
                        else {
                            if (!JOINTS.contains(entry.getKey()) || !entry.getValue().isArray() || entry.getValue().size() != 3) throw new IllegalArgumentException("关节名称或角度无效");
                            for (JsonNode angle : entry.getValue()) number(angle, -180, 180);
                        }
                    }
                }
            }
            if (o.has("clips")) {
                JsonNode clips = o.path("clips");
                if (!o.path("kind").asText().equals("actor") || !clips.isArray() || clips.size() > 100) throw new IllegalArgumentException("动作片段无效");
                int previousEnd = 0; Set<String> clipIds = new HashSet<>();
                for (JsonNode c : clips) {
                    int start = c.path("start").asInt(-1), end = c.path("end").asInt(-1), blend = c.path("blend").asInt(-1);
                    if (!c.path("id").isTextual() || !c.path("id").asText().matches("[a-zA-Z0-9_-]{1,100}") || !clipIds.add(c.path("id").asText())
                        || !ACTIONS.contains(c.path("action").asText()) || !c.path("start").isIntegralNumber() || !c.path("end").isIntegralNumber()
                        || start < previousEnd || end <= start || end > frames || !c.path("blend").isIntegralNumber() || blend < 0 || blend > (end-start)/2)
                        throw new IllegalArgumentException("动作片段时间无效、重叠或过渡过长");
                    number(c.path("speed"), .25, 3); number(c.path("strength"), 0, 1.5); previousEnd = end;
                }
            }
        }
        for (JsonNode o : doc.path("objects")) if (o.has("lookAtId") && (!o.path("lookAtId").isTextual() || !ids.contains(o.path("lookAtId").asText()) || o.path("lookAtId").asText().equals(o.path("id").asText()))) throw new IllegalArgumentException("注视对象无效");
        for (JsonNode o : doc.path("objects")) if (o.has("targetId")) {
            boolean castle = false;
            for (JsonNode target : doc.path("objects")) if (target.path("id").asText().equals(o.path("targetId").asText()) && target.path("kind").asText().equals("castle")) castle = true;
            if (!o.path("kind").asText().equals("cannon") || !o.path("targetId").isTextual() || !castle) throw new IllegalArgumentException("大炮命中目标必须是当前场景的城门城墙");
        }
        keys(doc.path("camera").path("keys"), frames);
        if (doc.has("markers")) {
            JsonNode markers = doc.path("markers");
            if (!markers.isArray() || markers.size() > 100) throw new IllegalArgumentException("动作标记无效");
            for (JsonNode marker : markers) if (!marker.path("frame").isIntegralNumber() || marker.path("frame").asInt() < 0 || marker.path("frame").asInt() >= frames || !marker.path("label").isTextual() || marker.path("label").asText().isBlank() || marker.path("label").asText().length() > 60) throw new IllegalArgumentException("动作标记无效");
        }
        for (JsonNode k : doc.path("camera").path("keys")) {
            vector(k.path("position"), false); vector(k.path("target"), false);
            number(k.path("fov"), 15, 100); number(k.path("roll"), -180, 180);
            ease(k);
            boolean same = true;
            for (int i = 0; i < 3; i++) if (Math.abs(k.path("position").get(i).asDouble() - k.path("target").get(i).asDouble()) >= .001) same = false;
            if (same) throw new IllegalArgumentException("机位与拍摄目标不能重合");
        }
        if (doc.path("camera").has("follow")) {
            JsonNode f = doc.path("camera").path("follow");
            if (!f.isObject() || !ids.contains(f.path("objectId").asText()) || !Set.of("aim", "follow").contains(f.path("mode").asText())) throw new IllegalArgumentException("摄影机跟随对象无效");
            vector(f.path("offset"), false); number(f.path("height"), 0, 3);
            if (f.path("mode").asText().equals("follow")) {
                double x=f.path("offset").get(0).asDouble(), y=f.path("offset").get(1).asDouble()-f.path("height").asDouble(), z=f.path("offset").get(2).asDouble();
                if (Math.sqrt(x*x+y*y+z*z) < .01) throw new IllegalArgumentException("跟随机位不能与目标重合");
            }
        }
    }
    private static void ease(JsonNode key) {
        if (key.has("ease") && (!key.path("ease").isTextual() || !Set.of("linear", "smooth", "hold").contains(key.path("ease").asText()))) throw new IllegalArgumentException("插值方式无效");
    }
    private static void vector(JsonNode v, boolean scale) {
        if (!v.isArray() || v.size() != 3) throw new IllegalArgumentException("坐标格式无效");
        for (JsonNode n : v) number(n, scale ? .1 : -100, scale ? 20 : 100);
    }
    private static void number(JsonNode v, double min, double max) {
        if (!v.isNumber() || !Double.isFinite(v.asDouble()) || v.asDouble() < min || v.asDouble() > max) throw new IllegalArgumentException("数值超出范围");
    }
    private static void keys(JsonNode list, int frames) {
        if (!list.isArray() || list.isEmpty() || list.size() > 1440 || list.get(0).path("frame").asInt(-1) != 0) throw new IllegalArgumentException("关键帧必须从第 0 帧开始");
        int previous = -1;
        for (JsonNode k : list) {
            int frame = k.path("frame").asInt(-1);
            if (!k.path("frame").isIntegralNumber() || frame <= previous || frame >= frames) throw new IllegalArgumentException("关键帧顺序或时间无效");
            previous = frame;
        }
    }
}
