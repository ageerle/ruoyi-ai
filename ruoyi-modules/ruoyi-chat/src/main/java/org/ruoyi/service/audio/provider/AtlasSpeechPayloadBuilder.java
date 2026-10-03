package org.ruoyi.service.audio.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.service.media.AtlasMediaSupport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Model-specific Atlas speech contracts; Gemini is not a Seed Audio reference request. */
public final class AtlasSpeechPayloadBuilder {
    public static final String GEMINI_MODEL = "google/gemini-3.1-flash-tts";
    public static final String DEFAULT_GEMINI_VOICE = "Charon";
    public static final String DRAMA_INSTRUCTIONS = "使用自然、有力的中文男声，保持同一人的声线；"
        + "根据原句表达情绪，语速适合短剧表演，不过度播报，不增加台词或旁白。";
    private static final Set<String> GEMINI_VOICES = Set.of(
        "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Aoede", "Callirrhoe", "Autonoe",
        "Enceladus", "Iapetus", "Umbriel", "Algieba", "Despina", "Erinome", "Algenib", "Rasalgethi",
        "Laomedeia", "Achernar", "Alnilam", "Schedar", "Gacrux", "Pulcherrima", "Achird", "Zubenelgenubi",
        "Vindemiatrix", "Sadachbia", "Sadaltager", "Sulafat");
    private static final Pattern AUDIO_LABEL = Pattern.compile("(?i)@audio\\d+\\b\\s*[:：]?[\\t ]*");

    private AtlasSpeechPayloadBuilder() { }

    public static boolean isGeminiTts(String model) {
        return GEMINI_MODEL.equals(model);
    }

    public static ObjectNode build(AudioContext context) {
        String model = context.getChatModelVo().getModelName();
        ObjectNode payload = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model);
        if (isGeminiTts(model)) {
            String voice = StrUtil.blankToDefault(context.getVoice(), DEFAULT_GEMINI_VOICE);
            if (!GEMINI_VOICES.contains(voice)) {
                throw new IllegalArgumentException("Gemini TTS 音色必须是官方 prebuilt voice，不能使用 Seed Audio 的 speaker 名称");
            }
            String spoken = spokenText(context.getInput(), List.of());
            if (StrUtil.isBlank(spoken)) throw new IllegalArgumentException("语音正文不能为空");
            String direction = StrUtil.blankToDefault(context.getInstructions(), DRAMA_INSTRUCTIONS);
            // Atlas exposes one text field. Gemini accepts natural-language performance instructions in it.
            payload.put("text", "单人配音任务。仅朗读下面“正文”中的原句，不朗读任务说明、表演指令、角色名或标签。\n"
                + "表演指令：" + direction + "\n正文：\n" + spoken);
            payload.put("voice", voice);
            return payload;
        }

        payload.put("text", context.getInput());
        payload.put("format", StrUtil.blankToDefault(context.getResponseFormat(), "mp3"));
        List<Map<String, String>> refs = context.getReferences();
        if (refs != null && !refs.isEmpty()) {
            if (refs.size() > 3) throw new IllegalArgumentException("Seed Audio最多3条声音参考或1张图像参考");
            ArrayNode arr = payload.putArray("references");
            boolean image = false;
            for (Map<String, String> ref : refs) {
                long sources = List.of("speaker", "audioUrl", "audioData", "imageUrl", "imageData").stream()
                    .filter(key -> StrUtil.isNotBlank(ref.get(key))).count();
                if (sources != 1) throw new IllegalArgumentException("每条 Seed Audio 参考必须且只能包含一种来源");
                image |= StrUtil.isNotBlank(ref.get("imageUrl")) || StrUtil.isNotBlank(ref.get("imageData"));
                ObjectNode r = arr.addObject();
                if (StrUtil.isNotBlank(ref.get("speaker"))) r.put("speaker", ref.get("speaker"));
                if (StrUtil.isNotBlank(ref.get("audioUrl"))) r.put("audio_url", ref.get("audioUrl"));
                if (StrUtil.isNotBlank(ref.get("audioData"))) r.put("audio_data", ref.get("audioData"));
                if (StrUtil.isNotBlank(ref.get("imageData"))) r.put("image_data", ref.get("imageData"));
                if (StrUtil.isNotBlank(ref.get("imageUrl"))) r.put("image_url", ref.get("imageUrl"));
            }
            if (image && refs.size() != 1) throw new IllegalArgumentException("Seed Audio图像参考最多1张，不能与声音参考混用");
        } else if (StrUtil.isNotBlank(context.getVoice())) {
            payload.putArray("references").addObject().put("speaker", context.getVoice());
        }
        if (context.getSampleRate() != null) payload.put("sample_rate", context.getSampleRate());
        if (context.getPitchRate() != null) payload.put("pitch_rate", context.getPitchRate());
        if (context.getSpeechRate() != null) payload.put("speech_rate", context.getSpeechRate());
        if (context.getLoudnessRate() != null) payload.put("loudness_rate", context.getLoudnessRate());
        return payload;
    }

    /** Remove production speaker labels, never arbitrary text preceding a colon inside a spoken sentence. */
    public static String spokenText(String input, Collection<String> speakerNames) {
        if (input == null) return "";
        String text = AUDIO_LABEL.matcher(input).replaceAll("");
        for (String alias : List.of("旁白", "画外音", "Narrator", "Narration")) {
            text = removeSpeakerPrefix(text, alias);
        }
        text = text.replaceAll("(?im)^[\\t ]*Speaker[\\t ]*\\d+[\\t ]*[:：][\\t ]*", "");
        if (speakerNames != null) {
            for (String name : speakerNames) {
                if (StrUtil.isNotBlank(name)) text = removeSpeakerPrefix(text, name);
            }
        }
        return text.trim();
    }

    private static String removeSpeakerPrefix(String text, String name) {
        return text.replaceAll("(?m)^[\\t ]*" + Pattern.quote(name)
            + "[\\t ]*(?:[（(][^）)\\r\\n]*[）)][\\t ]*)?[:：][\\t ]*", "");
    }
}
