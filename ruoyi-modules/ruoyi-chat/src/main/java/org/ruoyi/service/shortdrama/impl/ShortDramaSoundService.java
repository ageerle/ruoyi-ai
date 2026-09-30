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
    private Path directory(Long project) {return Path.of("data","short-drama-sounds",project.toString()).toAbsolutePath();}
    public void owner(Long id,Long user) {var p=projects.selectById(id);if(p==null || !Objects.equals(p.getUserId(),user))throw new IllegalArgumentException("项目不存在或无权限");}
    public List<Sound> list(Long id) throws Exception {
        Path dir=directory(id);if(!Files.isDirectory(dir))return List.of();
        try(var files=Files.list(dir)){List<Sound> all=new ArrayList<>();for(Path f:files.filter(p->p.toString().endsWith(".json")).sorted().toList())all.add(JSON.readValue(f.toFile(),Sound.class));return all;}
    }
    public Path file(Long project,String id) {if(!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("音频ID无效");return directory(project).resolve(id+".mp3");}
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
            if("reference".equals(purpose) && (duration<2 || duration>15.1))throw new IllegalArgumentException("生成参考音频应为2至15秒；长音乐请选择背景音乐用途");
            String url="";

            var sound=new Sound(id,Objects.toString(upload.getOriginalFilename(),"音频"),purpose,shots.stream().distinct().sorted().toList(),volume,offset,duration,url);
            JSON.writeValue(directory(project).resolve(id+".json").toFile(),sound);return sound;
        }finally{Files.deleteIfExists(input);}
    }
    public List<String> references(Long project,int shot,org.ruoyi.common.chat.domain.vo.chat.ChatModelVo model) {
        try {
            List<String> result=new ArrayList<>();double total=0;
            for(var s:list(project))if("reference".equals(s.purpose()) && s.shots().contains(shot)) {
                if(result.size()>=3)throw new IllegalArgumentException("每镜最多3条参考音频");
                total+=s.duration();if(total>15.1)throw new IllegalArgumentException("每镜参考音频总长不能超过15秒");
                result.add(images.getOriginalService(model.getProviderCode()).uploadMedia(model,Files.readAllBytes(file(project,s.id())),s.name(),"audio/mpeg"));
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
