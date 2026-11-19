package org.ruoyi.chat.controller.shortdrama;

import org.ruoyi.common.core.domain.R;
import org.ruoyi.chat.service.shortdrama.impl.ShortDramaVoiceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes={ShortDramaCharacterVoiceController.class,ShortDramaController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ShortDramaVoiceErrors {
    @ExceptionHandler(ShortDramaVoiceException.class)
    public R<Void> validation(ShortDramaVoiceException error) {return R.fail(error.getMessage());}
}
