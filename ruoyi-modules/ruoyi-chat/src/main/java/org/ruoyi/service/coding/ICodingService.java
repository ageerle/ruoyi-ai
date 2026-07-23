package org.ruoyi.service.coding;

import org.ruoyi.domain.bo.coding.CodingRequestBo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 编程能力 Service
 *
 * @author ageerle
 */
public interface ICodingService {

    /**
     * 编程对话（SSE 流式）
     *
     * @param bo     请求参数
     * @param userId 用户 ID（可为 null，第一阶段免鉴权）
     * @return SseEmitter
     */
    SseEmitter chat(CodingRequestBo bo, Long userId);
}
