package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.common.core.service.OssService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor
public class ShortDramaSoundService {
    private final ShortDramaProjectMapper projects;
    private final org.ruoyi.common.chat.factory.ImageServiceFactory images;
    private static final ObjectMapper JSON=new ObjectMapper();
    public record Sound(String id,String name,String purpose,List<Integer> shots,double volume,double offset,double duration,String providerUrl) {}
    public record Reference(String id,String name,Path file,double duration,String hash) {
        static Reference of(String id,String name,Path file,double duration) throws Exception {
            String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
            return new Reference(id,name,file,duration,hash);
        }
        public Map<String,Object> fingerprint(){return Map.of("id",id,"name",name,"duration",duration,"sha256",hash);}
    }
    private Path directory(Long project) {return Path.of("data","short-drama-sounds",project.toString()).toAbsolutePath();}
    public void owner(Long id,Long user) {var p=projects.selectById(id);if(p==null || !Objects.equals(p.getUserId(),user))throw new IllegalArgumentException("项目不存在或无权限");}
    public List<Sound> list(Long id) throws Exception {
        Path dir=directory(id);if(!Files.isDirectory(dir))return List.of();
        try(var files=Files.list(dir)){List<Sound> all=new ArrayList<>();for(Path f:files.filter(p->p.toString().endsWith(".json")).sorted().toList())all.add(JSON.readValue(f.toFile(),Sound.class));return all;}
    }
    public Path file(Long project,String id) {if(!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("音频ID无效");return directory(project).resolve(id+".mp3");}
    /** Import only URLs obtained by the server from a music prediction; never accepts a user URL. */
    public synchronized Sound importMusic(Long project,Long user,String id,String name,String url) throws Exception {
        owner(project,user);Path metadata=directory(project).resolve(UUID.fromString(id)+".json");
        if(Files.exists(metadata))return JSON.readValue(metadata.toFile(),Sound.class);
        var uri=java.net.URI.create(url);if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)throw new IllegalArgumentException("音乐结果地址无效");
        Files.createDirectories(directory(project));Path input=Files.createTempFile(directory(project),"music-",".bin");Path target=file(project,id);
        var client=new okhttp3.OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).build();
        try(var response=client.newCall(new okhttp3.Request.Builder().url(url).build()).execute()) {
            if(!response.isSuccessful() || response.body()==null)throw new IllegalStateException("音乐素材暂时无法下载");
            try(var stream=response.body().byteStream();var output=Files.newOutputStream(input)){byte[] buffer=new byte[8192];int count;long total=0;while((count=stream.read(buffer))!=-1){total+=count;if(total>19*1024*1024)throw new IllegalArgumentException("音轨超过19MB");output.write(buffer,0,count);}}
            run(List.of("ffmpeg","-v","error","-y","-i",input.toString(),"-vn","-t","600","-ar","48000","-ac","2","-c:a","libmp3lame",target.toString()),directory(project).resolve(id+".log"));
            Path probe=directory(project).resolve(id+".probe");run(List.of("ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1:nk=1",target.toString()),probe);
            var sound=new Sound(id,name,"music_candidate",List.of(),0.10,0,Double.parseDouble(Files.readString(probe).trim()),url);
            JSON.writeValue(metadata.toFile(),sound);return sound;
        }finally{Files.deleteIfExists(input);}
    }
    public synchronized Sound musicUsage(Long project,Long user,String id,List<Integer> shots,double volume,double offset,boolean enabled) throws Exception {
        owner(project,user);Path metadata=directory(project).resolve(UUID.fromString(id)+".json");var old=JSON.readValue(metadata.toFile(),Sound.class);
        if(old.providerUrl()==null || old.providerUrl().isBlank())throw new IllegalArgumentException("不是生成音乐素材");
        if(!Double.isFinite(volume) || volume<0 || volume>1 || !Double.isFinite(offset) || offset<0 || offset>=old.duration() || enabled && (shots==null || shots.isEmpty() || shots.stream().anyMatch(n->n==null || n<1)))throw new IllegalArgumentException("镜号、音量或素材起点无效");
        var sound=new Sound(id,old.name(),enabled?"bgm":"music_candidate",enabled?shots.stream().distinct().sorted().toList():List.of(),volume,offset,old.duration(),old.providerUrl());JSON.writeValue(metadata.toFile(),sound);return sound;
    }
    public synchronized Sound upload(Long project,Long user,MultipartFile upload,String purpose,List<Integer> shots,double volume,double offset) throws Exception {
        owner(project,user);
        if(!Set.of("bgm","reference").contains(purpose) || shots==null || shots.isEmpty() || shots.stream().anyMatch(n->n<1) || !Double.isFinite(volume) || volume<0 || volume>1 || !Double.isFinite(offset) || offset<0)
            throw new IllegalArgumentException("请设置用途、镜号、音量和有效的起点");
        if(upload.isEmpty() || upload.getSize()>19*1024*1024)throw new IllegalArgumentException("音频必须小于19MB");
        Files.createDirectories(directory(project));String id=UUID.randomUUID().toString();Path target=file(project,id);
        Path input=Files.createTempFile(directory(project),"input-",".bin");
        try {
            upload.transferTo(input);
            run(List.of("ffmpeg","-v","error","-y","-i",input.toString(),"-vn","-t","600","-ar","48000","-ac","2","-c:a","libmp3lame",target.toString()),directory(project).resolve(id+".log"));
            Path probe=directory(project).resolve(id+".probe");
            run(List.of("ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1:nk=1",target.toString()),probe);
            double duration=Double.parseDouble(Files.readString(probe).trim());
            if(offset>=duration)throw new IllegalArgumentException("起点超出音频长度");
            if("reference".equals(purpose) && (duration<2 || duration>30.1))throw new IllegalArgumentException("生成参考音频应为2至30秒（2.0模型提交时限15秒）；长音乐请选择背景音乐用途");
            String url="";

            var sound=new Sound(id,Objects.toString(upload.getOriginalFilename(),"音频"),purpose,shots.stream().distinct().sorted().toList(),volume,offset,duration,url);
            JSON.writeValue(directory(project).resolve(id+".json").toFile(),sound);return sound;
        }finally{Files.deleteIfExists(input);}
    }
    public List<Reference> referenceFiles(Long project,int shot) throws Exception {
        List<Reference> result=new ArrayList<>();
        for(var sound:list(project))if("reference".equals(sound.purpose()) && sound.shots().contains(shot))
            result.add(Reference.of(sound.id(),sound.name(),file(project,sound.id()),sound.duration()));
        return result;
    }
    public List<String> references(Long project,int shot,org.ruoyi.common.chat.domain.vo.chat.ChatModelVo model) {
        try {return uploadReferences(referenceFiles(project,shot),model);}
        catch(Exception e){throw new IllegalStateException("参考音频读取失败："+e.getMessage(),e);}
    }
    public synchronized List<String> uploadReferences(List<Reference> references,org.ruoyi.common.chat.domain.vo.chat.ChatModelVo model) {
        try {
            ShortDramaVoiceContract.validateReferences(model.getModelName(),references.stream().map(Reference::duration).toList());
            List<String> result=new ArrayList<>();
            for(var reference:references) {
                // Immutable file hashes keep retry parameters stable. Provider upload URLs may change.
                result.add(images.getOriginalService(model.getProviderCode()).uploadMedia(model,Files.readAllBytes(reference.file()),reference.name()+".mp3","audio/mpeg"));
            }
            return result;
        }catch(Exception e){throw new IllegalStateException("参考音频上传失败，当前模型须支持音频参考："+e.getMessage(),e);}
    }
    public Path mix(Long project,List<org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard> shots,Path video,Path work,double overlap) throws Exception {
        var music=list(project).stream().filter(a->"bgm".equals(a.purpose())).toList();
        if(music.isEmpty())return video;
        List<String> command=new ArrayList<>(List.of("ffmpeg","-v","error","-y","-i",video.toString()));
        List<String> filters=new ArrayList<>();List<String> tracks=new ArrayList<>();int input=1;
        for(var sound:music) {
            double cursor=0,start=-1,end=0;
            List<double[]> ranges=new ArrayList<>();
            for(var shot:shots) {
                double duration=shot.getDurationSeconds();
                if(sound.shots().contains(shot.getSceneNo())) {if(start<0)start=cursor;end=cursor+duration;}
                else if(start>=0){ranges.add(new double[]{start,end});start=-1;}
                cursor+=Math.max(0,duration-overlap);
            }
            if(start>=0)ranges.add(new double[]{start,end});
            for(var range:ranges) {
                double duration=range[1]-range[0];
                command.addAll(List.of("-stream_loop","-1","-i",file(project,sound.id()).toString()));
                String label="m"+input;
                filters.add("["+input+":a]atrim=start="+sound.offset()+":duration="+duration+",asetpts=PTS-STARTPTS,volume="+sound.volume()+",afade=t=in:d="+Math.min(1,duration/3)+",afade=t=out:st="+Math.max(0,duration-1)+":d="+Math.min(1,duration/3)+",adelay="+Math.round(range[0]*1000)+":all=1["+label+"]");
                tracks.add("["+label+"]");input++;
            }
        }
        if(tracks.isEmpty())return video;
        filters.add(String.join("",tracks)+"amix=inputs="+tracks.size()+":normalize=0[music]");
        filters.add("[0:a]asplit=2[dialogue][side]");
        filters.add("[music][side]sidechaincompress=threshold=0.03:ratio=8:attack=20:release=400[ducked]");
        // Disable limiter make-up gain and leave headroom for AAC intersample peaks.
        filters.add("[dialogue][ducked]amix=inputs=2:duration=first:normalize=0,alimiter=limit=0.7:level=false[out]");
        Path out=work.resolve("with-background-music.mp4");
        command.addAll(List.of("-filter_complex",String.join(";",filters),"-map","0:v:0","-map","[out]","-c:v","copy","-c:a","aac","-b:a","192k","-movflags","+faststart",out.toString()));
        run(command,work.resolve("music-mix.log"));return out;
    }
    public static void run(List<String> command,Path log) throws Exception {
        Process p=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!p.waitFor(120,TimeUnit.SECONDS)){p.destroyForcibly();throw new IllegalStateException("音频处理超时");}
        if(p.exitValue()!=0)throw new IllegalArgumentException("无法解码音频，请使用有效的音频文件");
    }
}
