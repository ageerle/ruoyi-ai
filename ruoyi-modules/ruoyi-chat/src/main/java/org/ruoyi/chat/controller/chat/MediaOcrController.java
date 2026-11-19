package org.ruoyi.chat.controller.chat;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.service.media.MediaOcrService;
import org.ruoyi.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaOcrController {

    private final MediaOcrService ocrService;

    @SaCheckLogin
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<OcrResult> recognize(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "model", defaultValue = MediaOcrService.MODEL) String model
    ) {
        return R.ok(new OcrResult(ocrService.recognize(file, model)));
    }

    @ExceptionHandler(MediaOcrService.OcrException.class)
    public R<OcrResult> handleFailure(MediaOcrService.OcrException error) {
        return R.fail(error.getMessage());
    }

    public record OcrResult(String text) {
    }
}
