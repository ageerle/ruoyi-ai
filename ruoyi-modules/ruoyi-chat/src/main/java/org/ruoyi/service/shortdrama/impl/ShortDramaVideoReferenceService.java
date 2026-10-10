package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Immutable, project-owned motion references. Uploading a clip does not generate media. */
@Service @RequiredArgsConstructor
public class ShortDramaVideoReferenceService {
    private final ShortDramaProjectMapper projects;
    private final ImageServiceFactory images;
    public record Clip(String id, String name, double duration, int width, int height, double fps, long bytes, String sha256) {}

    private Path directory(Long project) { return Path.of("data", "short-drama-motion", project.toString()).toAbsolutePath(); }
    public void owner(Long project, Long user) {
        var entity = projects.selectById(project);
        if (entity == null || !Objects.equals(entity.getUserId(), user)) throw new IllegalArgumentException("项目不存在或无权限");
    }
    static String checkedId(String id) {
        if (id == null || !id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")) throw new IllegalArgumentException("动作参考ID无效");
        return id;
    }
    public Path file(Long project, String id) { return directory(project).resolve(checkedId(id) + ".mp4"); }
    public List<Clip> list(Long project, Long user) throws Exception {
        owner(project, user);
        if (!Files.isDirectory(directory(project))) return List.of();
        try (var files = Files.list(directory(project))) {
            var result = new ArrayList<Clip>();
            for (var path : files.filter(p -> p.toString().endsWith(".json")).sorted().toList())
                result.add(AtlasMediaSupport.OBJECT_MAPPER.readValue(path.toFile(), Clip.class));
            return result;
        }
    }
    public synchronized Clip upload(Long project, Long user, MultipartFile input) throws Exception {
        owner(project, user);
        if (input.isEmpty() || input.getSize() > 30L * 1024 * 1024) throw new IllegalArgumentException("动作参考视频应小于30MB");
        String name = Objects.toString(input.getOriginalFilename(), "动作参考.mp4");
        if (!name.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mov)$")) throw new IllegalArgumentException("请选择MP4或MOV视频");
        Files.createDirectories(directory(project));
        String id = UUID.randomUUID().toString();
        Path target = file(project, id), probe = directory(project).resolve(id + ".probe");
        boolean saved = false;
        try {
            input.transferTo(target);
            var process = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                "format=duration:stream=codec_name,width,height,avg_frame_rate", "-of", "json", target.toString())
                .redirectErrorStream(true).redirectOutput(probe.toFile()).start();
            if (!process.waitFor(25, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("视频检查超时"); }
            if (process.exitValue() != 0) throw new IllegalArgumentException("无法读取动作视频");
            JsonNode data = AtlasMediaSupport.OBJECT_MAPPER.readTree(Files.readString(probe));
            var stream = data.path("streams").path(0);
            String[] ratio = stream.path("avg_frame_rate").asText("0/1").split("/");
            double fps = Double.parseDouble(ratio[0]) / (ratio.length == 2 ? Double.parseDouble(ratio[1]) : 1);
            var clip = new Clip(id, name.substring(0, Math.min(120, name.length())), data.path("format").path("duration").asDouble(),
                stream.path("width").asInt(), stream.path("height").asInt(), fps, Files.size(target),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(target.toFile()));
            validateClip(clip, stream.path("codec_name").asText());
            AtlasMediaSupport.OBJECT_MAPPER.writeValue(directory(project).resolve(id + ".json").toFile(), clip);
            saved = true; return clip;
        } finally {
            Files.deleteIfExists(probe);
            if (!saved) Files.deleteIfExists(target);
        }
    }
    static void validateClip(Clip clip, String codec) {
        double aspect = (double)clip.width() / clip.height();
        long pixels = (long)clip.width() * clip.height();
        if (!Set.of("h264", "hevc").contains(codec) || !Double.isFinite(clip.duration()) || clip.duration() < 2 || clip.duration() > 30.05
            || !Double.isFinite(clip.fps()) || clip.fps() < 23.9 || clip.fps() > 60.1 || clip.width() < 300 || clip.height() < 300
            || clip.width() > 6000 || clip.height() > 6000 || aspect < .4 || aspect > 2.5 || pixels < 409600 || pixels > 8295044)
            throw new IllegalArgumentException("动作参考须为2–30秒、24–60fps的H.264/H.265视频，画幅0.4–2.5且不低于480p");
    }
    public List<Clip> selected(Long project, Long user, String continuity) {
        owner(project, user);
        try {
            var node = AtlasMediaSupport.OBJECT_MAPPER.readTree(continuity == null || continuity.isBlank() ? "{}" : continuity).path("video_reference_ids");
            if (node.isMissingNode()) return List.of();
            if (!node.isArray() || node.size() > 10) throw new IllegalArgumentException("动作参考应为最多10个视频ID");
            var result = new ArrayList<Clip>(); var used = new HashSet<String>();
            for (var item : node) {
                if (!item.isTextual() || !used.add(checkedId(item.asText()))) throw new IllegalArgumentException("动作参考ID重复或无效");
                var path = directory(project).resolve(item.asText() + ".json");
                if (!Files.isRegularFile(path) || !Files.isRegularFile(file(project, item.asText()))) throw new IllegalArgumentException("动作参考不存在或不属于当前项目");
                var clip = AtlasMediaSupport.OBJECT_MAPPER.readValue(path.toFile(), Clip.class);
                if (!clip.id().equals(item.asText())) throw new IllegalArgumentException("动作参考记录不匹配");
                result.add(clip);
            }
            return result;
        } catch (java.io.IOException error) { throw new IllegalArgumentException("动作参考记录无法读取", error); }
    }
    public static void validateModel(String model, List<Clip> clips) {
        if (clips.isEmpty()) return;
        boolean mini = model.startsWith("bytedance/seedance-2.0"), full = model.startsWith("bytedance/seedance-2.5/");
        if ((!mini && !full) || !model.endsWith("/reference-to-video")) throw new IllegalArgumentException("当前模型不支持动作视频参考");
        double sum = clips.stream().mapToDouble(Clip::duration).sum();
        if (clips.size() > (mini ? 3 : 10) || sum > (mini ? 15.05 : 30.05)) throw new IllegalArgumentException(mini ? "Seedance 2.0最多3条视频，合计不超过15秒" : "Seedance 2.5最多10条视频，合计不超过30秒");
        for (var clip : clips) if (clip.bytes() > (mini ? 50L : 200L) * 1024 * 1024 || mini && (clip.duration() > 15.05 || (long)clip.width()*clip.height() > 2086876))
            throw new IllegalArgumentException("动作视频超过当前模型的时长、文件或分辨率限制");
    }
    public List<String> transport(Long project, ChatModelVo model, List<Clip> clips) {
        ShortDramaAtlasModels.requireProvider(model);
        validateModel(model.getModelName(), clips);
        var result = new ArrayList<String>();
        try {
            for (var clip : clips) {
                var path = file(project, clip.id());
                if (!cn.hutool.crypto.digest.DigestUtil.sha256Hex(path.toFile()).equals(clip.sha256())) throw new IllegalArgumentException("动作参考文件已变更，请重新上传");
                result.add(images.getOriginalService(model.getProviderCode()).uploadMedia(model, Files.readAllBytes(path), clip.id()+".mp4", "video/mp4"));
            }
            return result;
        } catch (java.io.IOException error) { throw new IllegalStateException("动作参考无法上传", error); }
    }
}
