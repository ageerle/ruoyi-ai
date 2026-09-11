package org.ruoyi.controller.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.service.media.AtlasMediaContentService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaContentController {
    private final IChatModelService models;
    private final AtlasMediaContentService content;

    /** Uses the normal login interceptor; browser clients fetch with their Authorization header. */
    @GetMapping("/content")
    public ResponseEntity<byte[]> content(@RequestParam @NotBlank String model,
                                          @RequestParam @NotBlank String predictionId) throws IOException {
        var configuredModel = models.selectModelByName(model);
        if (configuredModel == null) {
            throw new IllegalArgumentException("未找到模型配置");
        }
        var result = content.retrieve(configuredModel, predictionId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.mimeType()))
            .contentLength(result.bytes().length)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .header("X-Content-Type-Options", "nosniff")
            .body(result.bytes());
    }
}
