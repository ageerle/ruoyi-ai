package org.ruoyi.service.video;

import org.ruoyi.service.media.AtlasMediaSupport;

/** A provider response that explicitly confirms no video task was accepted. */
public final class VideoSubmissionRejectedException extends IllegalArgumentException {
    private final String responseBody;

    private VideoSubmissionRejectedException(String responseBody) {
        super("Atlas Cloud拒绝视频提交: HTTP 402 - " + responseBody);
        this.responseBody = responseBody;
    }

    public String providerCode() { return "atlas"; }
    public String responseBody() { return responseBody; }

    /** Be conservative: timeouts, 5xx and bodies with any task data remain unknown. */
    public static VideoSubmissionRejectedException atlasResponse(int httpStatus, String body) {
        if (httpStatus != 402 || body == null || body.length() > 2000) return null;
        try {
            var json = AtlasMediaSupport.OBJECT_MAPPER.readTree(body);
            if (json != null && json.isObject() && json.size() == 2
                && json.path("code").isIntegralNumber() && json.path("code").asInt() == 402
                && "insufficient balance".equalsIgnoreCase(json.path("msg").asText().trim()))
                return new VideoSubmissionRejectedException(json.toString());
        } catch (java.io.IOException ignored) { /* An unrecognized response cannot release a paid claim. */ }
        return null;
    }
}
