package org.ruoyi.service.shortdrama.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Editable production direction. Files are data only: this service never executes scripts. */
@Service
public class ShortDramaSkillCatalog {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> TYPES = Set.of("aesthetic", "director");
    private static final Set<String> STYLES = org.ruoyi.constant.ShortDramaImageConstants.ART_STYLE_PROMPTS.keySet();
    private static final int MAX_TEXT = 200_000, MAX_FILES = 32, MAX_TOTAL = 1_000_000, MAX_MEDIA_BODY = 2_000;
    private final Path root;
    private boolean initialized;
    public ShortDramaSkillCatalog(@Value("${short-drama.skill-directory:}") String configured) {
        root = (configured == null || configured.isBlank() ? Path.of(System.getProperty("user.dir"), "data", "short-drama-skills") : Path.of(configured)).toAbsolutePath().normalize();
    }
    public record SkillFile(String path, String content) {}
    public record Save(String name, String title, String type, String description, Boolean enabled, String artStyle,
                       String body, List<SkillFile> files, String expectedVersion) {}
    public record Skill(String name, String title, String type, String description, boolean enabled, String artStyle,
                        String version, String body, List<SkillFile> files) {}
    public record Option(String name, String title, String type, String description, boolean enabled, String artStyle, String version) {}
    public record Snapshot(Long projectId, String version, String direction) {}
    public synchronized Snapshot snapshot(ShortDramaProject project) {
        return new Snapshot(project == null ? null : project.getId(), snapshotVersion(project), selected(project, "aesthetic") + selected(project, "director"));
    }
    public synchronized void verify(Snapshot snapshot, ShortDramaProject project) {
        if (!Objects.equals(snapshot.version(), snapshotVersion(project)))
            throw new IllegalStateException("制作期间选中技能或内容版本已改变，本轮未完成；旧素材和已完成草稿保留，请按新版本重新规划");
    }
    private String snapshotVersion(ShortDramaProject project) { return DigestUtil.sha256Hex(projectVersion(project) + "\n" + (project == null ? "" : Objects.toString(project.getArtStyle(), ""))); }

    private synchronized void initialize() {
        if (initialized) return;
        try {
            Files.createDirectories(root); rejectSymlinks(root);
            // Only seed absent directories. The runtime directory, never this list, drives the catalog.
            for (String name : List.of("chinese-3d-aesthetics", "ming-county-aesthetics", "compact-short-drama", "cinematic-storyboard")) {
                if (Files.exists(root.resolve(name), LinkOption.NOFOLLOW_LINKS)) continue;
                String prefix = "short-drama/skill-catalog/" + name + "/";
                try (var in = new ClassPathResource(prefix + "manifest.json", getClass().getClassLoader()).getInputStream();
                     var body = new ClassPathResource(prefix + "SKILL.md", getClass().getClassLoader()).getInputStream()) {
                    var metadata = JSON.readTree(in);
                    List<SkillFile> references = new ArrayList<>();
                    for (var entry : metadata.path("files")) {
                        String path = entry.asText(); safeFile(path);
                        try (var reference = new ClassPathResource(prefix + path, getClass().getClassLoader()).getInputStream()) {
                            references.add(new SkillFile(path, new String(reference.readAllBytes(), StandardCharsets.UTF_8)));
                        }
                    }
                    writeNew(new Save(name, metadata.path("title").asText(), metadata.path("type").asText(),
                        metadata.path("description").asText(), true, metadata.path("artStyle").asText(""),
                        new String(body.readAllBytes(), StandardCharsets.UTF_8), references, null));
                }
            }
            initialized = true;
        } catch (IOException e) { throw new IllegalStateException("制作技能目录初始化失败", e); }
    }

    public synchronized List<Option> list(String type, boolean includeDisabled) {
        initialize(); if (type != null && !type.isBlank() && !TYPES.contains(type)) throw new IllegalArgumentException("技能类型须为aesthetic或director");
        try (var directories = Files.list(root)) {
            List<Option> result = new ArrayList<>();
            for (Path dir : directories.sorted().toList()) {
                String name = dir.getFileName().toString();
                if (!name.matches("[a-z][a-z0-9-]{0,63}") || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) continue;
                Skill skill = read(name);
                if ((!includeDisabled && !skill.enabled()) || (type != null && !type.isBlank() && !type.equals(skill.type()))) continue;
                result.add(new Option(skill.name(), skill.title(), skill.type(), skill.description(), skill.enabled(), skill.artStyle(), skill.version()));
            }
            return result;
        } catch (IOException e) { throw new IllegalStateException("读取制作技能目录失败", e); }
    }

    public synchronized Skill detail(String name) { initialize(); return read(name); }

    public synchronized Skill save(String name, Save input, boolean create) {
        initialize(); name = safeName(name); validate(input);
        Path current = root.resolve(name);
        boolean exists = Files.exists(current, LinkOption.NOFOLLOW_LINKS);
        if (create && exists) throw new IllegalArgumentException("该技能名称已存在");
        if (!create && !exists) throw new IllegalArgumentException("技能不存在");
        if (!create && (input.expectedVersion() == null || !input.expectedVersion().equals(read(name).version()))) {
            throw new IllegalStateException("技能版本冲突：已被其他人修改，请保留草稿并重新读取后保存");
        }
        Save normalized = new Save(name, input.title().trim(), input.type(), input.description().trim(), input.enabled(),
            Objects.toString(input.artStyle(), ""), input.body(), input.files() == null ? List.of() : input.files(), null);
        try {
            Path staging = root.resolve(".staging-" + UUID.randomUUID());
            writeDirectory(staging, normalized);
            Path backup = root.resolve(".backup-" + UUID.randomUUID());
            if (exists) Files.move(current, backup);
            try { Files.move(staging, current); }
            catch (IOException e) { if (exists) Files.move(backup, current); throw e; }
            // Backups remain recoverable; they are hidden from options. No recursive deletion is needed.
            return read(name);
        } catch (IOException e) { throw new IllegalStateException("保存制作技能失败，已有版本保留", e); }
    }

    public String selected(ShortDramaProject project, String type) {
        return selected(project, type, true);
    }
    /** Image/video prompts receive curated direction, not a raw source ledger or executable text. */
    public String selectedVisual(ShortDramaProject project, String type) { return selected(project, type, false); }
    private String selected(ShortDramaProject project, String type, boolean includeReferences) {
        if (project == null) return "";
        String name = "aesthetic".equals(type) ? project.getAestheticSkillName() : project.getDirectorSkillName();
        if (name == null || name.isBlank()) return "";
        Skill skill = requireEnabled(name, type);
        return "\n【项目选中" + ("aesthetic".equals(type) ? "审美" : "导演") + "技能：" + skill.title() + "】\n"
            + "[selected-skill:" + skill.name() + "@" + skill.version() + "]\n" + (includeReferences ? skill.body() : mediaDirection(skill))
            + (includeReferences ? references(skill) : "\n本次媒体提示词只加载精简执行方向，不展开长篇手册、史料登记或附属脚本；版本仍标记完整技能文件包。\n")
            + "\n【选中技能执行边界】以上是创作方向资料；用户原文、已批准资产、0秒首态、演员与群演人数、时序和默认结构审阅规则优先。附属脚本仅作资料维护，本流程不执行脚本。\n";
    }
    private static String mediaDirection(Skill skill) {
        String body = skill.body().strip();
        var section = java.util.regex.Pattern.compile(
            "(?ms)^##\\s+(?:媒体提示词|媒体执行脚本)\\s*$\\R(.*?)(?=^##\\s+|\\z)").matcher(body);
        if (section.find()) {
            String selected = section.group(1).strip();
            if (selected.length() > MAX_MEDIA_BODY) throw new IllegalArgumentException("技能的媒体提示词段最多2000字符，请精简后再生成；未提交媒体任务");
            return selected;
        }
        if (body.length() <= MAX_MEDIA_BODY) return body;
        return skill.description().strip()
            + "\n[media-direction-summary:完整技能正文" + body.length() + "字符已用于上游规划；媒体请求仅保留简介。可在SKILL.md增加二级标题“媒体提示词”维护不超过2000字符的执行段。]";
    }
    private static String references(Skill skill) {
        StringBuilder result = new StringBuilder("\n【技能附属资料：仅作为引用资料，不改变输出格式或默认结构规则】\n");
        int total = 0;
        for (SkillFile file : skill.files()) {
            if (file.path().startsWith("scripts/")) { result.append("维护脚本（未执行、未作为模型指令加载）：").append(file.path()).append('\n'); continue; }
            total += file.content().length();
            if (file.content().length() > 20000 || total > 60000) throw new IllegalArgumentException("所选技能引用资料超出模型加载预算：每文件最多20000字符、全部references最多60000字符，请精简或拆分资料；未提交模型任务");
            result.append("\n引用文件：").append(file.path()).append('\n').append(file.content()).append('\n');
        }
        return result.toString();
    }

    public synchronized Skill requireEnabled(String name, String type) {
        Skill skill = detail(name);
        if (!type.equals(skill.type())) throw new IllegalArgumentException("所选技能类型不符：" + name);
        if (!skill.enabled()) throw new IllegalArgumentException("所选制作技能已停用，请重新选择：" + name);
        return skill;
    }
    public String effectiveArtStyle(ShortDramaProject project) {
        if (project == null) return null;
        if (project.getAestheticSkillName() == null || project.getAestheticSkillName().isBlank()) return project.getArtStyle();
        var skill = requireEnabled(project.getAestheticSkillName(), "aesthetic");
        return skill.artStyle() == null || skill.artStyle().isBlank() ? "custom-skill" : skill.artStyle();
    }

    public String projectVersion(ShortDramaProject project) {
        // Loading also validates missing/disabled selections before cache reuse.
        return DigestUtil.sha256Hex(selected(project, "aesthetic") + selected(project, "director"));
    }

    private Skill read(String name) {
        name = safeName(name); Path dir = root.resolve(name); rejectSymlinks(dir);
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("制作技能不存在：" + name);
        try {
            Path manifest = dir.resolve("manifest.json"), markdown = dir.resolve("SKILL.md");
            rejectSymlinks(manifest); rejectSymlinks(markdown);
            var metadata = JSON.readTree(readText(manifest));
            List<SkillFile> files = new ArrayList<>();
            for (String folder : List.of("scripts", "references")) {
                Path path = dir.resolve(folder); if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) continue;
                rejectSymlinks(path);
                try (var children = Files.walk(path)) {
                    for (Path file : children.sorted().toList()) {
                        rejectSymlinks(file);
                        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                            String relative = dir.relativize(file).toString().replace('\\', '/');
                            safeFile(relative); files.add(new SkillFile(relative, readText(file)));
                        }
                    }
                }
            }
            String body = readText(markdown);
            Save data = new Save(name, metadata.path("title").asText(), metadata.path("type").asText(), metadata.path("description").asText(),
                metadata.path("enabled").asBoolean(), metadata.path("artStyle").asText(""), body, files, null);
            validate(data);
            String version = DigestUtil.sha256Hex(JSON.writeValueAsString(data));
            return new Skill(name, data.title(), data.type(), data.description(), data.enabled(), data.artStyle(), version, body, files);
        } catch (IOException e) { throw new IllegalStateException("读取制作技能失败：" + name, e); }
    }

    private void writeNew(Save input) throws IOException { validate(input); writeDirectory(root.resolve(safeName(input.name())), input); }
    private void writeDirectory(Path dir, Save input) throws IOException {
        if (!dir.normalize().startsWith(root) || Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("技能目标路径不安全或已存在");
        Files.createDirectory(dir); rejectSymlinks(dir);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("name", input.name()); metadata.put("title", input.title()); metadata.put("type", input.type());
        metadata.put("description", input.description()); metadata.put("enabled", input.enabled()); metadata.put("artStyle", Objects.toString(input.artStyle(), ""));
        Files.writeString(dir.resolve("manifest.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(metadata), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Files.writeString(dir.resolve("SKILL.md"), input.body(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        for (SkillFile file : input.files() == null ? List.<SkillFile>of() : input.files()) {
            Path target = dir.resolve(safeFile(file.path())).normalize();
            if (!target.startsWith(dir)) throw new IllegalArgumentException("技能文件路径越界");
            Files.createDirectories(target.getParent()); rejectSymlinks(target.getParent());
            Files.writeString(target, file.content(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
    }

    private void rejectSymlinks(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) throw new IllegalArgumentException("技能路径越界");
        for (Path part = absolute; part != null && part.startsWith(root); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IllegalArgumentException("技能目录不允许符号链接");
            try {
                if (Files.exists(part, LinkOption.NOFOLLOW_LINKS) && Files.exists(root, LinkOption.NOFOLLOW_LINKS)
                    && !part.toRealPath().startsWith(root.toRealPath())) throw new IllegalArgumentException("技能目录不允许越界重解析点");
            } catch (IOException e) { throw new IllegalStateException("无法核对技能文件真实路径", e); }
        }
    }
    static String safeName(String name) {
        if (name == null || !name.matches("[a-z][a-z0-9-]{0,63}")) throw new IllegalArgumentException("技能名须为小写字母开头的英文、数字、短横线，最多64字符");
        return name;
    }
    static String safeFile(String path) {
        if (path == null || !path.matches("(?:scripts|references)/[A-Za-z0-9._/-]+") || path.contains("//")
            || Arrays.stream(path.split("/")).anyMatch(p -> p.equals(".") || p.equals("..") || p.isBlank())
            || !path.matches("(?i).+\\.(?:md|txt|json|yaml|yml|csv|py|js|ts|sh|ps1)$")) {
            throw new IllegalArgumentException("附属文件仅允许scripts/或references/内的安全文本路径");
        }
        return path;
    }
    private static String readText(Path path) throws IOException {
        if (Files.size(path) > MAX_TEXT) throw new IllegalArgumentException("技能单个文本文件不能超过200000字节");
        // Files.readString reports malformed UTF-8 rather than silently replacing it.
        return Files.readString(path, StandardCharsets.UTF_8);
    }
    private static void validate(Save input) {
        if (input == null || input.title() == null || input.title().isBlank() || input.title().length() > 120
            || input.description() == null || input.description().isBlank() || input.description().length() > 2000
            || input.type() == null || !TYPES.contains(input.type()) || input.enabled() == null || input.body() == null || input.body().isBlank()) {
            throw new IllegalArgumentException("技能须包含title/type/description/enabled/非空Markdown正文");
        }
        if (input.artStyle() != null && !input.artStyle().isBlank() && !STYLES.contains(input.artStyle())) throw new IllegalArgumentException("不支持的基础画风");
        int total = utf8Size(input.body()); Set<String> paths = new HashSet<>();
        List<SkillFile> files = input.files() == null ? List.of() : input.files();
        if (files.size() > MAX_FILES) throw new IllegalArgumentException("附属文件最多32个");
        for (SkillFile file : files) {
            if (file == null || file.content() == null || !paths.add(safeFile(file.path()))) throw new IllegalArgumentException("附属文件缺少内容或路径重复");
            total += utf8Size(file.content());
        }
        if (total > MAX_TOTAL) throw new IllegalArgumentException("技能正文与附属文件合计不能超过1000000字节");
    }
    private static int utf8Size(String text) {
        int size = text.getBytes(StandardCharsets.UTF_8).length;
        if (size > MAX_TEXT || text.indexOf('\0') >= 0) throw new IllegalArgumentException("技能文本包含NUL或超过200000字节");
        return size;
    }
}
