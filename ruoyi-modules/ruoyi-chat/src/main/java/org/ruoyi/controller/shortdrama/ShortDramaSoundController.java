package org.ruoyi.controller.shortdrama;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaSoundService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/short-drama/{projectId}/sounds")
public class ShortDramaSoundController {
    private final ShortDramaSoundService service;
    @GetMapping public R<?> list(@PathVariable Long projectId) throws Exception {service.owner(projectId,LoginHelper.getUserId());return R.ok(service.list(projectId));}
    @PostMapping public R<?> upload(@PathVariable Long projectId,@RequestPart MultipartFile file,@RequestParam String purpose,@RequestParam List<Integer> shots,@RequestParam(defaultValue="0.12") double volume,@RequestParam(defaultValue="0") double offset) throws Exception {
        return R.ok(service.upload(projectId,LoginHelper.getUserId(),file,purpose,shots,volume,offset));
    }
    @GetMapping("/{id}") public ResponseEntity<byte[]> audio(@PathVariable Long projectId,@PathVariable String id) throws Exception {
        service.owner(projectId,LoginHelper.getUserId());return ResponseEntity.ok().contentType(MediaType.valueOf("audio/mpeg")).body(Files.readAllBytes(service.file(projectId,id)));
    }
}
