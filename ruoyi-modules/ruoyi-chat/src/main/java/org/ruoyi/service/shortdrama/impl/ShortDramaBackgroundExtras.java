package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;
import org.ruoyi.service.shortdrama.impl.ShortDramaServiceImpl.StoryboardPanelData;
import java.util.*;
import java.util.regex.Pattern;

/** Anonymous extras are a first-frame staging description, never an asset identity or speaker replacement. */
final class ShortDramaBackgroundExtras {
    private ShortDramaBackgroundExtras() {}
    private static final Pattern COUNT = Pattern.compile("^0(?:秒|s)\\s*[：:]\\s*可见人数\\s*[=:：]\\s*(\\d+)(?:\\s*[-–—~～至]\\s*(\\d+))?\\s*[；;]");
    // Role/category vocabulary, rather than a list of identities from any particular drama.
    // A generic occupation alias does not uniquely identify its registered foreground representative.
    private static final String CATEGORY = "军队|敌军|守军|援军|骑兵|步兵|乱兵|溃兵|散兵|兵卒|军士|士兵|军群|群众|百姓|村民|乡亲|路人|行人|人群|众人|同伴|差役|衙役|工匠|宾客|队伍|护卫|兵士|乡勇|徒弟|学徒|学生|工人|农民|农户|商人|顾客|乘客|医护人员|护士|医生|警员|警察|记者|店员|伙计|侍卫|侍从|仆人|仆役|富户|宫女|老人|老汉|少年|少女|儿童";
    private static final Pattern GROUP = Pattern.compile(CATEGORY);
    private static final Pattern GENERIC_NAME = Pattern.compile("^(?:(?:前景|后景|年轻|年老|老|小|普通|匿名|无名|一名|领队的|排首的|带头的|男|女))*(?:" + CATEGORY + ")$");
    static boolean genericRole(String name) { return name != null && GENERIC_NAME.matcher(name.trim()).matches(); }
    static String readText(com.fasterxml.jackson.databind.JsonNode continuity) {
        var value = continuity.path("background_extras");
        if (!value.isMissingNode() && !value.isNull() && !value.isTextual())
            throw new IllegalArgumentException("background_extras必须为文字字段");
        return value.asText("");
    }
    record FirstFrame(int minimum, int maximum, String text) { boolean visible() { return maximum > 0; } }
    static FirstFrame parse(String text) {
        if (text == null || text.isBlank()) return new FirstFrame(0, 0, "");
        if (text.length() > 4000) throw new IllegalArgumentException("background_extras不能超过4000字符");
        var count = COUNT.matcher(text.trim());
        if (!count.find()) throw new IllegalArgumentException("background_extras须以0秒：可见人数=整数或范围；开头");
        int min, max;
        try { min = Integer.parseInt(count.group(1)); max = count.group(2) == null ? min : Integer.parseInt(count.group(2)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("匿名群演首帧人数超出可用范围", e); }
        if (min < 0 || max < min || max > 1_000_000) throw new IllegalArgumentException("匿名群演首帧人数或范围无效");
        if (Pattern.compile("(?i)character[_-]?id|appearance[_-]?id|asset[_-]?id|角色\\s*ID|@image").matcher(text).find())
            throw new IllegalArgumentException("匿名群演不能使用角色ID或具名角色参考图");
        // An explicit zero count describes an empty first-frame extras roster.
        // There is no visible person whose costume or staging needs validation.
        if (max == 0) return new FirstFrame(0, 0, text.trim());
        for (String field : List.of("身份服饰", "位置", "姿态动作")) {
            if (fieldValue(text, field).isBlank()) throw new IllegalArgumentException("background_extras缺少" + field);
        }
        if (!text.contains("匿名") && !text.contains("无名")) throw new IllegalArgumentException("background_extras必须明确匿名群演，不创建命名角色");
        return new FirstFrame(min, max, text.trim());
    }
    static String frameGuidance(String text) {
        FirstFrame extras = parse(text);
        if (!extras.visible()) return "0秒匿名群演可见人数=0，不提前画入之后才入场的人物。";
        return extras.text() + "\n以上只定义0秒静态人数、位置和姿态；不提前执行后续入场、攀登完成或冲锋结果。匿名群演保持不同脸型与服饰细节，不能复用任何具名角色的身份参考。";
    }
    static void validateScene(String scene, List<ShortDramaCharacter> registered, List<StoryboardPanelData> panels) {
        String anonymousSource = Objects.toString(scene, "").replaceAll("「[^」]*」", "");
        for (var c : registered) {
            if (c.getName() != null) {
                // Remove an explicit representative, but preserve category plurals such as “徒弟们”.
                anonymousSource = removeRepresentative(anonymousSource, c.getName());
            }
            for (String alias : Objects.toString(c.getAliases(), "").split("[,，、;；\\n]"))
                if (uniqueAlias(alias)) anonymousSource = anonymousSource.replace(alias.trim(), "");
        }
        boolean sourceHasGroup = GROUP.matcher(anonymousSource).find();
        for (var panel : panels) {
            String text = panel.getBackgroundExtras();
            if (!parse(text).visible()) continue;
            if (!sourceHasGroup) throw new ShortDramaSceneCheckpoint.BindingMismatch("本场原文未安排匿名群演，不能借background_extras增加人物");
            String identity = fieldValue(text, "身份服饰");
            String staging = fieldValue(text, "位置") + ";" + fieldValue(text, "姿态动作");
            Set<String> visibleNamed = new HashSet<>(panel.getPresentCharacters() == null ? List.of() : panel.getPresentCharacters());
            for (var c : registered) {
                if (c.getName() != null && !genericRole(c.getName())) checkIdentityReference(c.getName(), c.getName(), identity, staging, visibleNamed);
                for (String alias : Objects.toString(c.getAliases(), "").split("[,，、;；\\n]"))
                    if (uniqueAlias(alias)) checkIdentityReference(c.getName(), alias.trim(), identity, staging, visibleNamed);
            }
        }
    }
    private static void checkIdentityReference(String canonical, String name, String identity, String staging, Set<String> visibleNamed) {
        if (identity.contains(name)) throw new ShortDramaSceneCheckpoint.BindingMismatch("background_extras不能隐藏具名角色或CG：" + canonical + "（" + name + "）");
        // Position/anti-cloning instructions may refer to an already visible registered foreground
        // actor. They cannot turn an absent/later actor or CG into a zero-second physical landmark.
        if (staging.contains(name) && !visibleNamed.contains(canonical))
            throw new ShortDramaSceneCheckpoint.BindingMismatch("background_extras定位参照未在0秒具名名册中：" + canonical + "（" + name + "）");
    }
    private static String fieldValue(String text, String field) {
        var value = Pattern.compile(Pattern.quote(field) + "\\s*[=:：]([^；;\\n]*)").matcher(text);
        return value.find() ? value.group(1).trim() : "";
    }
    private static boolean uniqueAlias(String alias) {
        String value = alias.trim();
        return value.length() >= 2 && !value.equals("无") && !genericRole(value);
    }
    private static String removeRepresentative(String source, String name) {
        var matcher = Pattern.compile(Pattern.quote(name)).matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String before = source.substring(Math.max(0, matcher.start() - 30), matcher.start());
            String after = source.substring(matcher.end(), Math.min(source.length(), matcher.end() + 3));
            boolean plural = genericRole(name) && (after.matches("^(?:们|群|队).*")
                || before.matches("(?s).*(?:[二两三四五六七八九十百千万]+|[2-9]|[1-9][0-9]+)[名个位户][^。；;\\n]{0,18}")
                || before.matches("(?s).*(?:其余|其他|一群|众多|数名|几名|多名|全体|一些|一队)[^。；;\\n]{0,18}"));
            matcher.appendReplacement(result, plural ? java.util.regex.Matcher.quoteReplacement(name) : "");
        }
        matcher.appendTail(result); return result.toString();
    }
    static String promptRules() {
        return "\n【登记身份与匿名群演：最高优先级】characters、present_characters、performance_beats.name只允许当前原文明确安排的精确登记角色名，包含明确可见的必要CG；全剧角色库与上一场名册不是本场出演许可。"
            + "原对白画外声保留原说话人/source_text与声源，不为画外声强画实体；同伴、乱兵、溃兵、百姓等未登记匿名群演绝不能自造群像角色名、别名、ID或借用某个头目的身份。"
            + "匿名群演在description、起止状态中写可见动作，且用单个background_extras文字字段明确0秒人数、身份服饰、位置和静态姿态；具名/登记演员名单与匿名群演分开，不要求每个可见人头都成为角色资产。"
            + "字段格式：0秒：可见人数=6-8；身份服饰=匿名乱兵，粗布军装、不同脸型；位置=梯架两侧；姿态动作=躬身扶梯，尚未登顶。"
            + "无匿名群演时字段为空；后续才入场时0秒人数写0，后续动作放video_prompt和story_action。身份服饰段禁止把具名、声源或CG藏入匿名身份；位置/姿态可引用present_characters中0秒已可见的登记演员作为定位，引用不产生新的演员。差役、乡勇、徒弟等职业泛称可同时用于登记前景代表与其余匿名同类，泛称不是每个人的唯一姓名。\n"
            + "【单镜地点精确绑定】每镜location只能逐字选择一条真实登记场景名，不自造、缩写、加后缀或把两个名字用及/和/斜杠合并。"
            + "原文多地点标题只是本场预算说明，逐镜按当前主体所在空间选择对应资产；城头与城外郊野分别绑定各自地点，必要时拆镜并做桥接，同一原文预算场编号保持。\n";
    }
}
