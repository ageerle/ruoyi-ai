package org.ruoyi.service.shortdrama.impl;

/** Per-request location corrections constrain an image candidate, never the saved world or geometry. */
final class ShortDramaLocationArtPrompt {
    private ShortDramaLocationArtPrompt() {}

    static boolean isRevision(String referencePurpose, String requirements) {
        String purpose = referencePurpose == null || referencePurpose.isBlank() ? "location" : referencePurpose.trim();
        // identity was the shared DTO's historical default for all image requests.
        if (!"location".equals(purpose) && !"identity".equals(purpose) && !"location_revision".equals(purpose))
            throw new IllegalArgumentException("地点图参考用途须为location或location_revision，不能使用角色身份修订用途");
        if (requirements != null && requirements.length() > 4000) throw new IllegalArgumentException("修订要求不能超过4000字符");
        boolean revision = requirements != null && !requirements.isBlank();
        if ("location_revision".equals(purpose) && !revision) throw new IllegalArgumentException("地点修订候选必须填写明确的修订要求");
        return revision;
    }

    static String reference(String name, String description, String artStyle, String worldbuilding, String requirements) {
        String base = ShortDramaAssetWorldPrompt.location(name, description, artStyle, worldbuilding);
        if (!isRevision(null, requirements)) return base;
        return base + "\n【用途：当前地点的局部修订候选，尚未批准连续性】\n"
            + "本轮只作用于图片，不改写已冻结的场景名称、场景文案、世界观或镜头连续性。"
            + "有参考图时只绑定其建筑几何、空间尺度、入口与窗的位置、道路走向、陈设布局及未指定修改的材质；"
            + "无参考图时按上述已冻结地点文案重建同一空间。不得搬动建筑、改造道路、改变空间规模或增加人物。\n"
            + "【局部要求优先级】本轮明确列出的时间、季候、光色或物件清理要求优先于参考图和冻结文案中的旧时间、旧光色及被指定删除的错误物件。"
            + "参考图的夕阳、夜色、天气与错误物件不锁定本轮结果；不能为了保留参考而恢复已明确要求移除的物件。"
            + "未列出的几何、结构、身份与可见内容继续遵守已冻结地点，不能从世界观补入未来装备或新的剧情成果。\n"
            + "【本轮明确修订要求】\n" + requirements.trim()
            + "\n修订结果仅追加为待审候选，须人工审阅并明确选择后才成为后续镜头的批准参考，不自动替换已有批准图片。";
    }
}
