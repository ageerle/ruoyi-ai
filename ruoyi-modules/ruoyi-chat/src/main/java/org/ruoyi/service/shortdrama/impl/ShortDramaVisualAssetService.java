package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ExecutorService coordinators = Executors.newFixedThreadPool(2);
    private final ExecutorService imageWorkers = Executors.newFixedThreadPool(4);
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
    public record AssetView(Long id,Long storyboardId,String kind,String title,String prompt,String status,
            String imageUrl,String model,String predictionId,String errorMessage) { }
    public Map<String,Object> status(Long id, Long user) {
        var project=owner(id,user);
        var rows = rows(id);
        return Map.of("outdated","script_changed".equals(project.getStatus()),"running",running.containsKey(id),"phase",running.getOrDefault(id,"idle"),
            "error",errors.getOrDefault(id,""),"assets",rows.stream().map(a->new AssetView(a.getId(),a.getStoryboardId(),a.getKind(),a.getTitle(),a.getPrompt(),a.getStatus(),a.getImageUrl(),a.getModel(),a.getPredictionId(),a.getErrorMessage())).toList(),
            "completed",rows.stream().filter(a -> "done".equals(a.getStatus())).count(),"total",rows.size());
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
        var m=models.selectModelByName(model);
        if(m==null) { var q=new ChatModelBo();q.setCategory("chat");m=models.queryAvailableList(q).stream().findFirst().orElseThrow(); }
        var chat=chats.getOriginalService(ShortDramaServiceImpl.shortDramaProviderCode(m.getProviderCode(),m.getModelName())).buildChatModel(m);
        // Do not invent extra characters or costumes merely to increase image counts.
        String prompt="从剧本提取反复出现、跨镜必须保持外观一致的关键实物道具。不是人物、建筑、屏幕截图或人物动作。只要3-10项，若不足3项按实际数量。合并同一个物体的别称，不把同型号的不同物体强行合并。描述外形材质颜色磨损，不新增品牌或剧情；账页和屏幕清晰文字由后期素材替换。返回JSON数组 [{\"name\":\"物体名\",\"description\":\"可生成单一道具定妆图的描述\",\"shot_numbers\":[1,2]}]。shot_numbers只填该实物真实出现的镜头，不按同名词误配。剧本：\n"+script.getScriptText()+"\n镜头列表：\n"+
            shots.stream().map(s->s.getSceneNo()+":"+s.getSourceText()+"\n本镜画面设计："+Objects.toString(s.getImagePrompt(),""))
                .reduce("",(a,b)->a+b+"\n")
            +"\n正文是简述，道具覆盖必须同时参考各镜画面设计。区分画面中可见、包内收起、禁止出现等语境；不能因通用提示里出现一个物体名就绑定所有镜头。";
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
            String desc="写实电影道具定妆图。单个物体，简洁中性背景，清楚表现正面和关键结构，不出现人物、拼贴分格、水印或额外道具。"+description;
            // reference_images holds typed linkage for props until image generation; no external URL is inferred.
            upsert(id,null,key,"prop",name,desc,JsonUtils.toJsonString(linked),DigestUtil.sha256Hex(desc+linked));
        }
        String propsVersion=props.toString();
        for(var shot:shots) {
            if (directInsert(shot.getContinuityJson())) continue;
            var refs=baseReferences(shot);
            if(refs.isEmpty()) throw new IllegalStateException("镜头"+shot.getSceneNo()+"缺少已确认的人物/场景参考图");
            String key="shot:"+shot.getId();keys.add(key);
            String framePrompt=framePrompt(shot);
            upsert(id,shot.getId(),key,"shot_frame","镜 "+shot.getSceneNo()+" · "+shot.getSceneTitle(),framePrompt,
                JsonUtils.toJsonString(Map.of("urls",refs,"propsVersion",propsVersion)),frameHash(shot,refs,propsVersion));
        }
        for(var old:rows(id)) if(!keys.contains(old.getAssetKey()) && !Objects.toString(old.getSourceHash(),"").startsWith("locked:")) assets.update(null,new LambdaUpdateWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getId,old.getId()).set(ShortDramaVisualAsset::getStatus,"obsolete"));
    }
    static String framePrompt(ShortDramaStoryboard shot) {
        String start="", anchor="", present="", directive="";
        try { var continuity=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}"));
            directive=continuity.path("keyframe_directive").asText("");
            start=continuity.path("start_state").asText(""); anchor=continuity.path("spatial_anchor").asText("");
            present=continuity.path("present_characters").toString();
            var cast=continuity.path("present_characters");
            if(cast.isArray() && cast.size()==1) {
                // A scene-wide anchor can mention someone who has not entered this shot yet.
                // For a solo shot use its structured binding, not the shared cast description.
                String name=cast.get(0).asText(), slot="";
                var bindings=JSON.readTree(Objects.toString(shot.getCharactersJson(),"[]"));
                for(var binding:bindings) if(name.equals(binding.path("name").asText())) slot=binding.path("slot").asText("");
                anchor="本镜画面严格只有1人："+name+"，位置："+slot+"。背景、门口、玻璃反射均无其他人物，不提前画入尚未入场的配角。视线和动作遵照起始状态。";
            }
        } catch(Exception ignored) { }
        return ShortDramaDirectorSkills.load("visual-world", "director-blocking", "director-review") + "电影单镜首帧，单幅完整构图，不生成三视图、分镜格或字幕。参考图中的多视角人物只作为同一身份，不复制成多人。严格按本镜角色数量、持物手、坐立和门灯状态构图。只画起始状态，不提前执行结束动作。清晰账页、软件屏幕留给后期替换。\n"
            +"本镜景别与角度："+Objects.toString(shot.getShotType(),"")+"。构图以本镜要求为准，参考人物和场景图只锁定身份、外观和建筑，不继承它们的景别、正面机位或人物站位。\n"
            +"出镜人物名单（空数组表示无人）："+present+"\n空间与视线："+anchor+"\n起始状态："+start+"\n"
            +(start.isBlank()?Objects.toString(shot.getImagePrompt(),""):"起始姿态为本图唯一动作依据，不拼接后续动作结果。服装、身份和建筑细节遵照绑定参考图。")
            + closeFrameInstruction(shot)
            +(directive.isBlank()?"":"\n本镜经人工审阅的构图修订（在起始状态内落实）："+directive);
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
        return "\n景别落实："+type+"以"+focus+"为视觉中心。人物近景只保留头肩或上半身，人物特写聚焦脸部或动作细节；道具特写只框叙事物体。不得退成展示整个房间和整张桌子的双人全景。名单限定允许出现的人物，不要求全部同时入画；同场倾听者可以在画外或只有肩部边缘，禁止为了参考图齐全而扩宽构图。";
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
                upsert(id,shot.getId(),old.getAssetKey(),"shot_frame","镜 "+shot.getSceneNo()+" · "+shot.getSceneTitle(),framePrompt(shot),JsonUtils.toJsonString(Map.of("urls",refs,"propsVersion",propsVersion)),frameHash(shot,refs,propsVersion));
            } catch(Exception e) { throw new IllegalStateException("镜头"+shot.getSceneNo()+"参考图需要更新",e); }
        }
    }
    static String frameHash(ShortDramaStoryboard shot,List<String> refs,String props) {
        return DigestUtil.sha256Hex(framePrompt(shot)+shot.getPhotographyRules()+shot.getCharactersJson()+shot.getContinuityJson()+refs+props);
    }
    private void upsert(Long project,Long shot,String key,String kind,String title,String prompt,String refs,String hash) {
        var old=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getProjectId,project).eq(ShortDramaVisualAsset::getAssetKey,key));
        if(old!=null && Objects.toString(old.getSourceHash(),"").startsWith("locked:") && !hash.startsWith("locked:"))return;
        if(old!=null && hash.equals(old.getSourceHash()) && !"obsolete".equals(old.getStatus())) return;
        if(old==null) {old=new ShortDramaVisualAsset();old.setId(IdUtil.getSnowflakeNextId());old.setProjectId(project);old.setAssetKey(key);}
        boolean insert=old.getKind()==null;
        old.setStoryboardId(shot);old.setKind(kind);old.setTitle(title);old.setPrompt(prompt);old.setReferenceImages(refs);old.setSourceHash(hash);old.setStatus("pending");
        old.setImageUrl("");old.setPredictionId("");old.setErrorMessage("");old.setUpdateTime(new Date());
        if(insert) {old.setCreateTime(new Date());assets.insert(old);} else assets.updateById(old);
    }
    static boolean voiceOnly(ShortDramaCharacter ch) {
        String introduction=Objects.toString(ch.getIntroduction(),"");
        return introduction.contains("全剧仅以手机语音出现") || introduction.contains("全剧仅画外音") || introduction.contains("无实体出场");
    }
    private List<String> baseReferences(ShortDramaStoryboard shot) throws Exception {
        List<String> refs=new ArrayList<>();
        var bindings=JSON.readTree(shot.getCharactersJson()==null?"[]":shot.getCharactersJson());
        for(var binding:bindings) {
            var ch=characters.selectOne(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId,shot.getProjectId()).eq(ShortDramaCharacter::getName,binding.path("name").asText()).last("limit 1"));
            if(ch==null) throw new IllegalStateException("人物绑定不存在："+binding.path("name").asText());
            if(voiceOnly(ch)) continue;
            var variants=appearances.selectList(new LambdaQueryWrapper<ShortDramaCharacterAppearance>().eq(ShortDramaCharacterAppearance::getCharacterId,ch.getId()).orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            var selected=variants.stream().filter(a->Objects.equals(a.getChangeReason(),binding.path("appearance").asText())).findFirst().orElse(variants.isEmpty()?null:variants.get(0));
            String url=selected==null?ch.getReferenceImageUrl():selected.getReferenceImageUrl();
            if(url==null || url.isBlank()) throw new IllegalStateException(ch.getName()+"缺少已确认形象图");
            if(!refs.contains(url))refs.add(url);
        }
        var loc=locations.selectOne(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId,shot.getProjectId()).eq(ShortDramaLocation::getName,shot.getLocationName()).last("limit 1"));
        if(loc==null || loc.getReferenceImageUrl()==null || loc.getReferenceImageUrl().isBlank()) throw new IllegalStateException("场景缺少参考图："+shot.getLocationName());
        if(!refs.contains(loc.getReferenceImageUrl()))refs.add(loc.getReferenceImageUrl());
        return refs;
    }
    public Map<String,Object> saveProp(Long id,String title,String prompt,List<Integer> shotNumbers,String imageUrl,Long user) {
        requireCurrentScript(id,user);
        if(title==null || title.isBlank() || title.length()>80 || prompt==null || prompt.isBlank() || prompt.length()>12000)
            throw new IllegalArgumentException("请填写道具名和外观描述");
        var shots=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id));
        Set<Integer> valid=new HashSet<>();shots.forEach(s->valid.add(s.getSceneNo()));
        if(shotNumbers==null || shotNumbers.isEmpty() || !valid.containsAll(shotNumbers))throw new IllegalArgumentException("请选择实际存在的镜号");
        if(running.putIfAbsent(id,"保存固定道具")!=null)throw new IllegalStateException("请等待资产生成完成");
        try {
            var linked=shotNumbers.stream().distinct().sorted().toList();
            upsert(id,null,"prop:"+title.trim(),"prop",title.trim(),prompt,JsonUtils.toJsonString(linked),"locked:"+DigestUtil.sha256Hex(prompt+linked+Objects.toString(imageUrl,"")).substring(0,57));
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
    public Map<String,Object> generateOpening(Long id,String model,Long user) { return generateRange(id,model,user,1,10); }
    public Map<String,Object> generateRange(Long id,String model,Long user,int start,int end) {
        if(start<1 || end<start || end-start>=10)throw new IllegalArgumentException("每批请选择连续1至10镜");
        requireCurrentScript(id,user);
        var configured=models.selectModelByName(model);
        if(configured==null || !"image".equals(configured.getCategory()))throw new IllegalArgumentException("请选择生图模型");
        var selected=boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,id).ge(ShortDramaStoryboard::getSceneNo,start).le(ShortDramaStoryboard::getSceneNo,end))
            .stream().map(ShortDramaStoryboard::getId).collect(java.util.stream.Collectors.toSet());
        if(selected.isEmpty())throw new IllegalArgumentException("范围内没有镜头");
        if(running.putIfAbsent(id,"生成第"+start+"至"+end+"镜资产")!=null)throw new IllegalStateException("资产任务正在运行");
        String tenant=TenantHelper.getTenantId();errors.remove(id);
        coordinators.submit(()->TenantHelper.dynamic(tenant,()->{
            try {
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
        List<String> result=new ArrayList<>();
        try {
            var visible=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("visible_props");
            for(var a:rows(shot.getProjectId()))if("prop".equals(a.getKind()) && "done".equals(a.getStatus())
                && (visible.isArray()?hasProp(visible,a.getTitle()):linked(a,shot.getSceneNo()))) result.add(a.getImageUrl());
        }catch(Exception e){throw new IllegalStateException("道具参考绑定无效",e);}
        return result;
    }
    public Map<String,Object> generate(Long id,String model,Long user) {
        requireCurrentScript(id,user);
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
            asset.setSourceHash(frameHash(currentShot,currentRefs,metadata.path("propsVersion").asText()));
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
            List<String> refs=new ArrayList<>();String prompt=a.getPrompt();
            if("shot_frame".equals(a.getKind())) {
                var shot=boards.selectById(a.getStoryboardId());
                if(shot==null || !isCurrentFrame(a,shot))throw new IllegalStateException("镜头或参考图已修改，请重新分析资产");
                refs.addAll(baseReferences(shot));
                var bindings=JSON.readTree(shot.getCharactersJson());int index=1;
                for(var binding:bindings) {
                    var ch=characters.selectOne(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId,shot.getProjectId()).eq(ShortDramaCharacter::getName,binding.path("name").asText()).last("limit 1"));
                    if(ch!=null && !voiceOnly(ch))prompt+="\n参考图"+(index++)+"绑定人物："+binding.path("name").asText()+"。";
                }
                prompt+="\n参考图"+refs.size()+"绑定场景建筑，移动物体按本镜起始状态摆放。";
                var compositionRefs=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("composition_reference_urls");
                if(compositionRefs.isArray()) {
                    if(compositionRefs.size()>2)throw new IllegalArgumentException("每镜最多2张连续构图参考");
                    for(var ref:compositionRefs) {
                        String url=ref.asText();if(!url.startsWith("https://"))throw new IllegalArgumentException("构图参考必须为已上传图片");
                        if(!refs.contains(url)){refs.add(url);prompt+="\n参考图"+refs.size()+"为相邻已审阅镜头：锁定摄影轴线、电脑桌与背景方位、人物身份服装。只按当前起始状态改变手势和道具位置，不复制参考镜的动作。";}
                    }
                }
                var visibleProps=JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}")).path("visible_props");
                for(var prop:rows(a.getProjectId()))if("prop".equals(prop.getKind()) && (visibleProps.isArray() ? hasProp(visibleProps,prop.getTitle()) : linked(prop,shot.getSceneNo()))) {
                    if(!"done".equals(prop.getStatus()))throw new IllegalStateException("等待道具资产："+prop.getTitle());
                    refs.add(prop.getImageUrl());prompt+="\n参考图"+refs.size()+"绑定道具："+prop.getTitle()+"。固定外观以此道具图为准，不能沿用其他参考里的旧物件。"+prop.getPrompt();
                }
            }
            String selected=model;
            if(!refs.isEmpty() && model.endsWith("/text-to-image"))selected=model.replace("/text-to-image","/edit");
            if(refs.isEmpty() && model.endsWith("/edit"))selected=model.replace("/edit","/text-to-image");
            var m=models.selectModelByName(selected);if(m==null)throw new IllegalStateException("缺少对应多参考图模型配置："+selected);
            // Resume acknowledged jobs; never submit the same prediction twice.
            boolean resume=a.getPredictionId()!=null && !a.getPredictionId().isBlank() && !"failed".equals(a.getStatus());
            if(resume && a.getModel()!=null)m=models.selectModelByName(a.getModel());
            a.setModel(m.getModelName());a.setErrorMessage("");a.setStatus("generating");a.setUpdateTime(new Date());assets.updateById(a);
            if(!resume) {
                a.setPredictionId("");assets.updateById(a);
                var result=images.getOriginalService(m.getProviderCode()).startImageGeneration(ImageContext.builder().chatModelVo(m)
                    .prompt(prompt).size("shot_frame".equals(a.getKind())?projects.selectById(a.getProjectId()).getComposeAspectRatio():"1:1")
                    .referenceImages(refs).build());
                if(result==null || result.getId()==null || result.getId().isBlank())throw new IllegalStateException("模型未返回任务ID");
                a.setPredictionId(result.getId());assets.updateById(a);
            }
            long deadline=System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(8);
            while(System.currentTimeMillis()<deadline) {
                if(Thread.currentThread().isInterrupted())return;
                var result=predictions.retrieve(m,a.getPredictionId());
                if(result!=null && ("completed".equals(result.getStatus()) || "succeeded".equals(result.getStatus())) && result.getUrl()!=null && !result.getUrl().isBlank()) {
                    a.setImageUrl(result.getUrl());a.setStatus("done");a.setUpdateTime(new Date());assets.updateById(a);return;
                }
                if(result!=null && "failed".equals(result.getStatus())) {providerFailed=true;throw new IllegalStateException("模型图片任务失败");}
                Thread.sleep(2500);
            }
            a.setStatus("waiting");a.setErrorMessage("服务端任务仍未完成；再次继续会查询原任务，不重复提交");assets.updateById(a);
        } catch(InterruptedException e) {Thread.currentThread().interrupt();}
        catch(Exception e) {a.setStatus(!providerFailed && a.getPredictionId()!=null && !a.getPredictionId().isBlank()?"waiting":"failed");a.setErrorMessage(safeError(e));a.setUpdateTime(new Date());assets.updateById(a);}
    }
    private boolean linked(ShortDramaVisualAsset prop,int number) throws Exception {
        for(var n:JSON.readTree(prop.getReferenceImages()))if(n.asInt()==number)return true;return false;
    }
    private boolean hasProp(JsonNode names,String title) { for(var n:names) if(title.equals(n.asText()))return true;return false; }
    public String readyFrame(Long storyboard) {
        var a=assets.selectOne(new LambdaQueryWrapper<ShortDramaVisualAsset>().eq(ShortDramaVisualAsset::getStoryboardId,storyboard).eq(ShortDramaVisualAsset::getStatus,"done").last("limit 1"));
        if(a==null)return null;
        var shot=boards.selectById(storyboard);
        try { return shot!=null && isCurrentFrame(a,shot)?a.getImageUrl():null; }
        catch(Exception e) {return null;}
    }
    private boolean isCurrentFrame(ShortDramaVisualAsset a, ShortDramaStoryboard shot) throws Exception {
        String propsVersion=JSON.readTree(a.getReferenceImages()).path("propsVersion").asText();
        return Objects.equals(a.getSourceHash(),frameHash(shot,baseReferences(shot),propsVersion));
    }
    private static String safeError(Exception e) {String message=e.getMessage();return message==null?"资产任务失败":message.substring(0,Math.min(300,message.length()));}
}
