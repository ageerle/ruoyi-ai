package org.ruoyi.service.shortdrama.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** Structural production review, not a minimum-length score or a substitute for visual QA. */
@Slf4j
public final class ShortDramaVideoPromptReview {
    private ShortDramaVideoPromptReview() { }
    private static final String[] SECTIONS = {"参考与边界", "镜头与运镜", "时间节拍", "对白与口型",
        "光色与材质", "声音", "结束与承接", "约束"};
    private static final Pattern RANGE = Pattern.compile("(?<![\\d.])(\\d+(?:\\.\\d+)?)\\s*(?:秒|s)?\\s*[-–—~至到]\\s*(\\d+(?:\\.\\d+)?)\\s*(?:秒|s)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTE = Pattern.compile("「([^」]+)」|“([^”]+)”");
    private static final Pattern SOURCE_DIALOGUE = Pattern.compile("「([^」]+)」|“([^”]+)”|(?m)^\\s*([\\p{IsHan}A-Za-z0-9（）()· ]{1,24})\\s*[:：]\\s*([^\\r\\n]+)");
    private static final Pattern VISUAL_TEXT_CONTEXT = Pattern.compile(
        ".*(?:屏幕|界面|查询栏|字幕|叠字|光字|画面文字|告示|牌匾|标题|题字|字样|标注|显示|内容为).*");
    private static final Pattern PLACEHOLDER = Pattern.compile("^(?:待补充|待生成|待设计|同上|略|TODO|暂无|未填写)[。；;\\s]*$", Pattern.CASE_INSENSITIVE);

    public static List<String> issues(String prompt, Integer duration, String source) {
        List<String> issues = new ArrayList<>();
        String text = prompt == null ? "" : prompt;
        boolean natural = !Pattern.compile("【[^】]*(?:参考与边界|时间节拍|动作节拍|时间轴|时序动作|时间分配)[^】]*】").matcher(text).find()
            && !RANGE.matcher(text).find();
        if (natural) {
            if (text.isBlank() || PLACEHOLDER.matcher(text.strip()).matches()) issues.add("缺少可执行的视频提示词");
            if (duration != null && duration < 1) issues.add("指定视频时长须为正整数");
            if (!text.matches("(?s).*(固定|锁定|推|拉|摇|移|跟|手持|微晃|收近|近景|中景|全景|特写|俯拍|仰拍|对焦).*"))
                issues.add("须描述景别、机位或运镜");
            // Remove camera prose before checking performance: a slow push alone is not an actor action.
            String action = text.replaceAll("(?:镜头|摄影机|机位)[^。；\\n]*[。；]?", "")
                .replaceAll("(?:远景|全景|中景|近景|特写)(?:缓推|缓拉|慢推|慢拉|固定)[。；]?", "");
            if (!action.matches("(?s).*(抬|垂|转|握|松|递|拿|放|落|提|摸|触|望|注视|停|保持|呼吸|眨|睁|闭|说|问|答|笑|迈|退|伸|翻|压|扶|拂|颤|浮|悬|投影|发光|熄|闪|流|吹|雨|雪|淡入|滚|推|撑|抓|攥|跑|颠|跌|缩|扇|掀|踏|扭|挥).*"))
                issues.add("须描述可见动作或反应，不能只写剧情结果");
            if (!text.matches("(?s).*(烛光|烛火|月光|暖光|冷光|侧光|背光|柔光|顶光|自然光|天光|光源|光色|灯光|光线|逆光|日光|阳光|照明).*"))
                issues.add("须说明当前光源或光色");
            if (!text.matches("(?s).*(无对白|无台词|静默|环境声|呼吸声|脚步声|画外|旁白|声源|声线|低声|轻声|同期声|口型|拟音|雨声|风声|哭声|咳嗽|轰鸣|回响|炮响|声响|[\\p{IsHan}]{1,12}(?:声|轻响)).*"))
                issues.add("须说明对白声源或环境声");
        } else {
        for (String section : SECTIONS) {
            String body = section(text, section);
            if (body.isBlank() || PLACEHOLDER.matcher(body).matches()) issues.add("缺少可执行的【" + section + "】内容");
        }
        String camera = section(text, "镜头与运镜");
        if (!camera.isBlank() && !camera.matches("(?s).*(固定|锁定|推|拉|摇|移|跟|环绕|手持|升|俯冲).*"))
            issues.add("镜头与运镜须明确固定机位或一种主导运动");
        reviewTimeline(section(text, "时间节拍"), duration, issues);
        }
        String dialogue = natural ? text : section(text, "对白与口型");
        List<String> spokenLines = sourceDialogue(source);
        int cursor = 0;
        for (String line : spokenLines) {
            int at = dialogue.indexOf(line, cursor);
            if (at < 0) issues.add("对白缺失或乱序：「" + line + "」");
            else {
                String speaker = sourceSpeaker(source, line);
                if (!speaker.isBlank() && !dialogue.substring(cursor, at).contains(speaker))
                    issues.add("对白须保留发言人" + speaker + "：「" + line + "」");
                cursor = at + line.length();
            }
        }
        if (!natural && !dialogue.isBlank() && !dialogue.matches("(?s).*(口型|画外|心声|旁白|声源|对白|说话|静默|静听|不开口).*"))
            issues.add("对白与口型须明确声源和开口规则");
        if (spokenLines.isEmpty() && !dialogue.isBlank() && QUOTE.matcher(dialogue).find())
            issues.add("原文无标注对白，须核对是否擅加台词");
        return List.copyOf(issues);
    }

    /** Keep the model's staging while making the dialogue block mechanically match source text. */
    static String normalizeDialogue(String prompt, String source) {
        String text = prompt == null ? "" : prompt;
        var heading = Pattern.compile("【[^】\\n]*(?:对白与口型|对白与声源|台词与口型|对白|口型|台词)[^】\\n]*】").matcher(text);
        if (!heading.find()) return text;
        int start = heading.end(), end = text.indexOf("【", start);
        if (end < 0) end = text.length();
        List<String> lines = sourceDialogue(source);
        String body;
        if (lines.isEmpty()) {
            body = "本镜无对白；所有可见人物保持闭口，仅保留原文已有的环境声与动作声。";
        } else {
            StringBuilder exact = new StringBuilder();
            for (String line : lines) {
                String speaker = sourceSpeaker(source, line);
                if (exact.length() > 0) exact.append('；');
                exact.append(speaker.isBlank() ? "原文声源" : speaker)
                    .append("说「").append(line).append("」，按原顺序发声并同步对应口型");
            }
            body = exact.append("；未发言者闭口静听，画外声不驱动画面人物口型。").toString();
        }
        return text.substring(0, start) + body + text.substring(end);
    }

    static List<String> sourceDialogue(String source) {
        String normalized = (source == null ? "" : source).replace("\\n", "\n");
        List<String> lines = new ArrayList<>();
        var matcher = SOURCE_DIALOGUE.matcher(normalized);
        while (matcher.find()) {
            if (matcher.group(1) != null || matcher.group(2) != null) {
                int from = Math.max(0, matcher.start() - 24), to = Math.min(normalized.length(), matcher.end() + 24);
                if (VISUAL_TEXT_CONTEXT.matcher(normalized.substring(from, to)).matches()) continue;
                lines.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            }
            else {
                String speaker = matcher.group(3).strip();
                if (speaker.matches(".*(?:起始状态|结束状态|场景|镜头|画面|音效|动作|提示|环境|承接|道具).*")) continue;
                String line = matcher.group(4).strip();
                var quoted = QUOTE.matcher(line);
                boolean hasQuote = false;
                while (quoted.find()) { lines.add(quoted.group(1) != null ? quoted.group(1) : quoted.group(2)); hasQuote = true; }
                if (!hasQuote && !line.isBlank()) lines.add(line);
            }
        }
        return List.copyOf(lines);
    }

    private static String sourceSpeaker(String source, String line) {
        var matcher = SOURCE_DIALOGUE.matcher((source == null ? "" : source).replace("\\n", "\n"));
        while (matcher.find()) if (matcher.group(3) != null && matcher.group(4).contains(line))
            return matcher.group(3).replaceAll("（[^）]*）|\\([^)]*\\)", "")
                .replaceFirst("(?:回答|低声说|说|问|答|心声|画外音)$", "").strip();
        return ""; // Unlabelled quoted prose needs human speaker review, not an invented role.
    }

    static String section(String text, String name) {
        String aliases = switch (name) {
            case "参考与边界" -> "参考与边界|参考锚定|参考与空间|身份与空间|起始锚定";
            case "镜头与运镜" -> "镜头与运镜|摄影与运镜|机位与运镜|镜头调度|摄影机";
            case "时间节拍" -> "时间节拍|动作节拍|时间轴|时序动作|时间分配";
            case "对白与口型" -> "对白与口型|对白与声源|台词与口型|对白|口型|台词";
            case "光色与材质" -> "光色与材质|光影与材质|光色|光影|光线|材质";
            case "声音" -> "声音|环境声|音效|声场|声源";
            case "结束与承接" -> "结束与承接|终态与承接|结束状态|终点与承接|镜尾";
            case "首帧实况" -> "首帧实况";
            default -> "约束|限制|负面约束|禁止";
        };
        // Synonyms and combined headings are accepted; no exact eight-label UI requirement.
        var heading = Pattern.compile("【[^】\\n]*(?:" + aliases + ")[^】\\n]*】").matcher(text);
        if (!heading.find()) return "";
        int start = heading.end(), end = text.indexOf("【", start);
        return text.substring(start, end < 0 ? text.length() : end).strip();
    }

    private static void reviewTimeline(String timeline, Integer duration, List<String> issues) {
        if (duration == null) {
            // A legacy timeline can still be reviewed without imposing its estimate on the provider.
            var declared = RANGE.matcher(timeline);
            while (declared.find()) duration = (int) Math.ceil(Double.parseDouble(declared.group(2)));
        }
        if (duration == null || duration < 1) { issues.add("时间节拍缺少有效动作区间"); return; }
        var ranges = RANGE.matcher(timeline);
        double previous = 0; int count = 0;
        List<Integer> ends = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (ranges.find()) {
            double from = Double.parseDouble(ranges.group(1)), to = Double.parseDouble(ranges.group(2));
            if (Math.abs(from - previous) > 0.05 || to <= from || to > duration + 0.05)
                issues.add("时间节拍须连续、无重叠并落在0至" + duration + "秒内");
            previous = to; count++; starts.add(ranges.start()); ends.add(ranges.end());
        }
        if (count == 0 || Math.abs(previous - duration) > 0.05) issues.add("时间节拍未从0秒完整覆盖至" + duration + "秒");
        for (int i = 0; i < count; i++) {
            String beat = timeline.substring(ends.get(i), i + 1 < count ? starts.get(i + 1) : timeline.length())
                .replaceAll("^[\\s:：;；,，\\[\\]（()）-]+|[\\s;；]+$", "").strip();
            if (beat.isBlank() || PLACEHOLDER.matcher(beat).matches()) issues.add("第" + (i + 1) + "段时间缺少具体动作或反应");
        }
    }

    public static void validate(Integer number, String prompt, Integer duration, String source) {
        if (prompt == null || prompt.isBlank()) throw new IllegalStateException("镜头" + number + "尚无视频提示词");
        List<String> issues = issues(prompt, duration, source);
        if (!issues.isEmpty()) log.info("镜头{}导演稿审阅建议（继续保存或生成）：{}", number, String.join("；", issues));
    }

    /** Used by the one-keyframe fast path, which must retain speech, duration and skills. */
    static String anchoredPrompt(String prompt, Integer duration, String source, String start, String end) {
        String reviewedStart = section(nullToEmpty(prompt), "首帧实况");
        String actualStart = reviewedStart.isBlank() ? nullToEmpty(start) : reviewedStart;
        return ShortDramaDirectorSkills.load("video-prompt", "video-continuity")
            + "本镜唯一画面参考是image 1，索引只说明实际已绑定的首帧。锁定身份、服装、构图、左右关系、持物手和光源，单一连续镜头，不复制多视图。\n"
            + "【绑定起止状态】起点：" + actualStart + "；终点：" + nullToEmpty(end)
            + (duration == null
                ? "\n【时长与内容】未指定视频秒数，完整执行原剧情、对白及动作顺序，不按制作估算删减。\n"
                : "\n【用户指定视频时长】" + duration + "秒\n") + nullToEmpty(prompt)
            + "\n【已审阅剧情与声音原文】\n" + nullToEmpty(source)
            + "\n只执行当前镜头。对白原话、发言人、先后顺序与声音位置以原文为准；画外声/心声不驱动画面人物口型；不得让听者代说。无特别约定不加音乐，不显示下一镜剧情。";
    }
    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
