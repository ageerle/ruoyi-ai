package org.ruoyi.controller.shortdrama;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.impl.ShortDramaVideoReferenceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/short-drama/{projectId}/video-references")
public class ShortDramaVideoReferenceController {
    private final ShortDramaVideoReferenceService references;
    @GetMapping public R<List<ShortDramaVideoReferenceService.Clip>> list(@PathVariable Long projectId) throws Exception {
        return R.ok(references.list(projectId, LoginHelper.getUserId()));
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ShortDramaVideoReferenceService.Clip> upload(@PathVariable Long projectId, @RequestPart("file") MultipartFile file) throws Exception {
        return R.ok(references.upload(projectId, LoginHelper.getUserId(), file));
    }
    @GetMapping("/{id}/content") public ResponseEntity<Resource> content(@PathVariable Long projectId, @PathVariable String id) {
        references.owner(projectId, LoginHelper.getUserId());
        var resource = new FileSystemResource(references.file(projectId, id));
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).body(resource);
    }
}
