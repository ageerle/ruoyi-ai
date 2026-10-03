package org.ruoyi.service.shortdrama.impl;

import cn.hutool.crypto.digest.DigestUtil;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/** Per-scene provenance: unrelated assets and transient image results never invalidate a validated scene. */
final class ShortDramaSceneCheckpoint {
    static final String VERSION = "scene-proof-v1";
    record Identity(Long projectId, Long scriptId, int sceneNumber, String sceneHash, String contextHash, String modelHash, int minimumShotSeconds) {
        String prefix() { return "short-drama:scene-checkpoint:" + VERSION + ":" + projectId + ":" + scriptId
            + ":" + sceneNumber + ":" + modelHash + ":" + sceneHash + ":" + contextHash + ":"; }
    }
    record Binding(String kind, Long id, String name, String signature) {}
    record Receipt(Identity identity, List<Binding> bindings, boolean validated, String origin,
                   long savedAt, List<StoryboardPanelData> panels) {}
    record Assets(List<ShortDramaCharacter> characters, List<ShortDramaCharacterAppearance> appearances,
                  List<ShortDramaLocation> locations, String selectedSkillVersion) {
        Assets(List<ShortDramaCharacter> characters, List<ShortDramaCharacterAppearance> appearances, List<ShortDramaLocation> locations) {
            this(characters, appearances, locations, "");
        }
        String signature() {
            List<BaseEntity> all = new ArrayList<>(); all.addAll(characters); all.addAll(appearances); all.addAll(locations);
            String original = ShortDramaServiceImpl.planningAssetSignature(all);
            return selectedSkillVersion == null || selectedSkillVersion.isBlank() ? original : DigestUtil.sha256Hex(original + selectedSkillVersion);
        }
        List<Binding> referenced(List<StoryboardPanelData> panels) {
            Map<String, Binding> bindings = new TreeMap<>();
            for (StoryboardPanelData panel : panels) {
                ShortDramaBackgroundExtras.parse(panel.getBackgroundExtras());
                if (panel.getLocation() == null || panel.getLocation().isBlank()) throw new BindingMismatch("镜头未绑定已登记场景");
                ShortDramaLocation location = unique(locations.stream().filter(a -> Objects.equals(a.getName(), panel.getLocation())).toList(), "场景", panel.getLocation());
                add(bindings, "location", location.getId(), location.getName(), location);
                // A listening actor or an offscreen speaker is also a dependency, even without a visual ref.
                Set<String> visualNames = new HashSet<>();
                for (var ref : panel.getCharacters() == null ? List.<ShortDramaServiceImpl.CharacterRef>of() : panel.getCharacters()) {
                    visualNames.add(ref.getName());
                    ShortDramaCharacter character = unique(characters.stream().filter(a -> Objects.equals(a.getName(), ref.getName())).toList(), "角色", ref.getName());
                    add(bindings, "character", character.getId(), character.getName(), character);
                    var choices = appearances.stream().filter(a -> Objects.equals(a.getCharacterId(), character.getId())).toList();
                    if (choices.isEmpty()) continue;
                    String requested = Objects.toString(ref.getAppearance(), "").trim();
                    List<ShortDramaCharacterAppearance> matched;
                    List<ShortDramaCharacterAppearance> named = choices.stream().filter(a -> Objects.equals(a.getChangeReason(), requested)).toList();
                    if (!requested.isBlank() && !named.isEmpty()) {
                        // Literal stage names can start with a year or age. Ambiguous names must not fall back to an index.
                        matched = named;
                    } else if (!requested.isBlank()) {
                        var index = Pattern.compile("^(?:([0-9]+)|\\[([0-9]+)\\].*)$").matcher(requested);
                        if (index.matches()) {
                            int number;
                            try { number = Integer.parseInt(index.group(1) == null ? index.group(2) : index.group(1)); }
                            catch (NumberFormatException e) { throw new BindingMismatch("角色形象索引超出范围: " + character.getName() + ":" + requested); }
                            matched = choices.stream().filter(a -> Objects.equals(a.getAppearanceIndex(), number)).toList();
                        } else matched = uniquelyDescribedAppearance(choices, requested);
                    } else {
                        matched = choices.size() == 1 ? choices : choices.stream().filter(a -> Objects.equals(a.getAppearanceIndex(), 0)).toList();
                    }
                    ShortDramaCharacterAppearance appearance = unique(matched, "角色形象", character.getName() + ":" + requested);
                    add(bindings, "appearance", appearance.getId(), character.getName(), appearance);
                }
                Set<String> otherNames = new HashSet<>(panel.getPresentCharacters() == null ? List.of() : panel.getPresentCharacters());
                String source = Objects.toString(panel.getSourceText(), "");
                for (var character : characters) {
                    if (character.getName() != null && Pattern.compile(Pattern.quote(character.getName())
                        + "(?:[（(][^）)]*[）)])?\\s*[：:]\\s*「").matcher(source).find()) otherNames.add(character.getName());
                }
                otherNames.removeAll(visualNames);
                for (String name : otherNames) {
                    ShortDramaCharacter character = unique(characters.stream().filter(a -> Objects.equals(a.getName(), name)).toList(), "角色", name);
                    add(bindings, "character", character.getId(), character.getName(), character);
                }
            }
            return List.copyOf(bindings.values());
        }
        void validateSceneCast(String scene, String previous, List<StoryboardPanelData> panels) {
            ShortDramaBackgroundExtras.validateScene(scene, characters, panels);
            Set<String> named = sourceNames(scene, previous);
            // Anonymous groups are staged separately. An empty source role set is never permission
            // to substitute a registered leader or carry an unrelated actor from the preceding scene.
            for (var p : panels) {
                Set<String> actual = new HashSet<>(p.getPresentCharacters() == null ? List.of() : p.getPresentCharacters());
                if (p.getCharacters() != null) for (var ref : p.getCharacters()) actual.add(ref.getName());
                actual.removeAll(named);
                if (!actual.isEmpty()) throw new BindingMismatch("本场原文未安排出镜角色（镜头" + p.getPanelNumber() + "）："
                    + actual.stream().map(n -> Objects.toString(n, "<空名称>")).sorted().collect(java.util.stream.Collectors.joining("、"))
                    + "；世界观/全剧角色库不得用于擅加本场演员或CG，请按本场原文重排");
            }
        }
        Set<String> sourceNames(String scene, String previous) {
            String visibleText = scene.replaceAll("「[^」]*」", "");
            Set<String> named = new HashSet<>();
            for (var c : characters) if (mentioned(visibleText, c)) named.add(c.getName());
            // Unnamed pronouns may inherit the preceding scene; no global world/library actor is implied.
            if (named.isEmpty() && visibleText.matches("(?s).*[他她它其二人众人].*"))
                for (var c : characters) if (mentioned(previous, c)) named.add(c.getName());
            return named;
        }
        private static boolean mentioned(String text, ShortDramaCharacter character) {
            if (character.getName() == null) return false;
            if (text.contains(character.getName())) return true;
            // Analysis stores explicit script aliases as comma-separated text. Never infer an actor from its global description.
            String aliases = Objects.toString(character.getAliases(), "");
            for (String value : aliases.split("[,，、;；\\n]")) {
                String alias = value.trim();
                if (alias.length() >= 2 && !alias.equals("无") && text.contains(alias)) return true;
            }
            return false;
        }
        private static <T> T unique(List<T> values, String kind, String name) {
            if (values.size() != 1) throw new BindingMismatch(kind + "绑定无法唯一核对: " + name);
            return values.get(0);
        }
        private static List<ShortDramaCharacterAppearance> uniquelyDescribedAppearance(
            List<ShortDramaCharacterAppearance> choices, String requested) {
            // Models sometimes echo a rich wardrobe description instead of the exact stage label.
            // Accept it only when substantial literal clauses point to one saved appearance by a
            // clear margin.  Shared face traits alone must never choose between two life stages.
            List<String> fragments = Arrays.stream(requested.split("[：:，,；;。\\n（）()、]"))
                .map(value -> value.replaceAll("\\s+", "").trim())
                .filter(value -> value.length() >= 4)
                .distinct().toList();
            if (fragments.isEmpty()) return List.of();
            record Scored(ShortDramaCharacterAppearance value, int score) {}
            List<Scored> scored = choices.stream().map(value -> {
                String haystack = (Objects.toString(value.getChangeReason(), "") + "\n"
                    + Objects.toString(value.getDescription(), "")).replaceAll("\\s+", "");
                int score = fragments.stream().filter(haystack::contains).mapToInt(String::length).sum();
                return new Scored(value, score);
            }).sorted(java.util.Comparator.comparingInt(Scored::score).reversed()).toList();
            if (scored.isEmpty() || scored.get(0).score() < 24) return List.of();
            int runnerUp = scored.size() > 1 ? scored.get(1).score() : 0;
            if (scored.get(0).score() < runnerUp + 12) return List.of();
            return List.of(scored.get(0).value());
        }
        private static void add(Map<String, Binding> result, String kind, Long id, String name, BaseEntity asset) {
            if (id == null) throw new BindingMismatch("引用资产缺少已保存ID");
            result.put(kind + ":" + id, new Binding(kind, id, name, ShortDramaServiceImpl.planningAssetSignature(List.of(asset))));
        }
    }
    static final class BindingMismatch extends IllegalArgumentException { BindingMismatch(String message) { super(message); } }
    interface Cache {
        String get(String key);
        void put(String key, String value);
    }
    private final Cache cache;
    ShortDramaSceneCheckpoint() {
        this(new Cache() {
            public String get(String key) { return RedisUtils.getCacheObject(key); }
            public void put(String key, String value) { RedisUtils.setCacheObject(key, value, Duration.ofDays(7)); }
        });
    }
    ShortDramaSceneCheckpoint(Cache cache) { this.cache = cache; }

    static Identity identity(Long projectId, ShortDramaScript script, int number, String scene, String previous,
                             String style, String aspect, String model) {
        return identity(projectId, script, number, scene, previous, style, aspect, model, 1);
    }
    static Identity identity(Long projectId, ShortDramaScript script, int number, String scene, String previous,
                             String style, String aspect, String model, int minimumShotSeconds) {
        String context = String.join("\n", Objects.toString(script.getWorldbuilding(), ""), Objects.toString(script.getOutlineText(), ""),
            Objects.toString(previous, ""), Objects.toString(style, ""), Objects.toString(aspect, ""), ShortDramaDirectorSkills.VERSION, ShortDramaDirectorSkills.PACING_VERSION, ShortDramaDirectorSkills.PLANNING_VERSION, "minimumShotSeconds=" + minimumShotSeconds);
        return new Identity(projectId, script.getId(), number, DigestUtil.sha256Hex(scene), DigestUtil.sha256Hex(context), DigestUtil.sha256Hex(model), minimumShotSeconds);
    }

    Receipt compatible(Identity identity, Assets assets, boolean validated) {
        List<Receipt> candidates = new ArrayList<>();
        for (Receipt receipt : read(identity)) {
            try {
                Identity saved = receipt.identity();
                if (!Objects.equals(saved.projectId(), identity.projectId()) || !Objects.equals(saved.scriptId(), identity.scriptId())
                    || saved.sceneNumber() != identity.sceneNumber() || saved.minimumShotSeconds() != identity.minimumShotSeconds() || !Objects.equals(saved.sceneHash(), identity.sceneHash()) || !Objects.equals(saved.contextHash(), identity.contextHash())
                    || !Objects.equals(saved.modelHash(), identity.modelHash()) || receipt.validated() != validated
                    || receipt.panels() == null || receipt.panels().isEmpty()) continue;
                if (Objects.equals(receipt.bindings(), assets.referenced(receipt.panels()))) candidates.add(receipt);
            } catch (Exception ignored) { /* Unproven/corrupt content is never silently treated as reusable. */ }
        }
        return candidates.stream().max(Comparator.comparingLong(Receipt::savedAt)).orElse(null);
    }

    private List<Receipt> read(Identity identity) {
        try {
            String value = cache.get(identity.prefix() + "receipts");
            if (value == null) return List.of();
            return AtlasMediaSupport.OBJECT_MAPPER.readValue(value,
                AtlasMediaSupport.OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Receipt.class));
        } catch (java.io.IOException ignored) { return List.of(); }
    }

    void save(Identity identity, Assets assets, List<StoryboardPanelData> panels, boolean validated, String origin) {
        List<Binding> bindings = assets.referenced(panels);
        Receipt receipt = new Receipt(identity, bindings, validated, origin, System.currentTimeMillis(), panels);
        try {
            List<Receipt> receipts = new ArrayList<>(read(identity));
            receipts.removeIf(r -> r.validated() == validated && Objects.equals(r.bindings(), bindings));
            receipts.add(receipt);
            // One address per scene avoids global scans and tenant-prefix ambiguity. Script generation is already locked.
            if (receipts.size() > 20) receipts = new ArrayList<>(receipts.subList(receipts.size() - 20, receipts.size()));
            cache.put(identity.prefix() + "receipts", AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(receipts));
        } catch (java.io.IOException ex) { throw new IllegalStateException("分场检查点序列化失败，未标记可复用", ex); }
    }
}
