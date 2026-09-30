package org.ruoyi.controller.shortdrama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.*;

/** Original screen captures are stored locally and never sent through an image model. */
@RestController @RequiredArgsConstructor
@RequestMapping("/short-drama/{projectId}/materials")
public class ShortDramaMaterialController {
    private final ShortDramaProjectMapper projects;
    private final ShortDramaStoryboardMapper boards;
    private static final ObjectMapper JSON = new ObjectMapper();
    private void owner(Long id) {
        var p=projects.selectById(id);
        if(p==null || !Objects.equals(p.getUserId(),LoginHelper.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
    }
    private Path directory(Long project) { return Path.of("data","short-drama-materials",project.toString()).toAbsolutePath().normalize(); }
    private ShortDramaStoryboard editableShot(Long projectId, Long shotId) {
        owner(projectId);
        var shot = boards.selectById(shotId);
        if (shot == null || !projectId.equals(shot.getProjectId())) throw new IllegalArgumentException("镜头不属于当前项目");
        if (!"pending".equals(shot.getVideoStatus())) throw new IllegalStateException("请先停止该镜头的视频任务");
        return shot;
    }
    static com.fasterxml.jackson.databind.node.ArrayNode items(ObjectNode continuity) {
        var media = continuity.path("source_media");
        if (media.path("items").isArray()) return ((com.fasterxml.jackson.databind.node.ArrayNode) media.path("items")).deepCopy();
        var result = JSON.createArrayNode();
        if (media.hasNonNull("url")) result.add(media.deepCopy());
        return result;
    }
    static void setItems(ObjectNode continuity, com.fasterxml.jackson.databind.node.ArrayNode items) {
        if (items.isEmpty()) { continuity.remove("source_media"); return; }
        ObjectNode media = ((ObjectNode) items.get(0)).deepCopy();
        media.remove("items");
        media.put("mode", "direct_insert");
        media.set("items", items);
        continuity.set("source_media", media);
    }
    private void save(ShortDramaStoryboard shot, ObjectNode continuity) {
        // Update only material metadata; do not overwrite unrelated shot fields.
        boards.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, shot.getId()).set(ShortDramaStoryboard::getContinuityJson, continuity.toString()));
    }
    @PostMapping(value="/{shotId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public synchronized R<Object> upload(@PathVariable Long projectId, @PathVariable Long shotId,
            @RequestPart("file") MultipartFile[] files, @RequestParam(required=false) String replaceUrl) throws Exception {
        var shot = editableShot(projectId, shotId);
        ObjectNode continuity = (ObjectNode) JSON.readTree(Objects.toString(shot.getContinuityJson(), "{}"));
        var list = items(continuity);
        int replace = -1;
        if (replaceUrl != null) {
            for (int i=0; i<list.size(); i++) if (replaceUrl.equals(list.get(i).path("url").asText())) replace=i;
            if (replace < 0) throw new IllegalArgumentException("素材已变化，请刷新后重试");
            if (files.length != 1) throw new IllegalArgumentException("更换时请选择一张图片");
        }
        if (files.length == 0 || files.length > 20 || (replace < 0 && list.size()+files.length > 20))
            throw new IllegalArgumentException("每个镜头最多20张素材");
        // Validate the whole selection before storing or changing any bindings.
        List<byte[]> contents = new ArrayList<>();
        for (var file : files) {
            if (file.isEmpty() || file.getSize()>10*1024*1024) throw new IllegalArgumentException("请选择10MB以内的PNG或JPEG图片");
            byte[] bytes = file.getBytes();
            if (!Set.of("image/png", "image/jpeg").contains(Objects.toString(file.getContentType(), "")) || ImageIO.read(new ByteArrayInputStream(bytes))==null)
                throw new IllegalArgumentException("图片格式无效："+file.getOriginalFilename());
            contents.add(bytes);
        }
        Path dir=directory(projectId); Files.createDirectories(dir);
        for (int i=0; i<files.length; i++) {
            var file=files[i]; String type=file.getContentType();
            String name=UUID.randomUUID()+("image/png".equals(type)?".png":".jpg");
            Files.write(dir.resolve(name),contents.get(i),StandardOpenOption.CREATE_NEW);
            ObjectNode image = replace >= 0 ? ((ObjectNode)list.get(replace)).deepCopy() : JSON.createObjectNode();
            image.remove("items");
            image.put("url", "/short-drama/"+projectId+"/materials/file/"+name);
            image.put("filename", Objects.toString(file.getOriginalFilename(), "真实素材"));
            image.put("type", type);
            if (replace >= 0) list.set(replace, image); else list.add(image);
        }
        setItems(continuity, list); save(shot, continuity);
        return R.ok(continuity.get("source_media"));
    }
    @DeleteMapping("/{shotId}")
    public synchronized R<Object> remove(@PathVariable Long projectId, @PathVariable Long shotId, @RequestParam String url) throws Exception {
        var shot=editableShot(projectId, shotId);
        ObjectNode continuity=(ObjectNode)JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}"));
        var list=items(continuity);
        for (int i=list.size()-1; i>=0; i--) if (url.equals(list.get(i).path("url").asText())) list.remove(i);
        setItems(continuity, list); save(shot, continuity);
        // Unbind only. Original files remain available for recovery.
        return R.ok(continuity.get("source_media"));
    }
    @GetMapping("/file/{name}")
    public ResponseEntity<byte[]> content(@PathVariable Long projectId,@PathVariable String name) throws Exception {
        owner(projectId);
        if(!name.matches("[a-f0-9-]{36}\\.(png|jpg)")) return ResponseEntity.badRequest().build();
        Path file=directory(projectId).resolve(name).normalize();
        if(!file.startsWith(directory(projectId)) || !Files.isRegularFile(file)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(name.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).header("Cache-Control","private, max-age=3600").body(Files.readAllBytes(file));
    }

    @GetMapping("/video/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> video(@PathVariable Long projectId,@PathVariable String id) {
        owner(projectId);if(!id.matches("[a-f0-9-]{36}"))return ResponseEntity.badRequest().build();
        var file=directory(projectId).resolve(id+".mp4");if(!Files.isRegularFile(file))return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).body(new org.springframework.core.io.FileSystemResource(file));
    }
    @PostMapping(value="/{shotId}/video", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public synchronized R<?> importVideo(@PathVariable Long projectId, @PathVariable Long shotId,
            @RequestPart("file") MultipartFile upload, @RequestParam String expectedVideoId) throws Exception {
        owner(projectId);
        var shot=boards.selectById(shotId);
        if(shot==null || !projectId.equals(shot.getProjectId()))throw new IllegalArgumentException("镜头不属于当前项目");
        if("generating".equals(shot.getVideoStatus()) || !Objects.equals(expectedVideoId,Objects.toString(shot.getVideoId(),"")))
            throw new IllegalArgumentException("视频状态已变化，请刷新后重试");
        var project=projects.selectById(projectId);
        if(Set.of("pending","processing").contains(Objects.toString(project.getComposeStatus(),"")))throw new IllegalArgumentException("请等待成片合成结束");
        if(upload.isEmpty() || upload.getSize()>19*1024*1024)throw new IllegalArgumentException("请选择19MB以内的视频");
        Path dir=directory(projectId);Files.createDirectories(dir);
        String id=UUID.randomUUID().toString();Path input=dir.resolve(id+".upload"),output=dir.resolve(id+".mp4"),probe=dir.resolve(id+".probe");
        try {
            upload.transferTo(input);
            org.ruoyi.service.shortdrama.impl.ShortDramaSoundService.run(List.of("ffprobe","-v","error","-show_entries","format=duration:stream=codec_type,width,height","-of","json",input.toString()),probe);
            var info=JSON.readTree(Files.readString(probe));double duration=info.path("format").path("duration").asDouble(-1);
            boolean video=false,audio=false;
            for(var stream:info.path("streams")) {if("video".equals(stream.path("codec_type").asText())){video=true;if(stream.path("width").asInt()>3840 || stream.path("height").asInt()>3840)throw new IllegalArgumentException("视频尺寸超过4K");}if("audio".equals(stream.path("codec_type").asText()))audio=true;}
            if(!video || !audio || !Double.isFinite(duration) || Math.abs(duration-shot.getDurationSeconds())>0.25)throw new IllegalArgumentException("视频须包含音轨，且时长与本镜预算一致（允许0.25秒误差）");
            org.ruoyi.service.shortdrama.impl.ShortDramaSoundService.run(List.of("ffmpeg","-v","error","-y","-i",input.toString(),"-map","0:v:0","-map","0:a:0","-t",shot.getDurationSeconds().toString(),"-c:v","libx264","-preset","fast","-crf","18","-pix_fmt","yuv420p","-c:a","aac","-movflags","+faststart",output.toString()),dir.resolve(id+".log"));
            String url="/short-drama/"+projectId+"/materials/video/"+id;
            var update=new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getId,shotId)
                .eq(ShortDramaStoryboard::getVideoStatus,shot.getVideoStatus());
            if(shot.getVideoId()==null)update.isNull(ShortDramaStoryboard::getVideoId);else update.eq(ShortDramaStoryboard::getVideoId,shot.getVideoId());
            int changed=boards.update(null,update.set(ShortDramaStoryboard::getVideoUrl,url).set(ShortDramaStoryboard::getVideoId,"material:"+id)
                .set(ShortDramaStoryboard::getVideoStatus,"done").set(ShortDramaStoryboard::getLastFrameUrl,null).set(ShortDramaStoryboard::getUpdateTime,new Date()));
            if(changed!=1)throw new IllegalStateException("镜头已更新，修订片段未覆盖当前版本");
            return R.ok(Map.of("videoUrl",url));
        } finally {Files.deleteIfExists(input);}
    }
    @PostMapping("/{shotId}/render")
    public synchronized R<?> render(@PathVariable Long projectId,@PathVariable Long shotId) throws Exception {
        owner(projectId);var shot=boards.selectById(shotId);
        if(shot==null || !projectId.equals(shot.getProjectId()) || "generating".equals(shot.getVideoStatus()))throw new IllegalArgumentException("镜头不存在或正在生成");
        var continuity=(ObjectNode)JSON.readTree(Objects.toString(shot.getContinuityJson(),"{}"));
        if(!"direct_insert".equals(continuity.path("source_media").path("mode").asText()))throw new IllegalArgumentException("仅支持真实素材镜头");
        var images=items(continuity);if(images.isEmpty())throw new IllegalArgumentException("请先上传真实素材");
        Path dir=directory(projectId);String job=UUID.randomUUID().toString();
        List<String> command=new ArrayList<>(List.of("ffmpeg","-v","error","-y"));List<String> filters=new ArrayList<>();StringBuilder concat=new StringBuilder();
        double duration=shot.getDurationSeconds(), part=duration/images.size();
        for(int i=0;i<images.size();i++) {
            String prefix="/short-drama/"+projectId+"/materials/file/",url=images.get(i).path("url").asText();
            if(!url.startsWith(prefix))throw new IllegalArgumentException("只允许当前项目已上传的原图");
            String name=url.substring(prefix.length());if(!name.matches("[a-f0-9-]{36}\\.(png|jpg)"))throw new IllegalArgumentException("素材路径无效");
            command.addAll(List.of("-loop","1","-t",Double.toString(part),"-i",dir.resolve(name).toString()));
            filters.add("["+i+":v]"+cropFilter(images.get(i).path("crop"))+"scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2:color=white,setsar=1,fps=24,trim=duration="+part+",setpts=PTS-STARTPTS[v"+i+"]");concat.append("[v").append(i).append("]");
        }
        command.addAll(List.of("-f","lavfi","-t",Double.toString(duration),"-i","anullsrc=r=48000:cl=stereo"));
        filters.add(concat+"concat=n="+images.size()+":v=1:a=0[out]");Path output=dir.resolve(job+".mp4");
        command.addAll(List.of("-filter_complex",String.join(";",filters),"-map","[out]","-map",images.size()+":a","-t",Double.toString(duration),"-c:v","libx264","-preset","fast","-crf","18","-pix_fmt","yuv420p","-c:a","aac","-movflags","+faststart",output.toString()));
        org.ruoyi.service.shortdrama.impl.ShortDramaSoundService.run(command,dir.resolve(job+".log"));
        String videoUrl="/short-drama/"+projectId+"/materials/video/"+job;
        boards.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getId,shotId)
            .set(ShortDramaStoryboard::getVideoUrl,videoUrl).set(ShortDramaStoryboard::getVideoStatus,"done").set(ShortDramaStoryboard::getVideoId,"material:"+job));
        return R.ok(Map.of("videoUrl",videoUrl,"duration",duration));
    }
    static String cropFilter(com.fasterxml.jackson.databind.JsonNode crop) {
        if(crop.isMissingNode() || crop.isNull())return "";
        if(!crop.isObject())throw new IllegalArgumentException("素材裁切区域格式无效");
        double x=crop.path("x").asDouble(-1),y=crop.path("y").asDouble(-1),w=crop.path("width").asDouble(-1),h=crop.path("height").asDouble(-1);
        if(!Double.isFinite(x+y+w+h) || x<0 || y<0 || w<=0 || h<=0 || x+w>1.00001 || y+h>1.00001)throw new IllegalArgumentException("素材裁切区域须在原图内");
        return "crop=iw*"+w+":ih*"+h+":iw*"+x+":ih*"+y+",";
    }
}
