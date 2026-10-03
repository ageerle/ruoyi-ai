package org.ruoyi.service.shortdrama.impl;

import java.util.Objects;
import java.util.regex.Pattern;

/** Narrow, owner-checked import feedback; never relax the global exception disclosure policy. */
public final class ShortDramaReviewedImportMessages {
    private ShortDramaReviewedImportMessages() { }
    private static final Pattern DOMAIN = Pattern.compile("^(?:项目不存在或无权限|剧本|规划|审阅|导入|缺少第|请等待当前分镜|已有分镜|当前资产|每镜|本场|镜头|角色|场景|background_extras|匿名群演|遗漏或乱序原文对白|对白语速|时间预算|minimumShotSeconds|未找到模型配置|无可用聊天模型).*");

    /** null means an unexpected/infrastructure error, which must retain the global safe response. */
    public static String failureMessage(RuntimeException failure) {
        if (!(failure instanceof IllegalArgumentException) && !(failure instanceof IllegalStateException)) return null;
        String message = Objects.toString(failure.getMessage(), "").replaceAll("[\\p{Cntrl}]+", " ").trim();
        if (!(failure instanceof ShortDramaSceneCheckpoint.BindingMismatch) && !DOMAIN.matcher(message).matches()) return null;
        return ShortDramaSceneCandidateDiagnostics.limit("审阅规划导入未通过："
            + ShortDramaSceneCandidateDiagnostics.redact(message), 2048);
    }
}
