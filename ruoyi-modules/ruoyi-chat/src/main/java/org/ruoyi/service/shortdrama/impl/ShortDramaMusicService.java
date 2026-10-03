package org.ruoyi.service.shortdrama.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mapper.shortdrama.ShortDramaScriptMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Project-owned, recoverable music tasks. TTS and music deliberately use separate contracts. */
@Service @RequiredArgsConstructor
public class ShortDramaMusicService {
    private final ShortDramaSoundService sounds;
    private final IChatModelService models;
    private final AtlasPredictionService predictions;
    private final ChatServiceFactory chats;
    private final ShortDramaScriptMapper scripts;
    private final ShortDramaStoryboardMapper storyboards;
    private final OkHttpClient http = new OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(180,TimeUnit.SECONDS).build();
    public record MusicRequest(String requestId,String model,String purpose,String title,String prompt,String style,Integer duration,String vocalGender,String negativeTags) {}
    public record Variant(int index,String url,String soundId,double duration) {}
    public record Job(String id,String predictionId,MusicRequest request,String status,String error,List<Variant> variants,long createdAt) {}
    public record BriefRequest(String model,String purpose,String direction) {}
    public record Brief(String title,String style,String prompt) {}
    public record Usage(int variant,List<Integer> shots,double volume,double offset,boolean enabled) {}
    private Path directory(Long project) {return Path.of("data","short-drama-sounds",project.toString(),"music-jobs").toAbsolutePath();}
    private Path file(Long project,String id) {UUID.fromString(id);return directory(project).resolve(id+".json");}
    private void save(Long project,Job job) throws Exception {
        Files.createDirectories(directory(project));Path temporary=Files.createTempFile(directory(project),"job-",".tmp");
        try {AtlasMediaSupport.OBJECT_MAPPER.writeValue(temporary.toFile(),job);try {Files.move(temporary,file(project,job.id()),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,file(project,job.id()),StandardCopyOption.REPLACE_EXISTING);}}
        finally {Files.deleteIfExists(temporary);}
    }
    private Job read(Long project,String id) throws Exception {return AtlasMediaSupport.OBJECT_MAPPER.readValue(file(project,id).toFile(),Job.class);}
    public List<Job> list(Long project,Long user) throws Exception {
        sounds.owner(project,user);if(!Files.isDirectory(directory(project)))return List.of();
        try(var files=Files.list(directory(project))){List<Job> jobs=new ArrayList<>();for(Path path:files.filter(p->p.toString().endsWith(".json")).toList()){var job=AtlasMediaSupport.OBJECT_MAPPER.readValue(path.toFile(),Job.class);if("submitting".equals(job.status()) && System.currentTimeMillis()-job.createdAt()>210000)job=new Job(job.id(),job.predictionId(),job.request(),"submission_unknown","提交尚未取得任务编号，请先核对 Atlas 记录；未自动重试",job.variants(),job.createdAt());jobs.add(job);}jobs.sort(Comparator.comparingLong(Job::createdAt).reversed());return jobs;}
    }
    private ChatModelVo musicModel(String name) {
        var model=models.selectModelByName(name);
        if(model==null || !"audio".equals(model.getCategory()) || !"atlas".equalsIgnoreCase(model.getProviderCode()) || !"suno/chirp-v6".equals(name))throw new IllegalArgumentException("请选择已配置的 Atlas Suno Chirp V6 音乐模型");
        return model;
    }
    static Map<String,Object> payload(MusicRequest r) {
        if(r==null || !Set.of("bgm","theme","ending").contains(Objects.toString(r.purpose(),"")))throw new IllegalArgumentException("请选择 BGM、主题曲或片尾曲");
        if(r.duration()==null || r.duration()<10 || r.duration()>360)throw new IllegalArgumentException("目标时长为10至360秒");
        if(r.title()==null || r.title().isBlank() || r.title().length()>50 || r.style()==null || r.style().isBlank() || r.style().length()>1000)throw new IllegalArgumentException("请填写标题（最多50字符）与音乐风格（最多1000字符）");
        if(r.prompt()!=null && r.prompt().length()>3000)throw new IllegalArgumentException("歌词或音乐描述最多3000字符");
        if(!"bgm".equals(r.purpose()) && (r.prompt()==null || r.prompt().isBlank()))throw new IllegalArgumentException("歌曲需要先填写歌词");
        if(r.negativeTags()!=null && r.negativeTags().length()>1000)throw new IllegalArgumentException("排除风格最多1000字符");
        var p=new LinkedHashMap<String,Object>();p.put("model",r.model());p.put("custom",true);p.put("instrumental","bgm".equals(r.purpose()));p.put("title",r.title().trim());p.put("prompt",Objects.toString(r.prompt(),""));p.put("style",r.style().trim());p.put("duration",r.duration());p.put("variety","normal");
        if(!"bgm".equals(r.purpose())) {if(!Set.of("Male","Female").contains(Objects.toString(r.vocalGender(),"")))throw new IllegalArgumentException("请选择男声或女声");p.put("vocal_gender",r.vocalGender());p.put("auto_lyrics",false);}
        if(r.negativeTags()!=null && !r.negativeTags().isBlank())p.put("negative_tags",r.negativeTags());return p;
    }
    public synchronized Job generate(Long project,Long user,MusicRequest request) throws Exception {
        sounds.owner(project,user);UUID.fromString(request.requestId());var payload=payload(request);var model=musicModel(request.model());
        // Reusing the same client request id never submits another paid prediction, even after a timeout.
        if(Files.exists(file(project,request.requestId()))) {Job old=read(project,request.requestId());if(!old.request().equals(request))throw new IllegalArgumentException("任务ID已用于其他音乐，请创建新任务");return old;}
        var initial=new Job(request.requestId(),null,request,"submitting","",List.of(),System.currentTimeMillis());save(project,initial);
        Request post=new Request.Builder().url(AtlasMediaSupport.endpoint(model.getApiHost(),"/model/generateAudio")).header("Authorization","Bearer "+model.getApiKey()).post(RequestBody.create(AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(payload),AtlasMediaSupport.JSON)).build();
        try(Response response=http.newCall(post).execute()) {
            String body=response.body()==null?"":response.body().string();
            if(!response.isSuccessful()){var failed=new Job(initial.id(),null,request,response.code()>=500?"submission_unknown":"failed","音乐提交返回 HTTP "+response.code()+"，请检查模型权限和账户余额",List.of(),initial.createdAt());save(project,failed);return failed;}
            var data=AtlasMediaSupport.OBJECT_MAPPER.readTree(body).path("data");String prediction=data.path("id").asText("");
            if(prediction.isBlank())throw new IllegalStateException("服务未返回任务ID");
            var submitted=new Job(initial.id(),prediction,request,"processing","",List.of(),initial.createdAt());save(project,submitted);return submitted;
        }catch(Exception e){var unknown=new Job(initial.id(),null,request,"submission_unknown","提交结果未知；未自动重试，请先核对 Atlas 任务记录",List.of(),initial.createdAt());save(project,unknown);return unknown;}
    }
    static List<String> outputs(JsonNode data) {
        List<String> urls=new ArrayList<>();for(JsonNode item:data.path("outputs")){String url=item.isTextual()?item.asText():item.path("audio_url").asText(item.path("url").asText(""));if(url.startsWith("https://") && !urls.contains(url))urls.add(url);}return urls;
    }
    public synchronized Job poll(Long project,Long user,String id) throws Exception {
        sounds.owner(project,user);var job=read(project,id);if(!Set.of("processing","saving").contains(job.status()))return job;
        var result=predictions.retrieve(musicModel(job.request().model()),job.predictionId());var data=AtlasMediaSupport.OBJECT_MAPPER.readTree(result.getRawResponse()).path("data");String status=Objects.toString(result.getStatus(),"");
        if(Set.of("failed","canceled","cancelled").contains(status)){String message=data.path("error").asText("音乐生成失败，请查看 Atlas 任务记录");if(message.contains("tags") && message.contains("1000"))message="音乐风格超过上游1000字符限制，请缩短后创建新任务";else message=message.substring(0,Math.min(500,message.length()));var failed=new Job(id,job.predictionId(),job.request(),"failed",message,job.variants(),job.createdAt());save(project,failed);return failed;}
        if(!Set.of("completed","succeeded").contains(status))return job;
        var urls=outputs(data);if(urls.isEmpty()){var failed=new Job(id,job.predictionId(),job.request(),"failed","服务已结束但未返回音轨",List.of(),job.createdAt());save(project,failed);return failed;}
        List<Variant> variants=new ArrayList<>();for(int i=0;i<urls.size();i++) {String soundId=UUID.nameUUIDFromBytes((id+":"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();variants.add(new Variant(i,urls.get(i),soundId,0));}
        save(project,new Job(id,job.predictionId(),job.request(),"saving","",variants,job.createdAt()));
        try {for(int i=0;i<variants.size();i++){var v=variants.get(i);var sound=sounds.importMusic(project,user,v.soundId(),job.request().title()+" · 版本"+(i+1),v.url());variants.set(i,new Variant(i,v.url(),v.soundId(),sound.duration()));}
            var complete=new Job(id,job.predictionId(),job.request(),"completed","",variants,job.createdAt());save(project,complete);return complete;
        }catch(Exception e){var saving=new Job(id,job.predictionId(),job.request(),"saving","音轨已生成，保存暂未完成；刷新任务可重试保存",variants,job.createdAt());save(project,saving);return saving;}
    }
    public ShortDramaSoundService.Sound use(Long project,Long user,String id,Usage usage) throws Exception {
        sounds.owner(project,user);var job=read(project,id);if(!"completed".equals(job.status()) || usage.variant()<0 || usage.variant()>=job.variants().size())throw new IllegalArgumentException("请选择已完成的音乐版本");
        var numbers=storyboards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,project)).stream().map(ShortDramaStoryboard::getSceneNo).toList();
        if(usage.enabled() && (usage.shots()==null || usage.shots().isEmpty() || !numbers.containsAll(usage.shots())))throw new IllegalArgumentException("请选择本项目中实际存在的镜号");
        // Applying one variant removes the other variants of this job from the mix.
        var selected=sounds.musicUsage(project,user,job.variants().get(usage.variant()).soundId(),usage.shots(),usage.volume(),usage.offset(),usage.enabled());
        if(usage.enabled())for(var v:job.variants())if(v.index()!=usage.variant())sounds.musicUsage(project,user,v.soundId(),List.of(),0,0,false);
        return selected;
    }
    public Brief write(Long project,Long user,BriefRequest request) {
        sounds.owner(project,user);var model=models.selectModelByName(request.model());if(model==null || !"chat".equals(model.getCategory()))throw new IllegalArgumentException("请选择写作模型");
        if(!Set.of("bgm","theme","ending").contains(Objects.toString(request.purpose(),"")) || request.direction()!=null && request.direction().length()>2000)throw new IllegalArgumentException("音乐用途或创作要求无效");
        var script=scripts.selectOne(new LambdaQueryWrapper<ShortDramaScript>().eq(ShortDramaScript::getProjectId,project).orderByDesc(ShortDramaScript::getId).last("limit 1"));if(script==null)throw new IllegalArgumentException("请先保存剧本");
        String context=Objects.toString(script.getScriptText(),"");if(context.length()>16000)context=context.substring(0,16000);
        String prompt="你是短剧音乐创作。只输出JSON对象，字段title中文曲名、style英文编曲与演唱风格、prompt。用途="+request.purpose()+"。BGM必须纯器乐，prompt写音乐发展不写歌词；歌曲prompt只能是原创中文歌词和[Verse]、[Chorus]等段落标签，不得插入音乐发展说明或制作解说，最多3000字符。编曲与演唱要求全部写style，最多800字符。遵守当前剧本事实和情感弧线，不虚构已取得的胜利，不照抄小说或现有歌曲，不使用人名口号和产品广告。采用克制具体的意象，适合片尾回味。标题最多50字符。要求："+Objects.toString(request.direction(),"")+"\n剧本：\n"+context;
        try {var writer="atlas".equalsIgnoreCase(model.getProviderCode()) ? dev.langchain4j.model.openai.OpenAiChatModel.builder().baseUrl(model.getApiHost()).apiKey(model.getApiKey()).modelName(model.getModelName()).timeout(java.time.Duration.ofMinutes(6)).maxRetries(0).maxTokens(8192).build() : chats.getOriginalService(ShortDramaServiceImpl.shortDramaProviderCode(model.getProviderCode(),model.getModelName())).buildChatModel(model);String raw=writer.chat(prompt);int start=raw.indexOf('{'),end=raw.lastIndexOf('}');if(start<0 || end<=start)throw new IllegalArgumentException("格式无效");return normalizeBrief(AtlasMediaSupport.OBJECT_MAPPER.readValue(raw.substring(start,end+1),Brief.class),request.purpose());
        }catch(Exception e){throw new IllegalStateException("音乐文案生成失败，请检查写作模型或手动填写",e);}
    }
    static Brief normalizeBrief(Brief brief,String purpose) {
        if(brief.title()==null || brief.title().isBlank() || brief.style()==null || brief.style().isBlank() || brief.prompt()==null)throw new IllegalArgumentException("音乐文案不完整");
        String lyrics=brief.prompt().trim();if(!"bgm".equals(purpose)){var tags=java.util.regex.Pattern.compile("(?i)\\[(?:intro|verse|chorus|bridge|outro|hook|pre-chorus)\\b[^\\]]*\\]").matcher(lyrics);if(tags.find())lyrics=lyrics.substring(tags.start());if(lyrics.isBlank())throw new IllegalArgumentException("歌词为空");}
        if(lyrics.length()>3000)throw new IllegalArgumentException("歌词或描述超过3000字符，请缩短文案");
        String style=brief.style().trim();if(style.length()>1000){style=style.substring(0,1000);int end=Math.max(style.lastIndexOf('\n'),style.lastIndexOf(','));if(end>700)style=style.substring(0,end);}
        return new Brief(brief.title().substring(0,Math.min(50,brief.title().length())).trim(),style,lyrics);
    }
}
