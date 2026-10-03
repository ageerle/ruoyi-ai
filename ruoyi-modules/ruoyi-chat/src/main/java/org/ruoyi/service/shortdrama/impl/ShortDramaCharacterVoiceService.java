package org.ruoyi.service.shortdrama.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.audio.provider.AtlasSpeechPayloadBuilder;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Versioned role-owned samples. No invented provider voiceId, no automatic replacement of a selected voice. */
@Service @RequiredArgsConstructor
public class ShortDramaCharacterVoiceService {
    public static final String MODEL = "bytedance/seed-audio-1.0";
    private final ShortDramaSoundService sounds;
    private final ShortDramaCharacterMapper characters;
    private final ShortDramaCharacterAppearanceMapper appearances;
    private final ShortDramaStoryboardMapper storyboards;
    private final IChatModelService models;
    private final AtlasPredictionService predictions;
    private final Map<String,Object> roleLocks = new java.util.concurrent.ConcurrentHashMap<>();
    private Object lock(Long project,Long character) {return roleLocks.computeIfAbsent(project+":"+character,key -> new Object());}
    private final OkHttpClient http = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();

    public record Settings(String description, String sampleText) {}
    public record Sample(String id, String label, String model, String predictionId, String source, double duration, long createdAt) {}
    public record Profile(String characterId, String characterName, String description, String sampleText,
                          String selectedSampleId, int version, List<Sample> samples) {}
    public record Generate(String requestId, String model, String description, String sampleText, String referenceSampleId) {}
    public record Job(String id, Generate request, String predictionId, String status, String error, String outputUrl, long createdAt) {}
    public record View(Profile profile, List<Job> jobs) {}
    public record Selection(String sampleId) {}
    public record SpeakersRequest(List<String> characterIds) {}
    public record Binding(String characterId, String characterName, String sampleId, int version, double duration, int audioIndex) {}
    public record Plan(List<Binding> bindings, List<ShortDramaSoundService.Reference> references, String direction, List<String> issues, boolean explicit, List<String> speakerIds) {
        public List<Map<String,Object>> fingerprints() {return references.stream().map(ShortDramaSoundService.Reference::fingerprint).toList();}
    }
    private Path directory(Long project, Long character) {return Path.of("data", "short-drama-sounds", project.toString(), "character-voices", character.toString()).toAbsolutePath();}
    private Path profileFile(Long project, Long character) {return directory(project, character).resolve("profile.json");}
    private Path jobFile(Long project, Long character, String id) {return directory(project, character).resolve(uuid(id) + ".json");}
    private Path sampleFile(Long project, Long character, String id) {return directory(project, character).resolve(uuid(id) + ".mp3");}
    private static String uuid(String id) {
        try {String canonical = UUID.fromString(id).toString(); if (!canonical.equalsIgnoreCase(id)) throw new ShortDramaVoiceException("样音任务ID必须是完整UUID"); return canonical;}
        catch (Exception e) {throw new ShortDramaVoiceException("样音任务ID必须是完整UUID");}
    }
    private ShortDramaCharacter owner(Long project, Long character, Long user) {
        sounds.owner(project, user);var actor = characters.selectById(character);
        if (actor == null || !project.equals(actor.getProjectId())) throw new ShortDramaVoiceException("角色不属于当前项目");
        return actor;
    }
    private List<ShortDramaCharacter> cast(Long project) {return characters.selectList(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId,project).orderByAsc(ShortDramaCharacter::getId));}
    private Profile read(Long project, ShortDramaCharacter actor) throws Exception {
        Path file = profileFile(project, actor.getId());
        if (Files.exists(file)) return AtlasMediaSupport.OBJECT_MAPPER.readValue(file.toFile(), Profile.class);
        String legacy = appearances.selectList(new LambdaQueryWrapper<ShortDramaCharacterAppearance>().eq(ShortDramaCharacterAppearance::getCharacterId,actor.getId()))
            .stream().map(ShortDramaCharacterAppearance::getVoice).filter(v -> v != null && !v.isBlank()).findFirst().orElse("");
        return new Profile(actor.getId().toString(), actor.getName(), legacy.isBlank() ? String.join("，", Objects.toString(actor.getGender(),""), Objects.toString(actor.getAgeRange(),"")) : legacy,
            "你先听我把话说完。", null, 0, List.of());
    }
    private void write(Path file, Object value) throws Exception {
        Files.createDirectories(file.getParent());Path temporary = Files.createTempFile(file.getParent(), "voice-", ".tmp");
        try {AtlasMediaSupport.OBJECT_MAPPER.writeValue(temporary.toFile(), value);
            try {Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);}
            catch (AtomicMoveNotSupportedException e) {Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }
    public View get(Long project, Long character, Long user) throws Exception {synchronized(lock(project,character)){return getLocked(project,character,user);}}
    private View getLocked(Long project, Long character, Long user) throws Exception {
        var actor = owner(project,character,user);List<Job> jobs = new ArrayList<>();Path dir=directory(project,character);
        if (Files.isDirectory(dir)) try (var files=Files.list(dir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().matches("[a-f0-9-]{36}\\.json")).toList()) {
                var job=AtlasMediaSupport.OBJECT_MAPPER.readValue(file.toFile(),Job.class);
                if ("submitting".equals(job.status()) && System.currentTimeMillis()-job.createdAt()>210000)
                    job=new Job(job.id(),job.request(),job.predictionId(),"submission_unknown","提交结果未知，请核对上游记录；未自动重试",job.outputUrl(),job.createdAt());
                jobs.add(job);
            }
        }
        jobs.sort(Comparator.comparingLong(Job::createdAt).reversed());return new View(read(project,actor),List.copyOf(jobs));
    }
    static void validate(Settings settings) {
        if (settings==null || settings.description()==null || settings.description().isBlank() || settings.description().length()>1500
            || settings.sampleText()==null || settings.sampleText().isBlank() || settings.sampleText().length()>80)
            throw new ShortDramaVoiceException("请填写声音描述（最多1500字）与简短样音正文（最多80字）");
    }
    public Profile settings(Long project,Long character,Long user,Settings settings) throws Exception {synchronized(lock(project,character)){return settingsLocked(project,character,user,settings);}}
    private Profile settingsLocked(Long project,Long character,Long user,Settings settings) throws Exception {
        validate(settings);var old=read(project,owner(project,character,user));var updated=new Profile(old.characterId(),old.characterName(),settings.description().trim(),settings.sampleText().trim(),old.selectedSampleId(),old.version(),old.samples());
        write(profileFile(project,character),updated);return updated;
    }
    private ChatModelVo model(String name) {
        var model=models.selectModelByName(name);
        if (model==null || !MODEL.equals(name) || !"audio".equals(model.getCategory()) || !"atlas".equalsIgnoreCase(model.getProviderCode()))
            throw new ShortDramaVoiceException("请在模型配置中启用 Atlas Seed Audio 1.0");
        return model;
    }
    static String prompt(Generate request) {
        return "生成单人中文角色样音。只朗读正文，不能读说明、角色名或标签，不增加对白，不加音乐和其他说话人。声音设定："
            + request.description() + (request.referenceSampleId()!=null && !request.referenceSampleId().isBlank() ? "。保持 @audio1 的同一人音色，情绪按声音设定表演。" : "。")
            + "\n正文：" + request.sampleText();
    }
    public Job generate(Long project,Long character,Long user,Generate request) throws Exception {synchronized(lock(project,character)){return generateLocked(project,character,user,request);}}
    private Job generateLocked(Long project,Long character,Long user,Generate request) throws Exception {
        var actor=owner(project,character,user);if(request==null)throw new ShortDramaVoiceException("缺少请求");uuid(request.requestId());validate(new Settings(request.description(),request.sampleText()));var model=model(request.model());
        Path file=jobFile(project,character,request.requestId());
        if (Files.exists(file)) {var existing=AtlasMediaSupport.OBJECT_MAPPER.readValue(file.toFile(),Job.class);if(!existing.request().equals(request))throw new ShortDramaVoiceException("任务ID已用于不同样音，请创建新任务");return existing;}
        List<Map<String,String>> references=List.of();
        if(request.referenceSampleId()!=null && !request.referenceSampleId().isBlank()) {
            sample(read(project,actor),request.referenceSampleId());
            references=List.of(Map.of("audioData",Base64.getEncoder().encodeToString(Files.readAllBytes(sampleFile(project,character,request.referenceSampleId())))));
        }
        var payload=AtlasSpeechPayloadBuilder.build(AudioContext.builder().chatModelVo(model).input(prompt(request)).references(references).responseFormat("mp3").sampleRate(24000).build());
        var initial=new Job(request.requestId(),request,null,"submitting","",null,System.currentTimeMillis());
        // A durable claim precedes the only paid POST; reuse of a UUID never resubmits.
        Files.createDirectories(file.getParent());Files.writeString(file,AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(initial),StandardOpenOption.CREATE_NEW);
        var post=new Request.Builder().url(AtlasMediaSupport.endpoint(model.getApiHost(),"/model/generateAudio"))
            .header("Authorization","Bearer "+model.getApiKey()).post(RequestBody.create(payload.toString(),AtlasMediaSupport.JSON)).build();
        Job result;
        try(Response response=http.newCall(post).execute()) {
            String raw=response.body()==null?"":response.body().string();
            if(!response.isSuccessful()) result=new Job(initial.id(),request,null,response.code()>=500?"submission_unknown":"failed","样音提交返回HTTP "+response.code()+"，请检查上游任务、模型权限与余额",null,initial.createdAt());
            else {
                var root=AtlasMediaSupport.OBJECT_MAPPER.readTree(raw);if(!root.has("data") && root.has("id"))raw=AtlasMediaSupport.OBJECT_MAPPER.createObjectNode().set("data",root).toString();
                var prediction=predictions.toResponse(raw,"audio");
                if(prediction.getId()==null || prediction.getId().isBlank())throw new IllegalStateException("未返回任务编号");
                result=new Job(initial.id(),request,prediction.getId(),"processing","",prediction.getUrl(),initial.createdAt());
            }
        } catch(Exception e) {result=new Job(initial.id(),request,null,"submission_unknown","提交结果未知，请核对 Atlas 记录；未自动重试",null,initial.createdAt());}
        write(file,result);return result;
    }
    public Job poll(Long project,Long character,Long user,String id) throws Exception {synchronized(lock(project,character)){return pollLocked(project,character,user,id);}}
    private Job pollLocked(Long project,Long character,Long user,String id) throws Exception {
        var actor=owner(project,character,user);Path file=jobFile(project,character,id);var old=AtlasMediaSupport.OBJECT_MAPPER.readValue(file.toFile(),Job.class);
        if(!Set.of("processing","saving").contains(old.status()))return old;
        String url=old.outputUrl();
        if(!"saving".equals(old.status())) {
            var result=predictions.retrieve(model(old.request().model()),old.predictionId());String status=Objects.toString(result.getStatus(),"");
            if(Set.of("failed","cancelled","canceled").contains(status)) {
                var root=AtlasMediaSupport.OBJECT_MAPPER.readTree(result.getRawResponse());var detail=root.path("data").path("error");
                String message=detail.isTextual()?detail.asText():detail.path("message").asText("上游样音生成失败，请检查 Atlas 任务记录");
                if(message.contains("concurrency"))message="上游配音并发额度已满，请等其他样音完成后创建新任务；本任务未自动重交";
                var failed=new Job(id,old.request(),old.predictionId(),"failed",message.substring(0,Math.min(500,message.length())),null,old.createdAt());write(file,failed);return failed;
            }
            if(!Set.of("completed","succeeded").contains(status))return old;
            url=result.getUrl();if(url==null || url.isBlank()){var failed=new Job(id,old.request(),old.predictionId(),"failed","任务完成但未返回音频",null,old.createdAt());write(file,failed);return failed;}
        }
        var saving=new Job(id,old.request(),old.predictionId(),"saving","样音已生成，正在保存",url,old.createdAt());write(file,saving);
        try {
            var profile=read(project,actor);
            if(profile.samples().stream().noneMatch(s -> s.id().equals(id))) {
                Path input=Files.createTempFile(directory(project,character),"audio-",".bin");
                try {download(url,input);double duration=normalize(input,sampleFile(project,character,id));
                    add(project,character,profile,new Sample(id,actor.getName()+" · 样音 "+(profile.samples().size()+1),MODEL,old.predictionId(),"generated",duration,System.currentTimeMillis()));
                } finally {Files.deleteIfExists(input);}
            }
            var completed=new Job(id,old.request(),old.predictionId(),"completed","",url,old.createdAt());write(file,completed);return completed;
        } catch(IllegalArgumentException e) {var failed=new Job(id,old.request(),old.predictionId(),"failed",e.getMessage(),url,old.createdAt());write(file,failed);return failed;}
          catch(Exception e) {
              String detail=Objects.toString(e.getMessage(),e.getClass().getSimpleName());
              var retry=new Job(id,old.request(),old.predictionId(),"saving","样音已生成，保存暂未完成；刷新可重试保存："+detail.substring(0,Math.min(240,detail.length())),url,old.createdAt());
              write(file,retry);return retry;
          }
    }
    private void download(String url,Path input) throws Exception {
        var uri=java.net.URI.create(url);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)throw new ShortDramaVoiceException("上游样音地址无效");
        try(var response=http.newCall(new Request.Builder().url(url).build()).execute()) {
            if(!response.isSuccessful() || response.body()==null)throw new IllegalStateException("上游样音下载HTTP "+response.code());
            try(var stream=response.body().byteStream();var output=Files.newOutputStream(input)){byte[] buffer=new byte[8192];int n;long size=0;while((n=stream.read(buffer))!=-1){size+=n;if(size>10*1024*1024)throw new ShortDramaVoiceException("样音超过10MB");output.write(buffer,0,n);}}
        }
    }
    private double normalize(Path input,Path target) throws Exception {
        Path probe=target.resolveSibling(target.getFileName()+".probe");
        ShortDramaSoundService.run(List.of("ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1:nk=1",input.toString()),probe);
        double duration=Double.parseDouble(Files.readString(probe).trim());
        if(!Double.isFinite(duration) || duration<2 || duration>30.1)throw new ShortDramaVoiceException("样音需要2至30秒，请用更短正文或上传短片段；原输出已保留");
        ShortDramaSoundService.run(List.of("ffmpeg","-v","error","-y","-i",input.toString(),"-vn","-ar","24000","-ac","1","-c:a","libmp3lame",target.toString()),target.resolveSibling(target.getFileName()+".log"));
        ShortDramaSoundService.run(List.of("ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1:nk=1",target.toString()),probe);
        return Double.parseDouble(Files.readString(probe).trim());
    }
    private void add(Long project,Long character,Profile old,Sample sample) throws Exception {
        List<Sample> samples=new ArrayList<>(old.samples());samples.add(sample);
        write(profileFile(project,character),new Profile(old.characterId(),old.characterName(),old.description(),old.sampleText(),old.selectedSampleId(),old.version(),List.copyOf(samples)));
    }
    public Sample upload(Long project,Long character,Long user,MultipartFile file) throws Exception {synchronized(lock(project,character)){return uploadLocked(project,character,user,file);}}
    private Sample uploadLocked(Long project,Long character,Long user,MultipartFile file) throws Exception {
        var old=read(project,owner(project,character,user));if(file.isEmpty() || file.getSize()>10*1024*1024)throw new ShortDramaVoiceException("请上传小于10MB的样音");
        Files.createDirectories(directory(project,character));String id=UUID.randomUUID().toString();Path input=Files.createTempFile(directory(project,character),"upload-",".bin");
        try {file.transferTo(input);var sample=new Sample(id,Objects.toString(file.getOriginalFilename(),"上传样音"),null,null,"uploaded",normalize(input,sampleFile(project,character,id)),System.currentTimeMillis());add(project,character,old,sample);return sample;}
        finally {Files.deleteIfExists(input);}
    }
    private Sample sample(Profile profile,String id) {return profile.samples().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow(() -> new ShortDramaVoiceException("样音不属于当前角色或尚未保存"));}
    public Profile select(Long project,Long character,Long user,Selection selection) throws Exception {synchronized(lock(project,character)){return selectLocked(project,character,user,selection);}}
    private Profile selectLocked(Long project,Long character,Long user,Selection selection) throws Exception {
        var old=read(project,owner(project,character,user));String id=selection.sampleId();if(id!=null && !id.isBlank()){sample(old,id);if(!Files.isRegularFile(sampleFile(project,character,id)))throw new ShortDramaVoiceException("样音文件丢失，请重新保存");}else id=null;
        var selected=new Profile(old.characterId(),old.characterName(),old.description(),old.sampleText(),id,old.version()+(Objects.equals(id,old.selectedSampleId())?0:1),old.samples());write(profileFile(project,character),selected);return selected;
    }
    public Path audio(Long project,Long character,Long user,String id) throws Exception {sample(read(project,owner(project,character,user)),id);return sampleFile(project,character,id);}
    public record Speech(String text,List<Map<String,String>> references) {}
    public Speech speech(ShortDramaAudio audio) {
        if(!"dialogue".equals(audio.getAudioType()) || audio.getLinkedStoryboardId()==null)return new Speech(audio.getText(),List.of());
        var shot=storyboards.selectById(audio.getLinkedStoryboardId());if(shot==null || !audio.getProjectId().equals(shot.getProjectId()))throw new ShortDramaVoiceException("关联对白分镜不属于当前项目");
        var plan=plan(shot,null,true);List<Map<String,String>> refs=new ArrayList<>();StringBuilder mapping=new StringBuilder();
        try {
            for(var binding:plan.bindings()) {
                refs.add(Map.of("audioData",Base64.getEncoder().encodeToString(Files.readAllBytes(sampleFile(audio.getProjectId(),Long.valueOf(binding.characterId()),binding.sampleId())))));
                mapping.append(binding.characterName()).append("使用 @audio").append(refs.size()).append(" 的音色；");
            }
        }catch(Exception e){throw new IllegalStateException("对白角色样音无法读取",e);}
        if(refs.size()>3)throw new ShortDramaVoiceException("Seed Audio最多3个说话人，请按发言段落拆分配音");
        String text=refs.isEmpty()?audio.getText():"角色配音任务。只说原对白，不朗读说明和角色标签，不增加台词或背景音乐。"+mapping+"\n原对白：\n"+audio.getText();
        return new Speech(text,List.copyOf(refs));
    }
    private ShortDramaStoryboard shot(Long project,Long storyboard,Long user) {sounds.owner(project,user);var shot=storyboards.selectById(storyboard);if(shot==null || !project.equals(shot.getProjectId()))throw new ShortDramaVoiceException("分镜不属于当前项目");return shot;}
    public synchronized ShortDramaStoryboard speakers(Long project,Long storyboard,Long user,SpeakersRequest request) throws Exception {
        var shot=shot(project,storyboard,user);String previous=shot.getContinuityJson();ObjectNode continuity=(ObjectNode)AtlasMediaSupport.OBJECT_MAPPER.readTree(Objects.toString(previous,"{}"));
        if(continuity==null)continuity=AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        if(request.characterIds()==null)continuity.remove("voice_speakers");else continuity.set("voice_speakers",AtlasMediaSupport.OBJECT_MAPPER.valueToTree(request.characterIds().stream().distinct().toList()));
        shot.setContinuityJson(continuity.toString());ShortDramaVoiceContract.resolve(shot,cast(project));
        // Only this field changes, and a concurrent edit cannot be overwritten.
        var condition=new LambdaUpdateWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getId,storyboard)
            .eq(previous!=null,ShortDramaStoryboard::getContinuityJson,previous).isNull(previous==null,ShortDramaStoryboard::getContinuityJson).set(ShortDramaStoryboard::getContinuityJson,shot.getContinuityJson());
        if(storyboards.update(null,condition)!=1)throw new IllegalStateException("分镜已被更新，请刷新后重试");return shot;
    }
    public Plan preview(Long project,Long storyboard,Long user,String model) throws Exception {return plan(shot(project,storyboard,user),model,false);}
    public Plan plan(ShortDramaStoryboard shot,String model,boolean required) {
        try {
            var actors=cast(shot.getProjectId());var resolved=ShortDramaVoiceContract.resolve(shot,actors);
            boolean configured=actors.stream().anyMatch(c -> Files.exists(profileFile(shot.getProjectId(),c.getId())));
            List<String> issues=new ArrayList<>();if(configured)issues.addAll(resolved.issues());
            List<Binding> bindings=new ArrayList<>();List<ShortDramaSoundService.Reference> refs=new ArrayList<>();
            for(var actor:resolved.actors()) {
                var profile=read(shot.getProjectId(),actor);
                if(profile.selectedSampleId()==null) {if(configured)issues.add(actor.getName()+"尚未选用样音，请先在角色卡绑定");continue;}
                var sample=sample(profile,profile.selectedSampleId());Path file=sampleFile(shot.getProjectId(),actor.getId(),sample.id());
                if(!Files.isRegularFile(file)){issues.add(actor.getName()+"已选样音文件丢失");continue;}
                refs.add(ShortDramaSoundService.Reference.of("voice:"+actor.getId()+":"+sample.id(),actor.getName(),file,sample.duration()));
                bindings.add(new Binding(actor.getId().toString(),actor.getName(),sample.id(),profile.version(),sample.duration(),refs.size()));
            }
            refs.addAll(sounds.referenceFiles(shot.getProjectId(),shot.getSceneNo()));
            if(model!=null && !model.isBlank())try {ShortDramaVoiceContract.validateReferences(model,refs.stream().map(ShortDramaSoundService.Reference::duration).toList());}catch(IllegalArgumentException e){issues.add(e.getMessage());}
            if(required && !issues.isEmpty())throw new ShortDramaVoiceException("镜"+shot.getSceneNo()+"声音绑定未就绪："+String.join("；",issues));
            String direction=bindings.isEmpty()?"":"\n【本镜角色声音绑定】\n"+bindings.stream().map(b -> b.characterName()+"（角色ID "+b.characterId()+"）使用 @音频"+b.audioIndex()+" 的音色").reduce((a,b)->a+"\n"+b).orElse("")
                +"\n这些样音只用于同一角色音色参考。按原对白与声源顺序表演，画外对白仍由对应角色发声；不要朗读样音正文或绑定说明，不给静默听者增加台词。";
            return new Plan(List.copyOf(bindings),List.copyOf(refs),direction,List.copyOf(issues),resolved.explicit(),resolved.actors().stream().map(c -> c.getId().toString()).toList());
        } catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("角色声音档案无法读取，请修复后再生成视频",e);}
    }
}
