package org.ruoyi.controller.shortdrama;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaCharacterVoiceService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.util.Map;

@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}")
public class ShortDramaCharacterVoiceController {
    private final ShortDramaCharacterVoiceService service;
    @GetMapping("/characters/{characterId}/voice")
    public R<?> get(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId) throws Exception {return R.ok(service.get(projectId,characterId,LoginHelper.getUserId()));}
    @PutMapping("/characters/{characterId}/voice")
    public R<?> settings(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@RequestBody ShortDramaCharacterVoiceService.Settings request) throws Exception {return R.ok(service.settings(projectId,characterId,LoginHelper.getUserId(),request));}
    @PostMapping("/characters/{characterId}/voice/generate")
    public R<?> generate(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@RequestBody ShortDramaCharacterVoiceService.Generate request) throws Exception {return R.ok(service.generate(projectId,characterId,LoginHelper.getUserId(),request));}
    @GetMapping("/characters/{characterId}/voice/jobs/{id}")
    public R<?> poll(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@PathVariable("id") String id) throws Exception {return R.ok(service.poll(projectId,characterId,LoginHelper.getUserId(),id));}
    @PostMapping("/characters/{characterId}/voice/upload")
    public R<?> upload(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@RequestPart("file") MultipartFile file) throws Exception {return R.ok(service.upload(projectId,characterId,LoginHelper.getUserId(),file));}
    @PostMapping("/characters/{characterId}/voice/select")
    public R<?> select(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@RequestBody ShortDramaCharacterVoiceService.Selection request) throws Exception {return R.ok(service.select(projectId,characterId,LoginHelper.getUserId(),request));}
    @GetMapping("/characters/{characterId}/voice/samples/{id}")
    public ResponseEntity<byte[]> audio(@PathVariable("projectId") Long projectId,@PathVariable("characterId") Long characterId,@PathVariable("id") String id) throws Exception {
        return ResponseEntity.ok().contentType(MediaType.valueOf("audio/mpeg")).cacheControl(CacheControl.noStore()).body(Files.readAllBytes(service.audio(projectId,characterId,LoginHelper.getUserId(),id)));
    }
    @GetMapping("/voices/storyboards/{storyboardId}")
    public R<?> preview(@PathVariable("projectId") Long projectId,@PathVariable("storyboardId") Long storyboardId,@RequestParam(value="model",required=false) String model) throws Exception {
        var plan=service.preview(projectId,storyboardId,LoginHelper.getUserId(),model);
        return R.ok(Map.of("bindings",plan.bindings(),"direction",plan.direction(),"issues",plan.issues(),"explicit",plan.explicit(),"references",plan.fingerprints(),"speakerIds",plan.speakerIds()));
    }
    @PutMapping("/voices/storyboards/{storyboardId}")
    public R<?> speakers(@PathVariable("projectId") Long projectId,@PathVariable("storyboardId") Long storyboardId,@RequestBody ShortDramaCharacterVoiceService.SpeakersRequest request) throws Exception {
        return R.ok(Map.of("continuityJson",service.speakers(projectId,storyboardId,LoginHelper.getUserId(),request).getContinuityJson()));
    }
}
