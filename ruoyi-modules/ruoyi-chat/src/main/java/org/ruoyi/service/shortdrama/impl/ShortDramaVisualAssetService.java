package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.tenant.helper.TenantHelper;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;

/** Identity/location masters remain stable; props and shot compositions have separate lifecycles. */
@Service @RequiredArgsConstructor @Slf4j
public class ShortDramaVisualAssetService {
    private final ShortDramaVisualAssetMapper assets;
    private final ShortDramaProjectMapper projects;
    private final ShortDramaScriptMapper scripts;
    private final ShortDramaStoryboardMapper boards;
    private final ShortDramaCharacterMapper characters;
    private final ShortDramaCharacterAppearanceMapper appearances;
    private final ShortDramaLocationMapper locations;
    private final IChatModelService models;
    private final ChatServiceFactory chats;
    private final ImageServiceFactory images;
    private final AtlasPredictionService predictions;
    private final ShortDramaSkillCatalog skillCatalog;
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ExecutorService coordinators = Executors.newFixedThreadPool(2);
    private final ExecutorService imageWorkers = Executors.newFixedThreadPool(32);
    private final ConcurrentHashMap<Long, String> running = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> errors = new ConcurrentHashMap<>();
    @PreDestroy void close() { coordinators.shutdownNow(); imageWorkers.shutdownNow(); }

    private ShortDramaProject owner(Long id, Long user) {
        var p = projects.selectById(id);
        if (p == null || !Objects.equals(user, p.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
        return p;
    }
    private List<ShortDramaVisualAsset> rows(Long id) {
        return assets.selectList(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,id)
            .ne(ShortDramaVisualAsset::getStatus,"obsolete").orderByAsc(ShortDramaVisualAsset::getId));
    }
    static final String PROP_EXTRACTION_PROMPT = "从剧本提取反复出现、跨镜必须保持外观一致的关键实物道具。不是人物、建筑、屏幕截图或人物动作。只要3-10项，若不足3项按实际数量。合并同一个物体的别称，不把同型号的不同物体强行合并。描述外形材质颜色磨损，不新增品牌或剧情；账页和屏幕清晰文字由后期素材替换。返回JSON数组 [{\"name\":\"物体名\",\"description\":\"可生成单一道具定妆图的描述\",\"shot_numbers\":[1,2]}]。shot_numbers只填该实物真实出现的镜头，不按同名词误配。剧本：\n";
    static final String SCRIPT_PROP_EXTRACTION_PROMPT = "从已审阅剧本提取反复出现、必须保持外观一致的关键实物道具。不是人物、场景建筑、屏幕截图或人物动作。按实际数量，不凑数；只描述剧本可见的形制、材质、颜色和磨损，不新增品牌、剧情或未来阶段成果。合并同一实物的别称。只返回JSON数组 [{\"name\":\"道具名\",\"description\":\"单物体定妆图描述\"}]。此阶段没有分镜，不编造镜号。剧本：\n";

    /** Script analysis writes descriptions only. Approved props and their task IDs remain unchanged. */
    public void analyzeScriptProps(Long id, Long scriptId, String model, Long user) {
        owner(id,user);
        var script=scripts.selectById(scriptId);
        if(script==null || !id.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var configured=models.selectModelByName(model);
        if(configured==null || !"chat".equals(configured.getCategory())) throw new IllegalArgumentException("后台尚未配置可用写作模型");
        var project=projects.selectById(id);
        var snapshot=skillCatalog.snapshot(project);
        var chat=chats.getOriginalService(ShortDramaServiceImpl.shortDramaProviderCode(configured.getProviderCode(),model)).buildChatModel(configured);
        String raw=chat.chat(SCRIPT_PROP_EXTRACTION_PROMPT + script.getScriptText() + ShortDramaScriptPreparation.worldContext(script) + snapshot.direction());
        int start=raw.indexOf('['),end=raw.lastIndexOf(']');
        if(start<0 || end<start) throw new IllegalStateException("道具分析未返回有效列表，原道具保留");
        try {
            var extracted=JSON.readTree(raw.substring(start,end+1));
            if(!extracted.isArray() || extracted.size()>30) throw new IllegalArgumentException("道具清单格式不正确");
            for(var prop:extracted) if(prop.path("name").asText().isBlank() || prop.path("name").asText().length()>80
                || prop.path("description").asText().isBlank() || prop.path("description").asText().length()>12000)
                throw new IllegalArgumentException("道具清单缺少名称或描述");
            skillCatalog.verify(snapshot,projects.selectById(id));
            Set<String> existing=rows(id).stream().filter(a->"prop".equals(a.getKind())).map(a->a.getTitle()).collect(java.util.stream.Collectors.toSet());
            for(var prop:extracted) {
                String name=prop.path("name").asText().trim();
                if(!existing.add(name)) continue;
                saveProp(id,name,prop.path("description").asText().trim(),List.of(),null,user);
            }
        } catch(java.io.IOException e) { throw new IllegalStateException("道具清单无法解析，原道具保留",e); }
    }

    public record AssetView(Long id,Long storyboardId,String kind,String title,String prompt,String status,
            String imageUrl,String model,String predictionId,String errorMessage,String referenceImageUrl,boolean undoAvailable) { }
    /** Called inside the asset-analysis transaction, after the entire result has been validated. */
    void saveAnalyzedProps(Long id, List<JsonNode> props, Long user) {
        Set<String> existing = rows(id).stream().filter(a -> "prop".equals(a.getKind()))
            .map(a -> a.getTitle().trim().toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        for (var prop : props) {
            String name = prop.path("name").asText().trim();
            if (existing.add(name.toLowerCase(java.util.Locale.ROOT)))
                saveProp(id, name, prop.path("description").asText().trim(), List.of(), null, user);
        }
    }
    public Map<String,Object> status(Long id, Long user) {
        var project=owner(id,user);
        var rows = rows(id);
        Set<String> undoableProps = new HashSet<>();
        for (var row : rows) if ("archived_prop".equals(row.getKind())) {
            String key = Objects.toString(row.getAssetKey(), "");
            int history = key.indexOf("@history-");
            if (history > 0) undoableProps.add(key.substring(0, history));
        }
        return Map.of("outdated","script_changed".equals(project.getStatus()),"running",running.containsKey(id),"phase",running.getOrDefault(id,"idle"),
            "error",errors.getOrDefault(id,""),"assets",rows.stream().map(a->new AssetView(a.getId(),a.getStoryboardId(),a.getKind(),a.getTitle(),a.getPrompt(),a.getStatus(),a.getImageUrl(),a.getModel(),a.getPredictionId(),a.getErrorMessage(),propReferenceImage(a.getReferenceImages()),"prop".equals(a.getKind()) && undoableProps.contains(a.getAssetKey()))).toList(),
            "completed",rows.stream().filter(a -> "done".equals(a.getStatus())).count(),"total",rows.size());
    }
    /**
     * A manual start frame is a storyboard-level override.  It deliberately does not
     * replace the generated shot-frame record, so a reviewed candidate remains intact
     * and can be used again after the override is removed.
     */
    public Map<String,Object> saveManualStartFrame(Long projectId, Long storyboardId, String imageUrl, Long user) {
        if (imageUrl == null || !imageUrl.startsWith("https://"))
            throw new IllegalArgumentException("请上传有效的起始帧图片");
        ShortDramaStoryboard shot = editableStartFrame(projectId, storyboardId, user);
        ObjectNode continuity = editableContinuity(shot);
        ObjectNode frame = continuity.putObject("manual_start_frame");
        frame.put("url", imageUrl);
        frame.put("source", "upload");
        saveContinuity(shot, continuity);
        return status(projectId, user);
    }
    public Map<String,Object> removeManualStartFrame(Long projectId, Long storyboardId, Long user) {
        ShortDramaStoryboard shot = editableStartFrame(projectId, storyboardId, user);
        ObjectNode continuity = editableContinuity(shot);
        continuity.remove("manual_start_frame");
        saveContinuity(shot, continuity);
        return status(projectId, user);
    }
    private ShortDramaStoryboard editableStartFrame(Long projectId, Long storyboardId, Long user) {
        owner(projectId, user);
        ShortDramaStoryboard shot = boards.selectById(storyboardId);
        if (shot == null || !projectId.equals(shot.getProjectId())) throw new IllegalArgumentException("镜头不属于当前项目");
        if (Set.of("generating", "submission_unknown").contains(Objects.toString(shot.getVideoStatus(), "")))
            throw new IllegalStateException("请先查询或停止该镜头的视频任务");
        return shot;
    }
    private ObjectNode editableContinuity(ShortDramaStoryboard shot) {
        try {
            JsonNode node = JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"));
            return node instanceof ObjectNode object ? object.deepCopy() : JSON.createObjectNode();
        } catch (Exception e) {
            throw new IllegalStateException("镜头连续性数据无效，请修复后重试", e);
        }
    }
    private void saveContinuity(ShortDramaStoryboard shot, ObjectNode continuity) {
        boards.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, shot.getId())
            .set(ShortDramaStoryboard::getContinuityJson, continuity.toString()));
    }
    public Map<String,Object> plan(Long id, String model, Long user) {
        requireCurrentScript(id,user);
        if (running.putIfAbsent(id,"正在分析关键道具与镜头覆盖") != null) throw new IllegalStateException("资产任务正在运行");
        errors.remove(id);
        String tenant = TenantHelper.getTenantId();
        coordinators.submit(() -> TenantHelper.dynamic(tenant, () -> {
            try { planNow(id,model); }
            catch(Exception e) { errors.put(id,safeError(e)); log.warn("视觉资产规划失败 project={}: {}",id,safeError(e)); }
            finally { running.remove(id); }
        }));
        return status(id,user);
    }
    private void planNow(Long id, String model) throws Exception {
        var shots=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id).orderByAsc(ShortDramaStoryboard::getSceneNo));
        if(shots.isEmpty()) throw new IllegalStateException("请先确认分镜，再规划逐镜资产");
        var script=scripts.selectById(shots.get(0).getScriptId());
        var project = projects.selectById(id);
        String selectedDirection = skillCatalog.selected(project, "aesthetic") + skillCatalog.selected(project, "director");
        String selectedMediaDirection = skillCatalog.selectedVisual(project, "aesthetic") + skillCatalog.selectedVisual(project, "director");
        var m=models.selectModelByName(model);
        if(m==null) { var q=new ChatModelBo();q.setCategory("chat");m=models.queryAvailableList(q).stream().findFirst().orElseThrow(); }
        var chat=chats.getOriginalService(ShortDramaServiceImpl.shortDramaProviderCode(m.getProviderCode(),m.getModelName())).buildChatModel(m);
        // Do not invent extra characters or costumes merely to increase image counts.
        String prompt=PROP_EXTRACTION_PROMPT+script.getScriptText()+"\n镜头列表：\n"+
            shots.stream().map(s->s.getSceneNo()+":"+s.getSourceText()+"\n本镜画面设计："+Objects.toString(s.getImagePrompt(),""))
                .reduce("",(a,b)->a+b+"\n")
            +"\n正文是简述，道具覆盖必须同时参考各镜画面设计。区分画面中可见、包内收起、禁止出现等语境；不能因通用提示里出现一个物体名就绑定所有镜头。";
        prompt = selectedDirection + prompt;
        String raw=chat.chat(prompt); int start=raw.indexOf('['),end=raw.lastIndexOf(']');
        if(start<0 || end<start) throw new IllegalStateException("道具分析未返回有效列表，原资产未变更");
        var props=JSON.readTree(raw.substring(start,end+1));
        if(!props.isArray() || props.size()>12) throw new IllegalStateException("道具清单格式不正确");
        Set<String> keys=new HashSet<>();
        Set<Integer> numbers=new HashSet<>();shots.forEach(s->numbers.add(s.getSceneNo()));
        for(var prop:props) {
            String name=prop.path("name").asText().trim(), description=prop.path("description").asText().trim();
            if(name.isBlank() || description.isBlank() || !prop.path("shot_numbers").isArray()) throw new IllegalStateException("道具缺少外观或镜头绑定");
            List<Integer> linked=new ArrayList<>();for(var n:prop.path("shot_numbers")) if(numbers.contains(n.asInt())) linked.add(n.asInt());
            if(linked.isEmpty()) continue;
            String key="prop:"+name; if(!keys.add(key)) continue;
            String desc=ShortDramaAssetAesthetics.propReference(skillCatalog.effectiveArtStyle(project), script.getWorldbuilding()) + selectedMediaDirection
                + "电影道具形制参照图。单个物体，简洁中性背景，清楚表现正面和关键结构，不出现人物、拼贴分格、水印或额外道具。"+description;
            // reference_images holds typed linkage for props until image generation; no external URL is inferred.
            upsert(id,null,key,"prop",name,desc,JsonUtils.toJsonString(linked),DigestUtil.sha256Hex(desc+linked));
        }
        String propsVersion=props.toString();
        for(var shot:shots) {
            if (directInsert(shot.getContinuityJson())) continue;
            var refs=baseReferences(shot);
            if(refs.isEmpty()) throw new IllegalStateException("镜头"+shot.getSceneNo()+"缺少已确认的人物/场景参考图");
            String key="shot:"+shot.getId();keys.add(key);
            String framePrompt=productionFramePrompt(shot);
            upsert(id,shot.getId(),key,"shot_frame","镜 "+shot.getSceneNo()+" · "+shot.getSceneTitle(),framePrompt,
                JsonUtils.toJsonString(Map.of("urls",refs,"propsVersion",propsVersion)),productionFrameHash(shot,refs,propsVersion));
        }
        for(var old:rows(id)) if(!keys.contains(old.getAssetKey()) && !Objects.toString(old.getSourceHash(),"").startsWith("locked:")) assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getId,old.getId()).set(ShortDramaVisualAsset::getStatus,"obsolete"));
    }
    static String framePrompt(ShortDramaStoryboard shot) {
        String start="", anchor="", present="", directive="", extras="";
        int namedCount = -1;
        try { extras = ShortDramaBackgroundExtras.readText(JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"))); }
        catch (java.io.IOException ignored) { } // Preserve legacy unstructured continuity, but reject a malformed extras field.
        try { var continuity=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}"));
            directive=continuity.path("keyframe_directive").asText("");
            start=continuity.path("start_state").asText(""); anchor=continuity.path("spatial_anchor").asText("");
            present=continuity.path("present_characters").toString();
            var cast=continuity.path("present_characters");
            namedCount = cast.isArray() ? cast.size() : -1;
            if(cast.isArray() && cast.size()==1) {
                // A scene-wide anchor can mention someone who has not entered this shot yet.
                // For a solo shot use its structured binding, not the shared cast description.
                String name=cast.get(0).asText(), slot="";
                var bindings=JSON.readTree(Objects.toString(shot.getCharactersJson(),"[]"));
                for(var binding:bindings) if(name.equals(binding.path("name").asText())) slot=binding.path("slot").asText("");
                anchor=ShortDramaBackgroundExtras.parse(extras).visible()
                    ? "本镜唯一具名/登记主体："+name+"，位置："+slot+"。匿名群演另按0秒background_extras人数和位置落实，不添加其他具名演员、CG或尚未入场者。"
                    : "本镜画面严格只有1人："+name+"，位置："+slot+"。背景、门口、玻璃反射均无其他人物，不提前画入尚未入场的配角。视线和动作遵照起始状态。";
            }
        } catch(Exception ignored) { }
        boolean visibleExtras = ShortDramaBackgroundExtras.parse(extras).visible();
        String population = namedCount == 0 && !visibleExtras ? "本镜0秒画面无人，无匿名群演，不画人影、倒影或反射脸。\n"
            : "首帧人数以0秒登记角色名单和匿名群演可见人数共同确定；不得按具名名册人数删掉原文群众，也不得提前画入后续入场的人物。\n";
        return ShortDramaDirectorSkills.media("visual-world", "character-art-direction", "director-blocking", "director-review") + ShortDramaAssetAesthetics.shotFrame() + "电影单镜首帧，单幅完整构图，不生成三视图、分镜格或字幕。参考图中的多视角人物只作为同一身份，不复制成多人。严格按本镜0秒具名演员与匿名群演状态、持物手、坐立和门灯状态构图。只画起始状态，不提前执行结束动作。只有剧本明确要求打破第四面墙才直视镜头；其他人物视线必须落到本镜对象或明确画外方向。建筑、服装和器物的维护程度以当前资产描述为准，不因贫困或年代自动加破败污损。清晰账页、软件屏幕留给后期替换。\n"
            +"本镜景别与角度："+Objects.toString(shot.getShotType(),"")+"。构图以本镜要求为准，参考人物和场景图只锁定身份、外观和建筑，不继承它们的景别、正面机位或人物站位。\n"
            +"参考场景图只锁建筑几何、身份与已绑定外观，不锁时间、天气、色温、光照或曝光。本镜时段、摄影光色和材质以当前镜头静态指导为准，当前镜头时段不得被场景参考图覆盖。\n"
            +"0秒具名/登记出镜角色名单（匿名群演另列）："+present+"\n"+population
            +"0秒匿名群演："+ShortDramaBackgroundExtras.frameGuidance(extras)+"\n空间与视线："+anchor+"\n起始状态："+start+"\n"
            +(start.isBlank()?Objects.toString(shot.getImagePrompt(),""):"起始姿态、人数和持物为本图唯一动作依据，不拼接后续动作结果。服装与身份遵照绑定参考图。\n当前镜头静态指导：\n"+staticFrameGuidance(shot))
            + closeFrameInstruction(shot)
            +(directive.isBlank()?"":"\n本镜经人工审阅的构图修订（在起始状态内落实）："+directive);
    }
    /** Conservative static clauses only: a mixed action/lighting sentence is omitted, never treated as a pose. */
    static String staticFrameGuidance(ShortDramaStoryboard shot) {
        LinkedHashSet<String> clauses = new LinkedHashSet<>();
        addStaticClauses(clauses, Objects.toString(shot.getImagePrompt(), ""));
        try {
            var photography = JSON.readTree(Objects.toString(shot.getPhotographyRules(), "{}"));
            // Whitelist current photographic fields, rather than importing actor/blocking/end-state rules.
            for (String path : List.of("/lighting/direction", "/lighting/quality", "/depth_of_field", "/color_palette", "/color_temperature", "/exposure", "/texture", "/material")) {
                var value = photography.at(path);
                if (value.isTextual()) addStaticClauses(clauses, value.asText());
            }
        } catch (Exception ignored) { }
        return String.join("。\n", clauses);
    }
    private static void addStaticClauses(Set<String> result, String text) {
        for (String raw : text.split("[。；;\\n\\r]+")) {
            String clause = raw.trim();
            if (clause.isBlank() || clause.length() > 600) continue;
            boolean staticCue = clause.matches("(?s).*(清晨|凌晨|晨光|午后|正午|白昼|黄昏|傍晚|夕阳|夜晚|月光|天光|主光|逆光|侧光|冷光|暖光|光线|光照|色温|色调|曝光|景深|焦距|机位|镜头|构图|材质|纹理|木纹|麻布|粗麻|绸|金属|石材|photograph|lighting|texture|material|morning|sunset).*");
            // Deliberately over-reject mixed prose. No generic image text may override structured start_state.
            boolean dynamic = clause.matches("(?is).*(坐|站|躺|卧|起身|起立|抬|低头|回头|转身|转头|伸手|递|接过|接纸|接住|放下|放开|拿|握|持|走|跑|迈|推|拉|开门|关门|进入|离开|说|喊|张嘴|点头|摇头|看向|望向|目光|表情|神情|姿态|动作|人物|人影|身旁|两人|角色|演员|扶|捧|抓|挥|抱|蹲|跪|挪|举|醒|睁眼|浮现|显现|出场|已经|随后|最终|结束|完成|sit|stand|lie|lying|raise|reach|hand over|turn|walk|run|speaks|holds|grabs).*");
            if (staticCue && !dynamic) result.add(clause);
        }
    }
    static String closeFrameInstruction(ShortDramaStoryboard shot) {
        String type=Objects.toString(shot.getShotType(),"");
        if((!type.contains("近景") && !type.contains("特写")) || type.contains("中近景") || type.contains("双人")) return "";
        String focus="本镜叙事主体";
        try {
            var bindings=JSON.readTree(Objects.toString(shot.getCharactersJson(),"[]"));
            String title=Objects.toString(shot.getSceneTitle(),"");
            for(var binding:bindings) if(!binding.path("name").asText().isBlank() && title.startsWith(binding.path("name").asText())) {
                focus=binding.path("name").asText();break;
            }
            String reviewed=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("frame_focus").asText("");
            if(!reviewed.isBlank()) focus=reviewed;
        } catch(Exception ignored) { }
        String extrasRule = "";
        try { if (ShortDramaBackgroundExtras.parse(ShortDramaBackgroundExtras.readText(JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}")))).visible())
            extrasRule = "具名名单只限定登记身份，不限制已明确的0秒匿名群演人数；按其位置安排景深与遮挡，不因只有一名具名演员删掉群演。"; }
        catch (java.io.IOException ignored) { }
        return "\n景别落实："+type+"以"+focus+"为视觉中心。人物近景只保留头肩或上半身，人物特写聚焦脸部或动作细节；道具特写只框叙事物体。不得退成展示整个房间和整张桌子的双人全景。"+extrasRule+"名单限定允许出现的登记身份，不要求全部同时入画；同场倾听者可以在画外或只有肩部边缘，禁止为了参考图齐全而扩宽构图。";
    }
    static boolean directInsert(String continuity) {
        try { return "direct_insert".equals(JSON.readTree(Objects.toString(continuity,"{}")).path("source_media").path("mode").asText()); }
        catch(Exception e) { return false; }
    }
    /** Reuse the approved prop library when only an existing shot is revised. */
    public void synchronizeExistingFrames(Long id) { synchronizeExistingFrames(id, null); }
    public void synchronizeExistingFrames(Long id, Set<Long> selected) {
        var current=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id));
        for(var shot:current) {
            if(selected != null && !selected.contains(shot.getId())) continue;
            var old=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,id).eq(ShortDramaVisualAsset::getStoryboardId,shot.getId()).last("limit 1"));
            if(old==null) continue;
            if(directInsert(shot.getContinuityJson())) {
                old.setStatus("obsolete");assets.updateById(old);continue;
            }
            try {
                String propsVersion=JSON.readTree(old.getReferenceImages()).path("propsVersion").asText();
                var refs=baseReferences(shot);
                upsert(id,shot.getId(),old.getAssetKey(),"shot_frame","镜 "+shot.getSceneNo()+" · "+shot.getSceneTitle(),productionFramePrompt(shot),JsonUtils.toJsonString(Map.of("urls",refs,"propsVersion",propsVersion)),productionFrameHash(shot,refs,propsVersion));
            } catch(Exception e) { throw new IllegalStateException("镜头"+shot.getSceneNo()+"参考图需要更新",e); }
        }
    }
    static String frameHash(ShortDramaStoryboard shot,List<String> refs,String props) {
        return DigestUtil.sha256Hex(framePrompt(shot)+shot.getPhotographyRules()+shot.getCharactersJson()+shot.getContinuityJson()+refs+props);
    }
    private String selectedDirection(Long projectId) {
        var project = projects.selectById(projectId);
        return skillCatalog.selectedVisual(project, "aesthetic") + skillCatalog.selectedVisual(project, "director");
    }
    private String productionFramePrompt(ShortDramaStoryboard shot) { return framePrompt(shot) + selectedDirection(shot.getProjectId()); }
    private String productionFrameHash(ShortDramaStoryboard shot, List<String> refs, String props) {
        String direction = selectedDirection(shot.getProjectId());
        return direction.isBlank() ? frameHash(shot, refs, props) : DigestUtil.sha256Hex(frameHash(shot, refs, props) + direction);
    }

    static boolean onlyVideoResolutionChanged(ShortDramaStoryboard before, ShortDramaStoryboard after) {
        if (before == null || after == null || Objects.equals(before.getContinuityJson(), after.getContinuityJson())) return false;
        try {
            var old = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(before.getContinuityJson());
            var next = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(after.getContinuityJson());
            old.remove("video_resolution"); next.remove("video_resolution");
            old.remove("video_seconds"); next.remove("video_seconds");
            old.remove("video_use_start_frame"); next.remove("video_use_start_frame");
            old.remove("video_reference_include_location"); next.remove("video_reference_include_location");
            return old.equals(next) && Objects.equals(before.getImagePrompt(), after.getImagePrompt())
                && Objects.equals(before.getPhotographyRules(), after.getPhotographyRules())
                && Objects.equals(before.getCharactersJson(), after.getCharactersJson())
                && Objects.equals(before.getLocationName(), after.getLocationName());
        } catch (Exception ignored) { return false; }
    }

    /** Video output settings do not change an already reviewed image composition. */
    public void preserveFrameForVideoSettings(ShortDramaStoryboard before, ShortDramaStoryboard after) {
        if (!onlyVideoResolutionChanged(before, after)) return;
        var asset = assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getStoryboardId, before.getId())
            .eq(ShortDramaVisualAsset::getStatus, "done").last("limit 1"));
        if (asset == null) return;
        try {
            if (!isCurrentFrame(asset, before)) return;
            String propsVersion = JSON.readTree(asset.getReferenceImages()).path("propsVersion").asText();
            assets.update(null, new LambdaUpdateWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getId, asset.getId())
                .eq(ShortDramaVisualAsset::getSourceHash, asset.getSourceHash())
                .set(ShortDramaVisualAsset::getSourceHash, productionFrameHash(after, baseReferences(after), propsVersion)));
        } catch (Exception e) { throw new IllegalStateException("保存视频设置时无法核对已审阅首帧", e); }
    }
    private void upsert(Long project,Long shot,String key,String kind,String title,String prompt,String refs,String hash) {
        var old=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,project).eq(ShortDramaVisualAsset::getAssetKey,key));
        if(old!=null && Objects.toString(old.getSourceHash(),"").startsWith("locked:") && !hash.startsWith("locked:"))return;
        if(old!=null && hash.equals(old.getSourceHash()) && !"obsolete".equals(old.getStatus())) return;
        archiveCurrentAsset(old, key, kind);
        if(old==null) {old=new ShortDramaVisualAsset();old.setId(IdUtil.getSnowflakeNextId());old.setProjectId(project);old.setAssetKey(key);}
        boolean insert=old.getKind()==null;
        old.setStoryboardId(shot);old.setKind(kind);old.setTitle(title);old.setPrompt(prompt);old.setReferenceImages(refs);old.setSourceHash(hash);old.setStatus("pending");
        old.setImageUrl("");old.setPredictionId("");old.setErrorMessage("");old.setUpdateTime(new Date());
        if(insert) {old.setCreateTime(new Date());assets.insert(old);} else assets.updateById(old);
    }
    /** Keep the accepted candidate recoverable before planning or regenerating a replacement. */
    private void archiveCurrentAsset(ShortDramaVisualAsset current, String key, String kind) {
        if(current==null || (Objects.toString(current.getImageUrl(), "").isBlank() && Objects.toString(current.getPredictionId(), "").isBlank())) return;
        if (Set.of("generating", "waiting").contains(current.getStatus()))
            throw new IllegalStateException("原素材任务仍已受理，请先查询原任务；旧任务不会被新规划覆盖");
        var previous = new ShortDramaVisualAsset(); org.springframework.beans.BeanUtils.copyProperties(current, previous);
        previous.setId(IdUtil.getSnowflakeNextId()); previous.setAssetKey(key + "@history-" + previous.getId());
        previous.setKind("archived_" + kind); previous.setStatus("archived"); previous.setUpdateTime(new Date());
        assets.insert(previous);
    }
    static boolean voiceOnly(ShortDramaCharacter ch) {
        String introduction=Objects.toString(ch.getIntroduction(),"");
        return introduction.contains("全剧仅以手机语音出现") || introduction.contains("全剧仅画外音") || introduction.contains("无实体出场");
    }
    static List<JsonNode> firstFrameBindings(ShortDramaStoryboard shot) throws java.io.IOException {
        Set<String> visible = null; // Legacy rows without an explicit zero-second roster keep their existing behavior.
        try {
            var continuity = JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"));
            var present = continuity.path("present_characters");
            if (present.isArray()) { visible = new HashSet<>(); for (var name : present) visible.add(name.asText()); }
        } catch (java.io.IOException ignored) { } // Old unstructured continuity is compatible too.
        var bindings=JSON.readTree(shot.getCharactersJson()==null?"[]":shot.getCharactersJson());
        List<JsonNode> selected = new ArrayList<>();
        if (!bindings.isArray()) throw new IllegalArgumentException("角色绑定须为数组");
        for(var binding:bindings) {
            if (visible != null && !visible.contains(binding.path("name").asText())) continue;
            selected.add(binding);
        }
        return selected;
    }
    record FrameReferences(List<String> urls, String labels) { }
    private List<String> baseReferences(ShortDramaStoryboard shot) throws Exception { return frameReferences(shot).urls(); }
    private FrameReferences frameReferences(ShortDramaStoryboard shot) throws Exception {
        List<String> refs=new ArrayList<>(); StringBuilder labels = new StringBuilder();
        // URLs and identity labels are built from the same zero-second selection and actual URL
        // indices. Neither later entrants nor deduplicated reference URLs can shift a label.
        for(var binding:firstFrameBindings(shot)) {
            var ch=characters.selectOne(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId,shot.getProjectId()).eq(ShortDramaCharacter::getName,binding.path("name").asText()).last("limit 1"));
            if(ch==null) throw new IllegalStateException("人物绑定不存在："+binding.path("name").asText());
            if(voiceOnly(ch)) continue;
            var variants=appearances.selectList(new LambdaQueryWrapper<ShortDramaCharacterAppearance>().eq(ShortDramaCharacterAppearance::getCharacterId,ch.getId()).orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            var selected=ShortDramaAppearanceResolver.resolve(variants,binding.path("appearance").asText());
            String url=selected==null?ch.getReferenceImageUrl():selected.getReferenceImageUrl();
            if(url==null || url.isBlank()) throw new IllegalStateException(ch.getName()+"缺少已确认形象图");
            if(!refs.contains(url))refs.add(url);
            labels.append("\n参考图").append(refs.indexOf(url)+1).append("绑定人物：").append(binding.path("name").asText()).append("。");
        }
        var loc=locations.selectOne(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId,shot.getProjectId()).eq(ShortDramaLocation::getName,shot.getLocationName()).last("limit 1"));
        if(loc==null || loc.getReferenceImageUrl()==null || loc.getReferenceImageUrl().isBlank()) throw new IllegalStateException("场景缺少参考图："+shot.getLocationName());
        if(!refs.contains(loc.getReferenceImageUrl()))refs.add(loc.getReferenceImageUrl());
        labels.append("\n参考图").append(refs.indexOf(loc.getReferenceImageUrl())+1).append("绑定场景建筑，移动物体按本镜起始状态摆放。");
        return new FrameReferences(refs, labels.toString());
    }
    /** Keep numeric prop bindings attached to the same shots when the user edits the list. */
    public void reindexStoryboardProps(Long projectId, Map<Integer,Integer> numbers, Long deletedId) {
        if (running.containsKey(projectId)) throw new IllegalStateException("资产正在处理，请完成后再增删分镜");
        for (var asset : rows(projectId)) {
            if (deletedId != null && deletedId.equals(asset.getStoryboardId())) {
                if (java.util.Set.of("pending", "processing", "generating").contains(Objects.toString(asset.getStatus(), ""))
                    && asset.getPredictionId() != null && !asset.getPredictionId().isBlank()) throw new IllegalStateException("此镜头图片仍在生成，请完成后再删除");
                assets.deleteById(asset.getId());
            } else if ("prop".equals(asset.getKind())) {
                var linked = propShotNumbers(asset.getReferenceImages()).stream()
                    .map(n -> numbers.getOrDefault(n, n)).filter(n -> n > 0).distinct().sorted().toList();
                try {
                    var original = JSON.readTree(Objects.toString(asset.getReferenceImages(), "[]"));
                    JsonNode updated;
                    if (original.isObject()) {
                        ((ObjectNode) original).set("shotNumbers", JSON.valueToTree(linked)); updated = original;
                    } else { updated = JSON.valueToTree(linked); }
                    assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getId,asset.getId())
                        .set(ShortDramaVisualAsset::getReferenceImages,updated.toString()));
                } catch (java.io.IOException e) { throw new IllegalStateException("道具镜头关联无法读取", e); }
            }
        }
    }

    public Map<String,Object> saveProp(Long id,String title,String prompt,List<Integer> shotNumbers,String imageUrl,Long user) {
        owner(id,user);
        if(title==null || title.isBlank() || title.length()>80 || prompt==null || prompt.isBlank() || prompt.length()>12000)
            throw new IllegalArgumentException("请填写道具名和外观描述");
        var shots=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id));
        Set<Integer> valid=new HashSet<>();shots.forEach(s->valid.add(s.getSceneNo()));
        List<Integer> requested = shotNumbers == null ? new ArrayList<>() : new ArrayList<>(shotNumbers);
        // Assets precede storyboards. An omitted binding also preserves existing links on a same-name edit.
        var existing=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,id).eq(ShortDramaVisualAsset::getAssetKey,"prop:"+title.trim()));
        if(requested.isEmpty() && existing!=null) requested.addAll(propShotNumbers(existing.getReferenceImages()));
        if(requested.stream().anyMatch(n -> n == null || n < 1) || !valid.containsAll(requested))throw new IllegalArgumentException("请选择实际存在的镜号，尚未分镜时可留空");
        if(imageUrl!=null && !imageUrl.isBlank() && !imageUrl.startsWith("https://"))throw new IllegalArgumentException("参考图必须是已上传的HTTPS地址");
        if(running.putIfAbsent(id,"保存固定道具")!=null)throw new IllegalStateException("请等待资产生成完成");
        try {
            var linked=requested.stream().distinct().sorted().toList();
            String referenceImageUrl = existing == null ? "" : propReferenceImage(existing.getReferenceImages());
            upsert(id,null,"prop:"+title.trim(),"prop",title.trim(),prompt,propMetadataJson(linked,referenceImageUrl),"locked:"+DigestUtil.sha256Hex(prompt+linked+referenceImageUrl+Objects.toString(imageUrl,"")).substring(0,57));
            if(imageUrl!=null && !imageUrl.isBlank()) {
                if(!imageUrl.startsWith("https://"))throw new IllegalArgumentException("参考图必须是已上传的HTTPS地址");
                assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,id).eq(ShortDramaVisualAsset::getAssetKey,"prop:"+title.trim())
                    .set(ShortDramaVisualAsset::getImageUrl,imageUrl).set(ShortDramaVisualAsset::getStatus,"done"));
            }
            for(var shot:shots)if(linked.contains(shot.getSceneNo())) assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>()
                .eq(ShortDramaVisualAsset::getStoryboardId,shot.getId()).set(ShortDramaVisualAsset::getStatus,"pending")
                .set(ShortDramaVisualAsset::getPredictionId,"").set(ShortDramaVisualAsset::getErrorMessage,"固定道具已更新，需要重生关键帧"));
            return status(id,user);
        } finally {running.remove(id);}
    }
    /** Edit a generated prop without replacing its accepted image or invalidating frames yet. */
    public Map<String,Object> updateProp(Long projectId, Long assetId, String prompt, String referenceImageUrl, Long user) {
        owner(projectId, user);
        var asset = assets.selectById(assetId);
        if(asset==null || !projectId.equals(asset.getProjectId()) || !"prop".equals(asset.getKind())) throw new IllegalArgumentException("只能维护当前项目的道具");
        if(prompt==null || prompt.isBlank() || prompt.length()>12000) throw new IllegalArgumentException("请填写有效的道具提示词（最多12000字）");
        if(Set.of("generating", "waiting").contains(asset.getStatus())) throw new IllegalStateException("道具任务正在进行，请先等待结果");
        String reference = validReferenceImageUrl(referenceImageUrl);
        var linked = propShotNumbers(asset.getReferenceImages());
        asset.setPrompt(prompt.trim());
        asset.setReferenceImages(propMetadataJson(linked, reference));
        asset.setSourceHash("locked:" + DigestUtil.sha256Hex(asset.getTitle() + asset.getPrompt() + linked + reference).substring(0,57));
        asset.setErrorMessage(""); asset.setUpdateTime(new Date()); assets.updateById(asset);
        return status(projectId, user);
    }
    /** Regenerate only one prop. The previous image remains visible until a new candidate succeeds. */
    public Map<String,Object> regenerateProp(Long projectId, Long assetId, String model, Long user) {
        owner(projectId, user);
        var asset = assets.selectById(assetId);
        if(asset==null || !projectId.equals(asset.getProjectId()) || !"prop".equals(asset.getKind())) throw new IllegalArgumentException("只能重新生成当前项目的道具");
        if(asset.getPrompt()==null || asset.getPrompt().isBlank()) throw new IllegalArgumentException("请先填写道具提示词");
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory())) throw new IllegalArgumentException("请选择已配置的生图模型");
        if(!propReferenceImage(asset.getReferenceImages()).isBlank() && "bytedance/seedream-v4.7/text-to-image".equals(model))
            throw new IllegalArgumentException("当前 Seedream 4.7 文生图不支持参考图；请在创意设定中改用可编辑的图片模型后重试");
        if(running.putIfAbsent(projectId,"重新生成道具："+asset.getTitle())!=null) throw new IllegalStateException("资产任务正在运行");
        try {
            archiveCurrentAsset(asset, asset.getAssetKey(), "prop");
            // Keep imageUrl in the current row until the replacement succeeds, so a failed retry never hides the approved candidate.
            asset.setStatus("pending"); asset.setPredictionId(""); asset.setErrorMessage(""); asset.setUpdateTime(new Date()); assets.updateById(asset);
        } catch(Exception e) { running.remove(projectId); throw e; }
        String tenant=TenantHelper.getTenantId(); errors.remove(projectId);
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try { CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(asset,model)),imageWorkers).join(); }
            catch(Exception e) { errors.put(projectId,safeError(e)); }
            finally { running.remove(projectId); }
        }));
        return status(projectId,user);
    }
    /** Swap the current prop with its latest archived candidate without submitting a new image task. */
    public Map<String,Object> undoProp(Long projectId, Long assetId, Long user) {
        owner(projectId, user);
        if(running.containsKey(projectId)) throw new IllegalStateException("资产任务正在运行");
        var current = assets.selectById(assetId);
        if(current==null || !projectId.equals(current.getProjectId()) || !"prop".equals(current.getKind()))
            throw new IllegalArgumentException("只能撤销当前项目的道具");
        if(Set.of("generating", "waiting").contains(current.getStatus())) throw new IllegalStateException("道具任务正在进行，请先等待结果");
        var previous = assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>()
            .eq(ShortDramaVisualAsset::getProjectId, projectId)
            .eq(ShortDramaVisualAsset::getKind, "archived_prop")
            .likeRight(ShortDramaVisualAsset::getAssetKey, current.getAssetKey() + "@history-")
            .orderByDesc(ShortDramaVisualAsset::getId).last("limit 1"));
        if(previous==null) throw new IllegalStateException("没有可恢复的上一版道具");

        var currentVersion = new ShortDramaVisualAsset();
        org.springframework.beans.BeanUtils.copyProperties(current, currentVersion);
        copyPropVersion(previous, current);
        current.setStatus(Objects.toString(current.getImageUrl(), "").isBlank() ? "pending" : "done");
        current.setErrorMessage(""); current.setUpdateTime(new Date());
        assets.updateById(current);

        copyPropVersion(currentVersion, previous);
        previous.setKind("archived_prop"); previous.setStatus("archived"); previous.setUpdateTime(new Date());
        assets.updateById(previous);
        invalidateFramesForProp(current);
        return status(projectId, user);
    }
    private static void copyPropVersion(ShortDramaVisualAsset source, ShortDramaVisualAsset target) {
        target.setStoryboardId(source.getStoryboardId());
        target.setTitle(source.getTitle());
        target.setPrompt(source.getPrompt());
        target.setReferenceImages(source.getReferenceImages());
        target.setSourceHash(source.getSourceHash());
        target.setImageUrl(source.getImageUrl());
        target.setModel(source.getModel());
        target.setPredictionId(source.getPredictionId());
        target.setErrorMessage(source.getErrorMessage());
    }
    static boolean needsPropImage(ShortDramaVisualAsset asset) {
        return "prop".equals(asset.getKind()) && Objects.toString(asset.getImageUrl(), "").isBlank()
            && Objects.toString(asset.getPredictionId(), "").isBlank()
            && Set.of("pending", "failed").contains(Objects.toString(asset.getStatus(), ""));
    }

    /** Generate missing project props only; never generate storyboard frames from the asset button. */
    public Map<String,Object> generateMissingProps(Long id, String model, Long user) {
        owner(id,user);
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory())) throw new IllegalArgumentException("后台尚未配置可用图片模型");
        if(running.putIfAbsent(id,"生成缺图道具")!=null) throw new IllegalStateException("资产任务正在运行，请等待完成");
        errors.remove(id);
        String tenant=TenantHelper.getTenantId();
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try {
                var tasks = rows(id).stream().filter(a -> needsPropImage(a))
                    .map(a -> CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(a,model)),imageWorkers)).toList();
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            }
            catch(Exception e) { errors.put(id,safeError(e)); }
            finally { running.remove(id); }
        }));
        return status(id,user);
    }

    public Map<String,Object> generateOpening(Long id,String model,Long user) { return generateRange(id,model,user,1,10); }
    public Map<String,Object> generateRange(Long id,String model,Long user,int start,int end) {
        if(start<1 || end<start || end-start>=40)throw new IllegalArgumentException("每批请选择连续1至40镜");
        requireCurrentScript(id,user);
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory()))throw new IllegalArgumentException("请选择生图模型");
        var selectedShots=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id).ge(ShortDramaStoryboard::getSceneNo,start).le(ShortDramaStoryboard::getSceneNo,end));
        var selected=selectedShots.stream().map(ShortDramaStoryboard::getId).collect(java.util.stream.Collectors.toSet());
        if(selected.isEmpty())throw new IllegalArgumentException("范围内没有镜头");
        if(running.putIfAbsent(id,"生成第"+start+"至"+end+"镜资产")!=null)throw new IllegalStateException("资产任务正在运行");
        String tenant=TenantHelper.getTenantId();errors.remove(id);
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try {
                // Newly inserted shots need a frame entry, without replanning approved shots.
                for (var shot : selectedShots) {
                    var existingFrame = rows(id).stream().filter(a -> "shot_frame".equals(a.getKind()) && Objects.equals(a.getStoryboardId(), shot.getId())).findFirst().orElse(null);
                    if (existingFrame == null || !isCurrentFrame(existingFrame, shot)) {
                        var refs = baseReferences(shot);
                        String propsVersion = "existing-props";
                        upsert(id, shot.getId(), "shot:" + shot.getId(), "shot_frame", "镜 " + shot.getSceneNo() + " · " + shot.getSceneTitle(),
                            productionFramePrompt(shot), JsonUtils.toJsonString(Map.of("urls", refs, "propsVersion", propsVersion)), productionFrameHash(shot, refs, propsVersion));
                    }
                }
                runGroup(id,model,"prop");
                List<CompletableFuture<Void>> tasks=new ArrayList<>();
                for(var a:rows(id))if("shot_frame".equals(a.getKind()) && selected.contains(a.getStoryboardId()) && !"done".equals(a.getStatus()))
                    tasks.add(CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(a,model)),imageWorkers));
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            } catch(Exception e){errors.put(id,safeError(e));}finally{running.remove(id);}
        }));
        return status(id,user);
    }
    public List<String> readyPropReferences(ShortDramaStoryboard shot) {
        return readyPropBindings(shot).stream().map(ShortDramaVideoPropReferences.Binding::imageUrl).toList();
    }
    public String readyPropDirection(ShortDramaStoryboard shot, List<String> images) {
        return ShortDramaVideoPropReferences.direction(readyPropBindings(shot), images);
    }
    private List<ShortDramaVideoPropReferences.Binding> readyPropBindings(ShortDramaStoryboard shot) {
        List<ShortDramaVideoPropReferences.Binding> result=new ArrayList<>();
        try {
            var visible=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("visible_props");
            for(var a:rows(shot.getProjectId()))if("prop".equals(a.getKind()) && "done".equals(a.getStatus())
                && (visible.isArray()?hasProp(visible,a.getTitle()):linked(a,shot.getSceneNo()))) result.add(new ShortDramaVideoPropReferences.Binding(a.getTitle(),a.getImageUrl()));
        }catch(Exception e){throw new IllegalStateException("道具参考绑定无效",e);}
        return result;
    }
    public Map<String,Object> generate(Long id,String model,Long user) {
        owner(id,user);
        // Standalone props can be produced before storyboards; frame generation still requires the current script.
        if (boards.selectCount(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id)) > 0)
            requireCurrentScript(id,user);
        synchronizeExistingFrames(id);
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory()))throw new IllegalArgumentException("请选择已配置的生图模型");
        if(rows(id).isEmpty())throw new IllegalStateException("请先分析镜头资产");
        if(running.putIfAbsent(id,"生成关键道具")!=null)throw new IllegalStateException("资产任务正在运行");
        errors.remove(id);
        String tenant = TenantHelper.getTenantId();
        coordinators.submit(()->TenantHelper.dynamic(tenant, () -> {
            try {
                runGroup(id,model,"prop");
                running.put(id,"生成逐镜关键帧（4路并发，可关闭页面后恢复查看）");
                runGroup(id,model,"shot_frame");
            } catch(Exception e) {errors.put(id,safeError(e));}
            finally {running.remove(id);}
        }));
        return status(id,user);
    }
    public Map<String,Object> reviseFrame(Long id,Long assetId,String prompt,String model,Long user) {
        requireCurrentScript(id,user);
        var asset=assets.selectById(assetId);
        if(asset==null || !id.equals(asset.getProjectId()) || !"shot_frame".equals(asset.getKind())) throw new IllegalArgumentException("只能修正本项目的镜头关键帧");
        if(prompt==null || prompt.isBlank() || prompt.length()>12000) throw new IllegalArgumentException("请填写有效的画面描述（最多12000字）");
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory()))throw new IllegalArgumentException("请选择生图模型");
        if(running.putIfAbsent(id,"修正单镜构图")!=null)throw new IllegalStateException("请等待当前资产队列完成后修正单图");
        try {
            var metadata=(com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(asset.getReferenceImages());
            var currentShot=boards.selectById(asset.getStoryboardId());
            var currentRefs=baseReferences(currentShot);
            metadata.set("urls",JSON.valueToTree(currentRefs));
            asset.setSourceHash(productionFrameHash(currentShot,currentRefs,metadata.path("propsVersion").asText()));
            metadata.put("previousImageUrl",Objects.toString(asset.getImageUrl(),""));
            metadata.put("previousPrompt",Objects.toString(asset.getPrompt(),""));
            metadata.put("previousPredictionId",Objects.toString(asset.getPredictionId(),""));
            asset.setReferenceImages(metadata.toString());asset.setPrompt(prompt);asset.setPredictionId("");
            asset.setStatus("pending");asset.setErrorMessage("");asset.setUpdateTime(new Date());assets.updateById(asset);
        } catch(Exception e) {running.remove(id);throw new IllegalStateException(safeError(e),e);}
        String tenant=TenantHelper.getTenantId();errors.remove(id);
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try {CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(asset,model)),imageWorkers).join();}
            catch(Exception e){errors.put(id,safeError(e));}
            finally {running.remove(id);}
        }));
        return status(id,user);
    }

    private void requireCurrentScript(Long id,Long user) {
        if("script_changed".equals(owner(id,user).getStatus()))throw new IllegalStateException("剧本已改变，请先重新规划分镜；旧版资产保留");
    }
    public Map<String,Object> recover(Long id,Long user) {
        owner(id,user);
        var acknowledged=rows(id).stream().filter(a->Set.of("generating","waiting").contains(a.getStatus()) && a.getPredictionId()!=null && !a.getPredictionId().isBlank()).toList();
        if(acknowledged.isEmpty())return status(id,user);
        if(running.putIfAbsent(id,"仅恢复已提交的图片任务")!=null)throw new IllegalStateException("资产任务正在运行");
        String tenant=TenantHelper.getTenantId();errors.remove(id);
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try {
                var tasks=acknowledged.stream().map(a->CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(a,a.getModel())),imageWorkers)).toList();
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            }catch(Exception e){errors.put(id,safeError(e));}finally{running.remove(id);}
        }));
        return status(id,user);
    }

    private void runGroup(Long project,String model,String kind) {
        List<CompletableFuture<Void>> tasks=new ArrayList<>();
        String tenant=TenantHelper.getTenantId();
        for(var a:rows(project))if(kind.equals(a.getKind()) && !"done".equals(a.getStatus()))tasks.add(CompletableFuture.runAsync(()->TenantHelper.dynamic(tenant,()->generateOne(a,model)),imageWorkers));
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
    }
    private void generateOne(ShortDramaVisualAsset a,String model) {
        boolean providerFailed = false;
        try {
            boolean resume=a.getPredictionId()!=null && !a.getPredictionId().isBlank() && !"failed".equals(a.getStatus());
            if (resume) {
                var original = models.selectModelByName(a.getModel());
                if (original == null) throw new IllegalStateException("原任务模型配置缺失，无法查询原prediction；没有提交新任务");
                awaitImage(a, original); return;
            }
            List<String> refs=new ArrayList<>();String prompt=a.getPrompt();
            if("prop".equals(a.getKind())) {
                String reference = propReferenceImage(a.getReferenceImages());
                if (!reference.isBlank()) {
                    refs.add(reference);
                    prompt += "\n参考图1为用户提供的道具外观参考：只提取材质、轮廓、工艺和磨损特征；保持本次道具名称与提示词约束，不复制图片中的人物、文字、背景或排版。";
                }
            } else if("shot_frame".equals(a.getKind())) {
                var shot=boards.selectById(a.getStoryboardId());
                if(shot==null || !isCurrentFrame(a,shot))throw new IllegalStateException("镜头或参考图已修改，请重新分析资产");
                var continuity = JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"));
                // Explicit opt-in for independent text-only candidates. Default continuity
                // generation still requires the approved identity and space references.
                boolean textOnlyCandidate = textOnlyCandidate(model, continuity);
                if (textOnlyCandidate) {
                    prompt += "\n【首帧生成模式】本镜明确选择纯文字重画候选，不传人物、场景或道具参考图片；不能宣称锁定既有脸和服装，生成后须重新审阅。按本镜文字中的人物与空间设计绘制。";
                } else {
                    var frameRefs = frameReferences(shot);
                    refs.addAll(frameRefs.urls()); prompt += frameRefs.labels();
                    String previousFrame = previousFrameReference(a.getReferenceImages());
                    if (!previousFrame.isBlank() && !refs.contains(previousFrame)) {
                        refs.add(previousFrame);
                        prompt += "\n参考图" + refs.size() + "为本镜上一版实际关键帧：保持已成立的人物数量、床侧构图、摄影轴线、姿态、表情、场景和光线，只执行本轮明确要求的局部修订；不得复制人物或把设定板排版带入画面。";
                    }
                }
                var compositionRefs=continuity.path("composition_reference_urls");
                if(compositionRefs.isArray()) {
                    if(textOnlyCandidate && !compositionRefs.isEmpty())throw new IllegalArgumentException("纯文字候选不能同时设置构图参考图片");
                    if(compositionRefs.size()>2)throw new IllegalArgumentException("每镜最多2张连续构图参考");
                    for(var ref:compositionRefs) {
                        String url=ref.asText();if(!url.startsWith("https://"))throw new IllegalArgumentException("构图参考必须为已上传图片");
                        if(!refs.contains(url)){refs.add(url);prompt+="\n参考图"+refs.size()+"为相邻已审阅镜头：锁定摄影轴线、电脑桌与背景方位、人物身份服装。只按当前起始状态改变手势和道具位置，不复制参考镜的动作。";}
                    }
                }
                var visibleProps=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("visible_props");
                for(var prop:rows(a.getProjectId()))if("prop".equals(prop.getKind()) && (visibleProps.isArray() ? hasProp(visibleProps,prop.getTitle()) : linked(prop,shot.getSceneNo()))) {
                    if(!"done".equals(prop.getStatus()))throw new IllegalStateException("等待道具资产："+prop.getTitle());
                    if(textOnlyCandidate) prompt += "\n本镜可见道具的文字设计："+prop.getTitle()+"。"+prop.getPrompt();
                    else { refs.add(prop.getImageUrl());prompt+="\n参考图"+refs.size()+"绑定道具："+prop.getTitle()+"。固定外观以此道具图为准，不能沿用其他参考里的旧物件。"+prop.getPrompt(); }
                }
            }
            String selected=model;
            if(!refs.isEmpty() && "bytedance/seedream-v4.7/text-to-image".equals(model))
                throw new IllegalArgumentException("所选Seedream 4.7文生图不接参考图；此首帧需另行选择编辑型号，不会自动转换或丢弃已批准身份参考");
            if(!refs.isEmpty() && model.endsWith("/text-to-image"))selected=model.replace("/text-to-image","/edit");
            if(refs.isEmpty() && model.endsWith("/edit"))selected=model.replace("/edit","/text-to-image");
            var m=models.selectModelByName(selected);if(m==null)throw new IllegalStateException("缺少对应多参考图模型配置："+selected);
            // Resume acknowledged jobs; never submit the same prediction twice.
            a.setModel(m.getModelName());a.setErrorMessage("");a.setStatus("generating");a.setUpdateTime(new Date());assets.updateById(a);
            if(!resume) {
                String currentDirection = selectedDirection(a.getProjectId());
                ensureSelectedMarkersCurrent(prompt, currentDirection);
                if (!currentDirection.isBlank() && !prompt.contains(currentDirection)) prompt += currentDirection;
                a.setPredictionId("");assets.updateById(a);
                var result=images.getOriginalService(m.getProviderCode()).startImageGeneration(ImageContext.builder().chatModelVo(m)
                    .prompt(prompt).size("shot_frame".equals(a.getKind())?projects.selectById(a.getProjectId()).getComposeAspectRatio():"1:1")
                    .referenceImages(refs).build());
                if(result==null || result.getId()==null || result.getId().isBlank())throw new IllegalStateException("模型未返回任务ID");
                a.setPredictionId(result.getId());assets.updateById(a);
            }
            awaitImage(a, m);
        } catch(InterruptedException e) {Thread.currentThread().interrupt();}
        catch(Exception e) {a.setStatus(!(e instanceof ImagePredictionFailed) && !providerFailed && a.getPredictionId()!=null && !a.getPredictionId().isBlank()?"waiting":"failed");a.setErrorMessage(safeError(e));a.setUpdateTime(new Date());assets.updateById(a);}
    }
    static boolean textOnlyCandidate(String model, JsonNode continuity) {
        return "bytedance/seedream-v4.7/text-to-image".equals(model) && continuity != null
            && "text_only_candidate".equals(continuity.path("keyframe_reference_mode").asText());
    }
    static String previousFrameReference(String metadata) {
        try {
            String url = JSON.readTree(Objects.toString(metadata, "{}")).path("previousImageUrl").asText("");
            return url.startsWith("https://") ? url : "";
        } catch (Exception ignored) {
            return "";
        }
    }
    private static final class ImagePredictionFailed extends IllegalStateException { ImagePredictionFailed() { super("模型图片任务失败"); } }
    static void ensureSelectedMarkersCurrent(String prompt, String currentDirection) {
        var matcher = java.util.regex.Pattern.compile("\\[selected-skill:[^]\\r\\n]+]").matcher(Objects.toString(prompt, ""));
        while (matcher.find()) if (!Objects.toString(currentDirection, "").contains(matcher.group()))
            throw new IllegalStateException("资产提示词属旧制作技能版本，请重新规划该资产；原图与任务ID保留，未提交新任务");
    }
    private void awaitImage(ShortDramaVisualAsset a, org.ruoyi.common.chat.domain.vo.chat.ChatModelVo m) throws InterruptedException {
            long deadline=System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(8);
            while(System.currentTimeMillis()<deadline) {
                if(Thread.currentThread().isInterrupted())return;
                var result=predictions.retrieve(m,a.getPredictionId());
                if(result!=null && ("completed".equals(result.getStatus()) || "succeeded".equals(result.getStatus())) && result.getUrl()!=null && !result.getUrl().isBlank()) {
                    a.setImageUrl(result.getUrl());a.setStatus("done");a.setUpdateTime(new Date());assets.updateById(a);
                    if ("prop".equals(a.getKind())) invalidateFramesForProp(a);
                    return;
                }
                if(result!=null && "failed".equals(result.getStatus())) throw new ImagePredictionFailed();
                Thread.sleep(2500);
            }
            a.setStatus("waiting");a.setErrorMessage("服务端任务仍未完成；再次继续会查询原任务，不重复提交");assets.updateById(a);
    }
    private void invalidateFramesForProp(ShortDramaVisualAsset prop) {
        var linked = propShotNumbers(prop.getReferenceImages());
        if (linked.isEmpty()) return;
        var shots = boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,prop.getProjectId()));
        for (var shot : shots) if (linked.contains(shot.getSceneNo())) assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>()
            .eq(ShortDramaVisualAsset::getProjectId,prop.getProjectId()).eq(ShortDramaVisualAsset::getStoryboardId,shot.getId())
            .eq(ShortDramaVisualAsset::getKind,"shot_frame")
            .set(ShortDramaVisualAsset::getStatus,"pending").set(ShortDramaVisualAsset::getPredictionId,"")
            .set(ShortDramaVisualAsset::getErrorMessage,"道具“"+prop.getTitle()+"”已更新，需要重生关键帧"));
    }
    static List<Integer> propShotNumbers(String metadata) {
        try {
            JsonNode node = JSON.readTree(Objects.toString(metadata, "[]"));
            JsonNode values = node.isObject() ? node.path("shotNumbers") : node;
            List<Integer> result = new ArrayList<>();
            if (values.isArray()) for (var value : values) if (value.isIntegralNumber() && value.intValue() > 0) result.add(value.intValue());
            return result.stream().distinct().sorted().toList();
        } catch (Exception ignored) { return List.of(); }
    }
    static String propReferenceImage(String metadata) {
        try {
            JsonNode node = JSON.readTree(Objects.toString(metadata, "{}"));
            String url = node.isObject() ? node.path("referenceImageUrl").asText("").trim() : "";
            return url.startsWith("https://") ? url : "";
        } catch (Exception ignored) { return ""; }
    }
    static String propMetadataJson(List<Integer> shotNumbers, String referenceImageUrl) {
        ObjectNode metadata = JSON.createObjectNode();
        var numbers = metadata.putArray("shotNumbers");
        for (Integer number : shotNumbers == null ? List.<Integer>of() : shotNumbers) if (number != null && number > 0) numbers.add(number);
        String reference = Objects.toString(referenceImageUrl, "").trim();
        if (!reference.isBlank()) metadata.put("referenceImageUrl", reference);
        return metadata.toString();
    }
    private static String validReferenceImageUrl(String referenceImageUrl) {
        String value = Objects.toString(referenceImageUrl, "").trim();
        if (value.isBlank()) return "";
        if (!value.startsWith("https://")) throw new IllegalArgumentException("参考图必须是已上传的 HTTPS 地址");
        return value;
    }
    private boolean linked(ShortDramaVisualAsset prop,int number) {
        return propShotNumbers(prop.getReferenceImages()).contains(number);
    }
    private boolean hasProp(JsonNode names,String title) { for(var n:names) if(title.equals(n.asText()))return true;return false; }
    public String readyFrame(Long storyboard) {
        var shot=boards.selectById(storyboard);
        String uploaded = manualStartFrame(shot);
        if (uploaded != null) return uploaded;
        var a=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getStoryboardId,storyboard)
            .eq(ShortDramaVisualAsset::getKind,"shot_frame").eq(ShortDramaVisualAsset::getStatus,"done")
            .orderByDesc(ShortDramaVisualAsset::getUpdateTime).last("limit 1"));
        if(a==null)return null;
        try { return shot!=null && isCurrentFrame(a,shot)?a.getImageUrl():null; }
        catch(Exception e) {return null;}
    }
    private String manualStartFrame(ShortDramaStoryboard shot) {
        if (shot == null) return null;
        try {
            String url = JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"))
                .path("manual_start_frame").path("url").asText("");
            return url.startsWith("https://") ? url : null;
        } catch (Exception ignored) { return null; }
    }
    private boolean isCurrentFrame(ShortDramaVisualAsset a, ShortDramaStoryboard shot) throws Exception {
        String propsVersion=JSON.readTree(a.getReferenceImages()).path("propsVersion").asText();
        return Objects.equals(a.getSourceHash(),productionFrameHash(shot,baseReferences(shot),propsVersion));
    }
    private static String safeError(Exception e) {String message=e.getMessage();return message==null?"资产任务失败":message.substring(0,Math.min(300,message.length()));}
}
