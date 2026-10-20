package org.ruoyi.service.shortdrama.impl;

import java.util.Objects;
import java.util.regex.Pattern;

/** Distill selected skills without a fixed length score or truncating necessary shot content. */
final class ShortDramaVideoPromptPolicy {
    static final String VERSION = "skill-essence-video-v1";
    private ShortDramaVideoPromptPolicy() { }

    static int characters(String text) {
        return (int) Objects.toString(text, "").codePoints().filter(c -> !Character.isWhitespace(c)).count();
    }

    static String draftingRules() {
        return """
            【按当前镜头提炼技能，删除冗余】
            完整skills供规划与审阅推理，不复制进video_prompt。审美skill提炼本镜的画风、具体光色和必要材质；导演skill提炼本镜的表演、关注点、机位和节奏。不同技能的适用要点融入具体动作，不叠加手册、相同约束或互相冲突的画风。
            字数由本镜真实出镜、对白、动作链、效果阶段、声源和交镜关系决定，不设置统一300/500字上限，不按scene_type套固定容量，也不因超字数自动重写或截断。简短行军与多角色对白、特效因果镜不能使用同一预算。
            保留当前空间与必要站位、动作发起→作用对象→可见结果→关键反应、摄影关注变化、具体光色和声音、结束状态。多人对话保留逐句原话、声源和回应；效果镜保留触发、展开、破坏或余波。涉及手与物接触、受力、投影可见性和连续性时，必要细节不能删。
            已有参考图不重复整套静态外貌和衣装，但必须保留本镜变化、持物、接触和会影响动作的空间条件。微动作仅在承载意图或因果时展开，不能机械省掉表演。
            删除同义形容词、重复摄影/美术段、反复锁定声明、与本镜无关的禁令、下一镜完整剧情、资料来源和技能版本。时长、分辨率、画幅留在参数；素材索引由后端绑定。原话及有效导演信息完整，重复信息只表达一次。
            """;
    }

    /** The director text is authoritative; supplement only genuinely missing original speech. */
    static String execution(String prompt, String source) {
        String text = Objects.toString(prompt, "");
        StringBuilder missing = new StringBuilder();
        // A line can be split around an acting cue: 「小翠，」...「我就知道。」 is one original utterance.
        var quotes = Pattern.compile("「([^」]+)」|“([^”]+)”").matcher(text);
        StringBuilder spoken = new StringBuilder();
        while (quotes.find()) spoken.append(quotes.group(1) == null ? quotes.group(2) : quotes.group(1));
        int cursor = 0, spokenCursor = 0;
        for (String line : ShortDramaVideoPromptReview.sourceDialogue(source)) {
            int found = text.indexOf(line, cursor);
            int quoted = spoken.indexOf(line, spokenCursor);
            if (found >= 0 || quoted >= 0) {
                if (found >= 0) cursor = found + line.length();
                if (quoted >= 0) spokenCursor = quoted + line.length();
                continue;
            }
            String speaker = ShortDramaVideoPromptReview.sourceSpeaker(source, line);
            missing.append(speaker.isBlank() ? "原文对白" : speaker).append("：「").append(line).append("」\n");
        }
        return missing.isEmpty() ? text : text + "\n[补充原对白]\n" + missing;
    }
}
